package com.clashsoft.p2psync.net;

import com.clashsoft.p2psync.Main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServerThread extends Thread
{
	private int port;
	private final List<Peer> peers = new ArrayList<>();

	private volatile boolean running = true;

	public ServerThread(int port)
	{
		super("Server-" + port);
		this.port = port;
	}

	public void close()
	{
		this.running = false;
		this.stop();
	}

	@Override
	public void run()
	{
		try (ServerSocket server = new ServerSocket(this.port))
		{
			server.setSoTimeout(Constants.TIMEOUT);

			this.port = Main.PORT = server.getLocalPort();

			System.out.println("Server listening on port " + server.getLocalPort());

			while (this.running)
			{
				this.read(server);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private void read(ServerSocket server) throws IOException
	{
		try
		{
			for (Iterator<Peer> iterator = this.peers.iterator(); iterator.hasNext(); )
			{
				// Look for data on the peer input streams

				final Peer peer = iterator.next();
				if (!peer.isConnected())
				{
					// Remove closed sockets
					peer.closeSocket();
					iterator.remove();
					continue;
				}

				peer.handlePacket();
			}

			final Socket connection = server.accept();
			System.out.println("Connected to " + connection);
			this.peers.add(new Peer(connection));
		}
		catch (SocketTimeoutException ignored)
		{
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
