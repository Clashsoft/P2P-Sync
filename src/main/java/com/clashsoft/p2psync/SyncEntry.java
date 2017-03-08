package com.clashsoft.p2psync;

import com.clashsoft.p2psync.net.Address;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SyncEntry
{
	private Address address;

	private String localFile;
	private String remoteFile;

	public SyncEntry()
	{
	}

	public SyncEntry(Address address, String localFile, String remoteFile)
	{
		this.address = address;
		this.localFile = localFile;
		this.remoteFile = remoteFile;
	}

	public Address getAddress()
	{
		return this.address;
	}

	public void setAddress(Address address)
	{
		this.address = address;
	}

	public String getLocalFile()
	{
		return this.localFile;
	}

	public void setLocalFile(String localFile)
	{
		this.localFile = localFile;
	}

	public String getRemoteFile()
	{
		return this.remoteFile;
	}

	public void setRemoteFile(String remoteFile)
	{
		this.remoteFile = remoteFile;
	}

	public void write(DataOutputStream output) throws IOException
	{
		this.address.write(output);
		output.writeUTF(this.localFile);
		output.writeUTF(this.remoteFile);
	}

	public void read(DataInputStream input) throws IOException
	{
		this.address = Address.read(input);
		this.localFile = input.readUTF();
		this.remoteFile = input.readUTF();
	}

	public SyncEntry copy()
	{
		return new SyncEntry(this.address, this.localFile, this.remoteFile);
	}
}
