package com.clashsoft.p2psync;

import com.clashsoft.p2psync.net.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Main extends Application
{
	public static int PORT;
	public static String SAVE_FOLDER = Constants.getSaveFolder();

	private ServerThread serverThread;

	private ConnectionThread connectionThread;

	public final ObservableList<SyncEntry> entries = FXCollections.observableArrayList(SyncEntry::properties);
	public final Map<Address, Peer>        peers   = new HashMap<>();

	public static void main(String[] args)
	{
		parseArgs(args);

		launch(args);
	}

	private static void parseArgs(String[] args)
	{
		for (String s : args)
		{
			if (s.startsWith("--port:"))
			{
				try
				{
					PORT = Integer.parseInt(s.substring(7));
				}
				catch (NumberFormatException ignored)
				{
				}
			}
			else if (s.startsWith("--data:"))
			{
				SAVE_FOLDER = s.substring(7);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void start(Stage primaryStage) throws IOException
	{

		try (final InputStream inputStream = Main.class.getResource("main.fxml").openStream();)
		{
			FXMLLoader loader = new FXMLLoader();
			final Parent root = loader.load(inputStream);
			primaryStage.titleProperty()
			            .bind(Bindings.concat("P2P-Sync — Port: ", Bindings.createIntegerBinding(() -> PORT)));
			primaryStage.setScene(new Scene(root));
			primaryStage.show();

			final Controller controller = loader.getController();
			final EntryController entryController = this.loadEntryController(primaryStage);

			controller.initMain(this, entryController);

			primaryStage.setOnCloseRequest(e -> this.close());

			this.loadEntries();
			this.entries.addListener((ListChangeListener<SyncEntry>) c -> this.saveEntries());

			this.serverThread = new ServerThread(Main.PORT, this);
			this.serverThread.start();
			this.connectionThread = new ConnectionThread(Main.PORT, this);
			this.connectionThread.start();
		}
	}

	private EntryController loadEntryController(Window window) throws IOException
	{
		try (final InputStream entryStream = Main.class.getResource("editentry.fxml").openStream())
		{
			final FXMLLoader loader = new FXMLLoader();
			final Parent root = loader.load(entryStream);
			final EntryController controller = loader.getController();
			controller.init(window, root);
			return controller;
		}
	}

	public void close()
	{
		this.peers.forEach((a, p) -> p.closeSocket());
		this.serverThread.close();
		this.connectionThread.close();
	}

	private void loadEntries()
	{
		if (Main.SAVE_FOLDER == null)
		{
			return;
		}

		final File file = new File(Main.SAVE_FOLDER, "data.bin");
		if (!file.exists())
		{
			return;
		}

		System.out.println("Loading from " + file);

		try (DataInputStream input = new DataInputStream(new FileInputStream(file)))
		{
			final int version = input.read();
			final int entries = input.readInt();

			synchronized (this.entries)
			{
				for (int i = 0; i < entries; i++)
				{
					SyncEntry entry = new SyncEntry();
					entry.read(input, version);
					this.entries.add(entry);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void saveEntries()
	{
		if (Main.SAVE_FOLDER == null)
		{
			return;
		}

		final File file = new File(Main.SAVE_FOLDER, "data.bin");

		System.out.println("Saving to " + file);

		try
		{
			file.getParentFile().mkdirs();
			file.createNewFile();

			try (DataOutputStream output = new DataOutputStream(new FileOutputStream(file)))
			{
				synchronized (this.entries)
				{
					output.write(2); // version
					output.writeInt(this.entries.size());
					for (SyncEntry entry : this.entries)
					{
						entry.write(output);
					}
				}
			}
		}
		catch (IOException e)
		{
			System.err.println("Failed to save data: " + e.getMessage());
		}
	}

	public SyncEntry getInboundEntry(Address address, String localPath, String remotePath)
	{
		synchronized (this.entries)
		{
			for (SyncEntry entry : this.entries)
			{
				if (entry.getAddress().hostname.equals(address.hostname) //
					    && entry.getRemoteFile().equals(remotePath) && entry.getLocalFile().equals(localPath))
				{
					return entry;
				}
			}

			SyncEntry entry = new SyncEntry(new Address(address.hostname, 0), localPath, remotePath, SyncEntry.Type.INBOUND);
			Platform.runLater(() -> this.entries.add(entry));
			return entry;
		}
	}
}
