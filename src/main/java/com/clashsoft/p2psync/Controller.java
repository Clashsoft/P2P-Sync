package com.clashsoft.p2psync;

import com.clashsoft.p2psync.net.Address;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class Controller
{

	@FXML
	public TableView<SyncEntry> syncTable;

	@FXML
	public Button editEntryButton;
	@FXML
	public Button duplicateEntryButton;
	@FXML
	public Button deleteEntryButton;

	@FXML
	public TableColumn<SyncEntry, Boolean>        enableColumn;
	@FXML
	public TableColumn<SyncEntry, SyncEntry.Type> typeColumn;
	@FXML
	public TableColumn<SyncEntry, Address>        remoteAddressColumn;
	@FXML
	public TableColumn<SyncEntry, String>         localFileColumn;
	@FXML
	public TableColumn<SyncEntry, String>         remoteFileColumn;

	private EntryController entryController;
	private Main            main;

	public void initMain(Main main, EntryController entryController)
	{
		this.main = main;
		this.entryController = entryController;

		this.syncTable.setItems(main.entries);

		this.enableColumn.setCellValueFactory(c -> c.getValue().enabledProperty());
		this.enableColumn.setCellFactory(CheckBoxTableCell.forTableColumn(this.enableColumn));

		this.typeColumn.setCellValueFactory(c -> c.getValue().typeProperty());
		this.remoteAddressColumn.setCellValueFactory(c -> c.getValue().addressProperty());
		this.localFileColumn.setCellValueFactory(c -> c.getValue().localFileProperty());
		this.remoteFileColumn.setCellValueFactory(c -> c.getValue().remoteFileProperty());

		final BooleanBinding unselected = this.syncTable.getSelectionModel().selectedItemProperty().isNull();
		this.editEntryButton.disableProperty().bind(unselected);
		this.duplicateEntryButton.disableProperty().bind(unselected);
		this.deleteEntryButton.disableProperty().bind(unselected);
	}

	private SyncEntry getSelectedEntry()
	{
		return this.syncTable.getSelectionModel().getSelectedItem();
	}

	@FXML
	public void handleNewEntry()
	{
		final SyncEntry entry = new SyncEntry(new Address("", Main.PORT), "", "", SyncEntry.Type.OUTBOUND);
		if (this.entryController.open(entry))
		{
			synchronized (this.main.entries)
			{
				this.main.entries.add(entry);
			}
		}
	}

	@FXML
	public void handleEditEntry()
	{
		this.entryController.open(this.getSelectedEntry());
	}

	@FXML
	public void handleDuplicateEntry()
	{
		final SyncEntry copy = this.getSelectedEntry().copy();
		if (this.entryController.open(copy))
		{
			synchronized (this.main.entries)
			{
				this.main.entries.add(copy);
			}
		}
	}

	@FXML
	public void handleDeleteEntry()
	{
		synchronized (this.main.entries)
		{
			this.main.entries.remove(this.getSelectedEntry());
		}
	}

	@FXML
	public void handleTableKeyTyped(KeyEvent event)
	{
		// DELETE or Meta-Backspace or Ctrl-Backspace
		if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE)
		{
			this.handleDeleteEntry();
		}
	}
}
