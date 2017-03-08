package com.clashsoft.p2psync;

import com.clashsoft.p2psync.net.*;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Controller
{
	public static int PORT;

	public static String SAVE_FOLDER = Constants.getSaveFolder();

	@FXML
	public TableView<SyncEntry> syncTable;

	@FXML
	public Button newEntryButton;

	@FXML
	public TextField remoteHostname;
	@FXML
	public TextField remotePort;
	@FXML
	public TextField localFile;
	@FXML
	public TextField remoteFile;

	@FXML
	public Label errorLabel;

	public final ObservableList<SyncEntry> entries = FXCollections.observableArrayList();
	public final Map<Address, Peer>        peers   = new HashMap<>();

	private ServerThread     serverThread;
	private ConnectionThread connectionThread;

	@SuppressWarnings("unchecked")
	@FXML
	public void initialize()
	{
		this.syncTable.setItems(this.entries);

		final ObservableList<TableColumn<SyncEntry, ?>> columns = this.syncTable.getColumns();
		columns.get(0).setCellValueFactory(new PropertyValueFactory("address"));
		columns.get(1).setCellValueFactory(new PropertyValueFactory("localFile"));
		columns.get(2).setCellValueFactory(new PropertyValueFactory("remoteFile"));

		this.remotePort.setText(Integer.toString(PORT));

		this.newEntryButton.disableProperty().bind(Bindings.or(Bindings.or(this.remoteHostname.textProperty().isEmpty(),
		                                                                   this.remotePort.textProperty().isEmpty()),
		                                                       Bindings.or(this.localFile.textProperty().isEmpty(),
		                                                                   this.remoteFile.textProperty().isEmpty())));

		this.loadEntries();
		this.entries.addListener((ListChangeListener<SyncEntry>) c -> this.saveEntries());

		this.serverThread = new ServerThread(PORT, this);
		this.serverThread.start();
		this.connectionThread = new ConnectionThread(PORT, this);
		this.connectionThread.start();
	}

	public void close()
	{
		this.peers.forEach((a, p) -> p.closeSocket());
		this.serverThread.close();
		this.connectionThread.close();
	}

	private void loadEntries()
	{
		if (SAVE_FOLDER == null)
		{
			return;
		}

		final File file = new File(SAVE_FOLDER, "data.bin");
		if (!file.exists())
		{
			return;
		}

		System.out.println("Loading from " + file);

		try (DataInputStream input = new DataInputStream(new FileInputStream(file)))
		{
			@SuppressWarnings("unused") final int version = input.read();
			final int entries = input.readInt();

			synchronized (this.entries)
			{
				for (int i = 0; i < entries; i++)
				{
					SyncEntry entry = new SyncEntry();
					entry.read(input);
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
		if (SAVE_FOLDER == null)
		{
			return;
		}

		final File file = new File(SAVE_FOLDER, "data.bin");
		//noinspection ResultOfMethodCallIgnored
		file.getParentFile().mkdirs();

		System.out.println("Saving to " + file);

		try (DataOutputStream output = new DataOutputStream(new FileOutputStream(file)))
		{
			synchronized (this.entries)
			{
				output.write(1); // version
				output.writeInt(this.entries.size());
				for (SyncEntry entry : this.entries)
				{
					entry.write(output);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@FXML
	public void handleNewEntry()
	{
		try
		{
			final int port = Integer.parseInt(this.remotePort.getText());
			final Address address = new Address(this.remoteHostname.getText(), port);

			this.entries.add(new SyncEntry(address, this.localFile.getText(), this.remoteFile.getText()));

			this.localFile.clear();
			this.remoteFile.clear();
			this.errorLabel.setText("");
		}
		catch (NumberFormatException ex)
		{
			this.errorLabel.setText("Invalid Port: " + this.remotePort.getText());
		}
	}

	@FXML
	public void handleOpenLocalFile()
	{
		FileChooser fileChooser = new FileChooser();
		File file = fileChooser.showOpenDialog(null);
		this.localFile.setText(file.getAbsolutePath());
	}

	@FXML
	public void handleTableKeyTyped(KeyEvent event)
	{
		// DELETE or Meta-Backspace or Ctrl-Backspace
		if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE)
		{
			synchronized (this.entries)
			{
				final SyncEntry entry = this.syncTable.getSelectionModel().getSelectedItem();
				this.entries.remove(entry);
			}
		}
	}
}
