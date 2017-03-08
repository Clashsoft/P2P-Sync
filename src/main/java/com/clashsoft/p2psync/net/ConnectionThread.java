package com.clashsoft.p2psync.net;

import com.clashsoft.p2psync.Main;
import com.clashsoft.p2psync.SyncEntry;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class ConnectionThread extends Thread
{
	private final Main main;

	private volatile boolean running = true;

	public ConnectionThread(int port, Main main)
	{
		super("Connection-" + port);
		this.main = main;
	}

	public void close()
	{
		this.running = false;
	}

	@Override
	public void run()
	{
		while (this.running)
		{
			final Main main = this.main;

			synchronized (main.peers)
			{
				synchronized (main.entries)
				{
					for (SyncEntry entry : main.entries)
					{
						final Peer peer = this.connectTo(main.peers, entry.getAddress());
						if (peer != null)
						{
							peer.addSyncEntry(entry);
						}
					}
				}

				for (Iterator<Peer> iterator = main.peers.values().iterator(); iterator.hasNext(); )
				{
					final Peer peer = iterator.next();
					if (!peer.isConnected())
					{
						peer.closeSocket();
						iterator.remove();
						continue;
					}

					peer.synchronize();
					try
					{
						peer.handlePacket();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}

			try
			{
				Thread.sleep(5000);
			}
			catch (InterruptedException ignored)
			{
			}
		}
	}

	private Peer connectTo(Map<Address, Peer> peers, Address address)
	{
		Peer peer = peers.get(address);
		if (peer != null)
		{
			return peer;
		}

		try
		{
			peer = new Peer(address);
			peers.put(address, peer);
			peers.put(peer.address, peer);
		}
		catch (IOException e)
		{
			System.err.println("Cannot connect to " + address);
			e.printStackTrace();
		}

		return peer;
	}
}
