package com.clashsoft.p2psync.net;

import com.clashsoft.p2psync.Main;
import com.clashsoft.p2psync.SyncEntry;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class Peer
{
	private static final int OFFER_END = 0;
	private static final int OFFER_ENTRY = 1;
	private static final int OFFER_DIRECTORY = 2;
	private static final int OFFER_FILE = 3;

	public final Address address;

	public final Set<SyncEntry> syncEntries = new HashSet<>();

	private Socket socket;
	private final Main main;

	public Peer(Socket socket, Main main) throws IOException
	{
		this.socket = socket;
		this.main = main;
		this.address = Address.fromSocket(this.socket);
	}

	public Peer(Address address, Main main)
	{
		this.main = main;
		this.address = address;
	}

	public void addSyncEntry(SyncEntry entry)
	{
		this.syncEntries.add(entry);
	}

	private static Socket newSocket(Address address) throws IOException
	{
		final Socket socket = new Socket();

		try
		{
			socket.connect(new InetSocketAddress(address.hostname, address.port), Constants.TIMEOUT);
		}
		catch (IOException ex)
		{
			System.err.println("Cannot connect to " + address + ": " + ex.getMessage());
		}

		socket.setKeepAlive(true);
		socket.setSoTimeout(Constants.TIMEOUT);
		return socket;
	}

	private Socket getSocket() throws IOException
	{
		if (this.socket == null)
		{
			return this.socket = newSocket(this.address);
		}
		if (!this.socket.isConnected())
		{
			this.closeSocket();
		}
		if (this.socket.isClosed())
		{
			return this.socket = newSocket(this.address);
		}

		return this.socket;
	}

	public void closeSocket()
	{
		try
		{
			if (this.socket != null && !this.socket.isClosed())
			{
				this.socket.close();
			}
		}
		catch (IOException ignored)
		{
		}
	}

	public boolean isConnected()
	{
		return this.socket != null && !this.socket.isClosed() && this.socket.isConnected() && this.socket.isBound();
	}

	public void sendOffer() throws IOException
	{
		if (this.syncEntries.isEmpty() || this.syncEntries.stream().noneMatch(SyncEntry::isEnabled))
		{
			return;
		}

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(bos);
		dos.write(Constants.OFFER);

		for (SyncEntry entry : this.syncEntries)
		{
			if (!entry.isEnabled())
			{
				continue;
			}

			final String localFile = entry.getLocalFile();
			System.out.println("Sending OFFER to copy from " + localFile);

			dos.write(OFFER_ENTRY);
			dos.writeUTF(localFile);

			this.writeFileOffer(dos, new File(localFile));
		}

		dos.write(OFFER_END);
		dos.flush();

		bos.writeTo(this.getSocket().getOutputStream());
	}

	private void writeFileOffer(DataOutputStream dos, File localPath) throws IOException
	{
		if (!localPath.isDirectory())
		{
			dos.write(OFFER_FILE);
			dos.writeUTF(localPath.getAbsolutePath().replace(File.separatorChar, '/'));
			dos.writeLong(localPath.lastModified());
			return;
		}

		dos.write(OFFER_DIRECTORY);
		dos.writeUTF(localPath.getName());

		//noinspection ConstantConditions
		for (String subFile : localPath.list())
		{
			this.writeFileOffer(dos, new File(localPath, subFile));
		}

		dos.write(OFFER_END);
	}

	private void sendRequest(String localPath, String remotePath) throws IOException
	{
		DataOutputStream dos = new DataOutputStream(this.getSocket().getOutputStream());
		System.out.println("Sending REQUEST to copy from " + localPath + " to " + remotePath);

		dos.write(Constants.REQUEST);
		dos.writeUTF(localPath); // from
		dos.writeUTF(remotePath); // to
		dos.flush();
	}

	private void sendFile(File localPath, String remotePath) throws IOException
	{
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(this.getSocket().getOutputStream()));
		System.out.println("Sending RESPONSE payload from " + localPath + " to " + remotePath);

		dos.write(Constants.RESPONSE);
		dos.writeUTF(remotePath); // to

		final Path path = localPath.toPath();
		dos.writeLong(Files.size(path));

		Files.copy(path, dos);

		dos.flush();
	}

	public void handlePacket() throws IOException
	{
		if (!this.isConnected())
		{
			return;
		}

		final InputStream inputStream = this.socket.getInputStream();
		if (inputStream.available() >= 0)
		{
			this.handlePacket(new DataInputStream(new BufferedInputStream(inputStream)));
		}
	}

	private void handlePacket(DataInputStream input) throws IOException
	{
		try
		{
			switch (input.read())
			{
			case Constants.OFFER:
				this.handleOffer(input);
				return;
			case Constants.REQUEST:
				this.handleRequest(input);
				return;
			case Constants.RESPONSE:
				this.handleResponse(input);
				return;
			default:
			}
		}
		catch (SocketTimeoutException ignored)
		{
		}
	}

	private void handleOffer(DataInputStream input) throws IOException
	{
		while (true)
		{
			switch (input.read())
			{
			case OFFER_END:
				return;
			case OFFER_ENTRY:
				final String remotePath = input.readUTF();
				final SyncEntry currentEntry = this.main.getInboundEntry(this.address, remotePath);
				final String localDir = currentEntry.getLocalFile();

				switch (input.read())
				{
				case OFFER_FILE:
					this.readFileOffer(input, currentEntry, localDir);
					break;
				case OFFER_DIRECTORY:
					input.readUTF(); // the dir name, already the last component of localDir
					//noinspection StatementWithEmptyBody
					while (this.readOffer(input, currentEntry, localDir));
				}
			}
		}
	}

	private boolean readOffer(DataInputStream input, SyncEntry currentEntry, String localDir) throws IOException
	{
		switch (input.read())
		{
		case OFFER_END:
			return false;
		case OFFER_FILE:
		{
			this.readFileOffer(input, currentEntry, localDir);
			return true;
		}
		case OFFER_DIRECTORY:
			final String remoteName = input.readUTF();
			final String newDir = localDir + File.separator + remoteName;

			//noinspection StatementWithEmptyBody
			while (this.readOffer(input, currentEntry, newDir));
		}
		return true;
	}

	private void readFileOffer(DataInputStream input, SyncEntry currentEntry, String localDir) throws IOException
	{
		final String remotePath = input.readUTF();
		final long lastChanged = input.readLong();
		final String remoteName = remotePath.substring(remotePath.lastIndexOf('/') + 1);
		final String localPath = localDir + File.separator + remoteName;

		System.out.print("Received OFFER to copy from " + remotePath + " to " + localPath + ", ");

		if (!currentEntry.isEnabled() || currentEntry.getLocalFile().isEmpty())
		{
			System.out.println("declined (inbound configuration disabled)");
			return;
		}

		if (lastChanged > new File(localPath).lastModified())
		{
			System.out.println("accepted");
			this.sendRequest(remotePath, localPath);
		}
		else
		{
			System.out.println("declined (file up-to-date)");
		}
	}

	private void handleRequest(DataInputStream input) throws IOException
	{
		String localPath = input.readUTF();
		String remotePath = input.readUTF();

		System.out.println("Received REQUEST to copy from " + localPath + " to " + remotePath);

		this.sendFile(new File(localPath), remotePath);
	}

	private void handleResponse(DataInputStream input) throws IOException
	{
		String localPath = input.readUTF();
		long size = input.readLong();

		System.out.println("Received RESPONSE of size " + size + "B, copying to " + localPath + "...");

		this.saveFile(input, localPath, size);

		this.writeEOT();
	}

	private void writeEOT() throws IOException
	{
		final OutputStream outputStream = this.getSocket().getOutputStream();
		outputStream.write(0);
		outputStream.flush();
	}

	private void saveFile(DataInputStream input, String localPath, long size)
	{
		try
		{
			// write the data to a temporary file

			File temp = File.createTempFile("p2p_temp_", null);
			try (FileOutputStream fos = new FileOutputStream(temp))
			{
				byte[] buffer = new byte[4096];
				for (long l = size; l > 0; )
				{
					int read = input.read(buffer, 0, (int) Math.min(4096, l));
					fos.write(buffer, 0, read);
					l -= read;
				}
			}

			// move the temp to the desired location

			final File targetFile = new File(localPath);
			Files.createDirectories(targetFile.getParentFile().toPath());
			temp.renameTo(targetFile);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}
}
