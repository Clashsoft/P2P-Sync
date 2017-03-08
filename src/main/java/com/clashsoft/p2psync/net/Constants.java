package com.clashsoft.p2psync.net;

public class Constants
{
	public static final int OFFER    = 1;
	public static final int REQUEST  = 2;
	public static final int RESPONSE = 3;
	public static final int TIMEOUT  = 5000;

	public static String getSaveFolder()
	{
		String os = System.getProperty("os.name").toUpperCase();
		if (os.contains("WIN"))
		{
			return System.getenv("APPDATA") + "/P2P-Sync";
		}
		else if (os.contains("MAC"))
		{
			return System.getProperty("user.home") + "/Library/Application Support/P2P-Sync";
		}
		else if (os.contains("NUX"))
		{
			return System.getProperty("user.home") + "/P2P-Sync";
		}
		return System.getProperty("user.dir") + "/P2P-Sync";
	}
}
