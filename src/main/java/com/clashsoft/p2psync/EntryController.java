package com.clashsoft.p2psync;

import com.clashsoft.p2psync.net.Address;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;

public class EntryController
{
	private Stage dialog;

	@FXML
	public TextField remoteHostname;
	@FXML
	public TextField remotePort;
	@FXML
	public TextField localFile;
	@FXML
	public TextField remoteFile;

	@FXML
	public Button submitButton;

	private SyncEntry entry;
	private boolean submitted;

	public void initialize()
	{
		this.remotePort.setText(Integer.toString(Main.PORT));

		this.submitButton.disableProperty().bind(Bindings.or(Bindings.or(this.remoteHostname.textProperty().isEmpty(),
		                                                                 this.remotePort.textProperty().isEmpty()),
		                                                     Bindings.or(this.localFile.textProperty().isEmpty(),
		                                                                 this.remoteFile.textProperty().isEmpty())));
	}

	public void init(Window window, Parent root)
	{
		Stage dialog = new Stage();
		dialog.initOwner(window);
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.setTitle("Edit Sync Configuration");
		dialog.setScene(new Scene(root));
		this.dialog = dialog;
	}

	public boolean open(SyncEntry entry)
	{
		this.entry = entry;
		this.submitted = false;

		final Address address = entry.getAddress();

		this.remoteHostname.setText(address.hostname);
		this.remotePort.setText(Integer.toString(address.port));
		this.localFile.setText(entry.getLocalFile());
		this.remoteFile.setText(entry.getRemoteFile());

		this.dialog.showAndWait();
		return this.submitted;
	}

	@FXML
	public void handleSubmit()
	{
		try
		{
			this.entry
				.setAddress(new Address(this.remoteHostname.getText(), Integer.parseInt(this.remotePort.getText())));
			this.entry.setLocalFile(this.localFile.getText());
			this.entry.setRemoteFile(this.remoteFile.getText());
			this.submitted = true;

			this.dialog.close();
		}
		catch (NumberFormatException ignored)
		{
		}
	}

	@FXML
	public void handleCancel()
	{
		this.dialog.close();
	}

	@FXML
	public void handleOpenLocalFile()
	{
		FileChooser fileChooser = new FileChooser();
		File file = fileChooser.showOpenDialog(null);
		this.localFile.setText(file.getAbsolutePath());
	}
}
