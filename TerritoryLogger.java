/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.TerritoryZone;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.Level;

import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.IO.ReikaFileReader;
import Reika.DragonAPI.Instantiable.IO.ModLogger.LoggerOut;
import Reika.DragonAPI.Libraries.MathSci.ReikaDateHelper;


public class TerritoryLogger {

	public static final TerritoryLogger instance = new TerritoryLogger();

	private LoggerOut IOThread;

	private TerritoryLogger() {

	}

	public void init() {
		this.setOutput(new File(DragonAPICore.getMinecraftDirectory(), "logs/TerritoryZone/"+this.getFilename()+".log"));
	}

	private String getFilename() {
		return "Launch ["+ReikaDateHelper.getFormattedTimeFilesafe(DragonAPICore.getLaunchTime())+"]";
	}

	private void setOutput(File f) {
		File par = new File(f.getParent());
		if (!par.exists())
			par.mkdirs();
		this.purgeEmptyFiles(f);

		IOThread = new LoggerOut("Territory Zone Logger", f);
		IOThread.start();
	}

	private void purgeEmptyFiles(File f) {
		File[] arr = f.getParentFile().listFiles();
		for (int i = 0; i < arr.length; i++) {
			try {
				if (ReikaFileReader.isEmpty(arr[i]))
					arr[i].delete();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void log(String s) {
		s = this.getTimeStamp()+s;
		IOThread.addMessage(s, Level.INFO);
	}

	private String getTimeStamp() {
		return "["+ReikaDateHelper.getCurrentTime()+"] ";
	}

}
