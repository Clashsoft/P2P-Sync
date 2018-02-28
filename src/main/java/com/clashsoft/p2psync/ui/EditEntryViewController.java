package com.clashsoft.p2psync.ui;

import com.clashsoft.p2psync.Main;
import com.clashsoft.p2psync.SyncEntry;
import com.clashsoft.p2psync.net.Address;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class EditEntryViewController
{
	@FXML
	private TextField remoteAddressField;
	@FXML
	private TextField remotePortField;
	@FXML
	private TextField localPathField;
	@FXML
	private TextField remotePathField;

	@FXML
	private Button okButton;

	private Stage     stage;
	private Runnable  updateHandler;
	private SyncEntry entry;
	private boolean   create;

	public void setStage(Stage stage)
	{
		this.stage = stage;
	}

	public void setCreate(boolean create)
	{
		this.create = create;
		this.stage.setTitle(create ? "Create new Entry" : "Edit Entry");
	}

	public void setUpdateHandler(Runnable updateHandler)
	{
		this.updateHandler = updateHandler;
	}

	public void setEntry(SyncEntry entry)
	{
		this.entry = entry;

		this.update();
	}

	public static void open(SyncEntry entry, boolean create, Runnable update)
	{
		try
		{
			final FXMLLoader loader = new FXMLLoader(EditEntryViewController.class.getResource("EditEntryView.fxml"));
			final Parent parent = loader.load();
			final EditEntryViewController controller = loader.getController();
			final Stage stage = new Stage();

			controller.setStage(stage);
			controller.setCreate(create);
			controller.setUpdateHandler(update);
			controller.setEntry(entry);

			stage.setScene(new Scene(parent));
			stage.show();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@FXML
	private void initialize()
	{
		this.remotePortField.setText(Integer.toString(Main.PORT));

		this.remotePathField.setEditable(false);

		this.okButton.disableProperty().bind(
			this.remoteAddressField.textProperty().isEmpty().or(this.remotePortField.textProperty().isEmpty())
			                       .or(this.localPathField.textProperty().isEmpty()));
	}

	private void update()
	{
		final boolean outbound = this.entry.getType() == SyncEntry.Type.OUTBOUND;
		this.remoteAddressField.setEditable(outbound);
		this.remotePortField.setEditable(outbound);

		final Address address = this.entry.getAddress();

		this.remoteAddressField.setText(address.hostname);
		this.remotePortField.setText(Integer.toString(address.port));
		this.localPathField.setText(this.entry.getLocalFile());
		this.remotePathField.setText(this.entry.getRemoteFile());
	}

	@FXML
	private void onBrowseFileAction()
	{
		final FileChooser fileChooser = new FileChooser();
		final File file = fileChooser.showOpenDialog(this.stage);
		this.localPathField.setText(file.getAbsolutePath());
	}

	@FXML
	private void onBrowseDirectoryAction()
	{
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		final File file = directoryChooser.showDialog(this.stage);
		this.localPathField.setText(file.getAbsolutePath());
	}

	@FXML
	private void onOKAction()
	{
		final String remoteAddress = this.remoteAddressField.getText();
		final int remotePort = Integer.parseInt(this.remotePortField.getText());
		final String localPath = this.localPathField.getText();

		this.entry.setAddress(new Address(remoteAddress, remotePort));
		this.entry.setLocalFile(localPath);

		this.updateHandler.run();

		if (this.create)
		{
			// this.entry.getMain().addEntry(this.entry);
		}

		this.stage.close();
	}

	@FXML
	private void onCancelAction()
	{
		this.stage.close();
	}
}
