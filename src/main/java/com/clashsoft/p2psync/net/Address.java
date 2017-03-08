package com.clashsoft.p2psync.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Address
{
	public final String hostname;
	public final int port;

	public Address(String hostname, int port)
	{
		this.hostname = hostname;
		this.port = port;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Address))
		{
			return false;
		}

		Address address = (Address) o;

		return this.port == address.port && this.hostname.equals(address.hostname);
	}

	@Override
	public int hashCode()
	{
		int result = this.hostname.hashCode();
		result = 31 * result + this.port;
		return result;
	}

	@Override
	public String toString()
	{
		return this.hostname + ":" + this.port;
	}

	public static Address fromSocket(Socket socket)
	{
		return new Address(socket.getInetAddress().getHostAddress(), socket.getPort());
	}

	public static Address read(DataInputStream input) throws IOException
	{
		return new Address(input.readUTF(), input.readInt());
	}

	public void write(DataOutputStream output) throws IOException
	{
		output.writeUTF(this.hostname);
		output.writeInt(this.port);
	}
}
