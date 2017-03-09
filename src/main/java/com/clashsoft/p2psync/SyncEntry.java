package com.clashsoft.p2psync;

import com.clashsoft.p2psync.net.Address;
import javafx.beans.Observable;
import javafx.beans.property.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SyncEntry
{
	public enum Type
	{
		OUTBOUND("Outbound"), INBOUND("Inbound");

		private final String displayName;

		Type(String displayName)
		{
			this.displayName = displayName;
		}

		public String getDisplayName()
		{
			return this.displayName;
		}

		public static Type read(DataInputStream input) throws IOException
		{
			switch (input.readByte())
			{
			case 0:
				return OUTBOUND;
			case 1:
				return INBOUND;
			}
			return null;
		}

		public void write(DataOutputStream output) throws IOException
		{
			switch (this)
			{
			case OUTBOUND:
				output.writeByte(0);
				return;
			case INBOUND:
				output.writeByte(1);
				return;
			}
		}

		@Override
		public String toString()
		{
			return this.displayName;
		}
	}

	private Property<Address> address = new SimpleObjectProperty<>(this, "address");

	private StringProperty localFile  = new SimpleStringProperty(this, "localFile");
	private StringProperty remoteFile = new SimpleStringProperty(this, "remoteFile");

	private ObjectProperty<Type> type = new SimpleObjectProperty<>(this, "type", Type.OUTBOUND);

	private BooleanProperty enabled = new SimpleBooleanProperty(this, "enabled", false);

	public SyncEntry()
	{
	}

	public SyncEntry(Address address, String localFile, String remoteFile, Type type)
	{
		this.address.setValue(address);
		this.localFile.setValue(localFile);
		this.remoteFile.setValue(remoteFile);
		this.type.setValue(type);
	}

	public Observable[] properties()
	{
		return new Observable[] { this.address, this.localFile, this.remoteFile, this.type, this.enabled };
	}

	public Type getType()
	{
		return this.type.get();
	}

	public void setType(Type type)
	{
		this.type.setValue(type);
	}

	public ObjectProperty<Type> typeProperty()
	{
		return this.type;
	}

	public boolean isEnabled()
	{
		return this.enabled.get();
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled.set(enabled);
	}

	public BooleanProperty enabledProperty()
	{
		return this.enabled;
	}

	public Address getAddress()
	{
		return this.address.getValue();
	}

	public void setAddress(Address address)
	{
		this.address.setValue(address);
	}

	public Property<Address> addressProperty()
	{
		return this.address;
	}

	public String getLocalFile()
	{
		return this.localFile.get();
	}

	public void setLocalFile(String localFile)
	{
		this.localFile.set(localFile);
	}

	public StringProperty localFileProperty()
	{
		return this.localFile;
	}

	public String getRemoteFile()
	{
		return this.remoteFile.get();
	}

	public void setRemoteFile(String remoteFile)
	{
		this.remoteFile.set(remoteFile);
	}

	public StringProperty remoteFileProperty()
	{
		return this.remoteFile;
	}

	public void write(DataOutputStream output) throws IOException
	{
		output.writeBoolean(this.isEnabled());
		this.getType().write(output);

		this.getAddress().write(output);
		output.writeUTF(this.getLocalFile());
		output.writeUTF(this.getRemoteFile());
	}

	public void read(DataInputStream input, int version) throws IOException
	{
		if (version >= 2)
		{
			this.setEnabled(input.readBoolean());
			this.setType(Type.read(input));
		}

		this.setAddress(Address.read(input));
		this.setLocalFile(input.readUTF());
		this.setRemoteFile(input.readUTF());
	}

	public SyncEntry copy()
	{
		final SyncEntry entry = new SyncEntry(this.getAddress(), this.getLocalFile(), this.getRemoteFile(),
		                                      this.getType());
		entry.setEnabled(this.isEnabled());
		return entry;
	}
}
