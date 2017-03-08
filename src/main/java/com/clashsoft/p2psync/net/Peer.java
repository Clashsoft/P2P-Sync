package com.clashsoft.p2psync.net;

import com.clashsoft.p2psync.SyncEntry;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class Peer
{
	public final Address address;

	public final Set<SyncEntry> syncEntries = new HashSet<>();

	private Socket socket;

	public Peer(Socket socket) throws IOException
	{
		this.socket = socket;
		this.address = Address.fromSocket(this.socket);
	}

	public Peer(Address address) throws IOException
	{
		this.socket = newSocket(address);
		this.address = Address.fromSocket(this.socket);
	}

	public void addSyncEntry(SyncEntry entry)
	{
		this.syncEntries.add(entry);
	}

	private static Socket newSocket(Address address) throws IOException
	{
		final Socket socket = new Socket(address.hostname, address.port);

		socket.setKeepAlive(true);
		socket.setSoTimeout(Constants.TIMEOUT);
		return socket;
	}

	private Socket getSocket() throws IOException
	{
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
			if (!this.socket.isClosed())
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
		return !this.socket.isClosed() && this.socket.isConnected() && this.socket.isBound();
	}

	public void synchronize()
	{
		try
		{
			this.sendOffer();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void sendOffer() throws IOException
	{
		DataOutputStream dos = new DataOutputStream(this.getSocket().getOutputStream());
		dos.write(Constants.OFFER);

		System.out.println("Sending OFFER...");

		for (SyncEntry entry : this.syncEntries)
		{
			this.writeFileOffer(dos, new File(entry.getLocalFile()), new File(entry.getRemoteFile()));

			// from
			// to
			// from changed
		}

		dos.writeUTF("");
		dos.flush();
	}

	private void writeFileOffer(DataOutputStream dos, File localPath, File remotePath) throws IOException
	{
		if (!localPath.isDirectory())
		{
			dos.writeUTF(localPath.getAbsolutePath());
			dos.writeUTF(remotePath.getAbsolutePath());
			dos.writeLong(localPath.lastModified());
			return;
		}

		//noinspection ConstantConditions
		for (String subFile : localPath.list())
		{
			this.writeFileOffer(dos, new File(localPath, subFile), new File(remotePath, subFile));
		}
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
		DataOutputStream dos = new DataOutputStream(this.getSocket().getOutputStream());
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
		final InputStream inputStream = this.socket.getInputStream();
		if (inputStream.available() >= 0)
		{
			this.handlePacket(new DataInputStream(inputStream));
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
			final String remotePath = input.readUTF();
			if (remotePath.isEmpty())
			{
				return;
			}

			final String localPath = input.readUTF();
			final long remoteTimeStamp = input.readLong();

			System.out.print("Received OFFER to copy from " + remotePath + " to " + localPath + ", ");

			final File localFile = new File(localPath);
			if (remoteTimeStamp > localFile.lastModified())
			{
				System.out.println("accepted");
				this.sendRequest(remotePath, localPath);
			}
			else
			{
				System.out.println("declined (file up-to-date)");
			}
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

		new Thread(() -> this.saveFile(input, localPath, size)).start();

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

			temp.renameTo(new File(localPath));
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}
}
