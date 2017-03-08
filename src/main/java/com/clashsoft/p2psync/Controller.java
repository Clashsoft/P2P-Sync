package com.clashsoft.p2psync;

import com.clashsoft.p2psync.net.Address;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
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

	private EntryController entryController;
	private Main            main;

	@SuppressWarnings("unchecked")
	public void initMain(Main main, EntryController entryController)
	{
		this.main = main;
		this.entryController = entryController;

		this.syncTable.setItems(main.entries);

		final ObservableList<TableColumn<SyncEntry, ?>> columns = this.syncTable.getColumns();
		columns.get(0).setCellValueFactory(new PropertyValueFactory("address"));
		columns.get(1).setCellValueFactory(new PropertyValueFactory("localFile"));
		columns.get(2).setCellValueFactory(new PropertyValueFactory("remoteFile"));

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
		synchronized (this.main.entries)
		{
			final SyncEntry entry = new SyncEntry(new Address("", Main.PORT), "", "");
			if (this.entryController.open(entry))
			{
				this.main.entries.add(entry);
			}
		}
	}

	@FXML
	public void handleEditEntry()
	{
		synchronized (this.main.entries)
		{
			final int index = this.syncTable.getSelectionModel().getSelectedIndex();
			final SyncEntry selectedEntry = this.main.entries.get(index);
			if (this.entryController.open(selectedEntry))
			{
				// trigger an update to the ObservableList
				this.main.entries.set(index, selectedEntry);
			}
		}
	}

	@FXML
	public void handleDuplicateEntry()
	{
		synchronized (this.main.entries)
		{
			final SyncEntry copy = this.getSelectedEntry().copy();
			if (this.entryController.open(copy))
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
