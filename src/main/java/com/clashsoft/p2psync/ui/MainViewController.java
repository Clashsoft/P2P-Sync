package com.clashsoft.p2psync.ui;

import com.clashsoft.p2psync.SyncEntry;
import com.clashsoft.p2psync.net.Address;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class MainViewController
{
	@FXML
	private TableView<SyncEntry> table;

	@FXML
	private TableColumn<SyncEntry, Boolean>        enableColumn;
	@FXML
	private TableColumn<SyncEntry, SyncEntry.Type> typeColumn;
	@FXML
	private TableColumn<SyncEntry, Address>        remoteAddressColumn;
	@FXML
	private TableColumn<SyncEntry, Address>        localFileColumn;
	@FXML
	private TableColumn<SyncEntry, String>         remoteFileColumn;
	@FXML
	private TableColumn<SyncEntry, String>         statusColumn;

	@FXML
	private void onSearchAction()
	{
	}

	@FXML
	private void onFilterAction()
	{
	}

	@FXML
	private void onAddAction()
	{
	}

	@FXML
	private void onRemoveAction()
	{
	}

	@FXML
	private void onEditAction()
	{
	}
}
