package com.clashsoft.p2psync;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class Main extends Application
{
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
					Controller.PORT = Integer.parseInt(s.substring(7));
				}
				catch (NumberFormatException ignored)
				{
				}
			}
			else if (s.startsWith("--data:"))
			{
				Controller.SAVE_FOLDER = s.substring(7);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void start(Stage primaryStage) throws IOException
	{
		FXMLLoader loader = new FXMLLoader();

		try (final InputStream inputStream = Main.class.getResource("main.fxml").openStream();)
		{
			Parent root = loader.load(inputStream);
			primaryStage.setTitle("P2PSync - Port " + Controller.PORT);
			primaryStage.setScene(new Scene(root, 800, 600));
			primaryStage.show();

			final Controller controller = loader.getController();
			primaryStage.setOnCloseRequest(e -> controller.close());
		}
	}
}
