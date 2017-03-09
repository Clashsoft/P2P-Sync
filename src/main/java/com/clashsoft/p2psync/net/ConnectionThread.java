package com.clashsoft.p2psync.net;

import com.clashsoft.p2psync.Main;
import com.clashsoft.p2psync.SyncEntry;

import java.io.IOException;
import java.util.Iterator;

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
		long currentTime = System.currentTimeMillis();

		while (this.running)
		{
			final Main main = this.main;

			synchronized (main.peers)
			{
				// Create Peer Map
				synchronized (main.entries)
				{
					for (SyncEntry entry : main.entries)
					{
						if (entry.getType() != SyncEntry.Type.OUTBOUND)
						{
							continue;
						}

						final Peer peer = this.getPeer(entry.getAddress());
						if (peer != null)
						{
							peer.addSyncEntry(entry);
						}
					}
				}

				// Connect and communicate with peers
				for (Iterator<Peer> iterator = main.peers.values().iterator(); iterator.hasNext(); )
				{
					final Peer peer = iterator.next();

					try
					{
						peer.sendOffer();
						peer.handlePacket();
					}
					catch (IOException e)
					{
						peer.closeSocket();
						iterator.remove();
					}
				}
			}

			final long newTime = System.currentTimeMillis();
			final long elapsed = newTime - currentTime;
			currentTime = newTime;
			this.trySleep(Constants.TIMEOUT - elapsed);
		}
	}

	private void trySleep(long elapsed)
	{
		if (elapsed <= 0)
		{
			return;
		}

		try
		{
			Thread.sleep(elapsed);
		}
		catch (InterruptedException ignored)
		{
		}
	}

	private Peer getPeer(Address address)
	{
		Peer peer = this.main.peers.get(address);
		if (peer != null)
		{
			return peer;
		}

		peer = new Peer(address, this.main);
		this.main.peers.put(address, peer);
		return peer;
	}
}
