/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2016
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.TerritoryZone;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.Libraries.IO.ReikaFormatHelper;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;


public class FileLogger {

	public static final FileLogger instance = new FileLogger();

	private static final String NEWLINE = System.getProperty("line.separator");

	private BufferedWriter outputFile;
	private LoggerOut IOThread;
	private String destination;

	private FileLogger() {

	}

	public void init() {
		this.setOutput(DragonAPICore.getMinecraftDirectoryString()+"/logs/TerritoryZone/"+this.getFilename()+".log");
	}

	private String getFilename() {
		return "Launch ["+ReikaFormatHelper.getFormattedTimeFilesafe(DragonAPICore.getLaunchTime())+"]";
	}

	private void setOutput(String file) {
		for (int i = 0; i < 20; i++) {
			try {
				this.flushOutput();
				File f = new File(file);
				File par = new File(f.getParent());
				if (!par.exists())
					par.mkdirs();
				f.createNewFile();
				destination = f.getAbsolutePath();
				this.setOutput(new BufferedWriter(new PrintWriter(f)));
				return;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		throw new RuntimeException("TERRITORYZONE: Could not create log file!");
	}

	private void setOutput(BufferedWriter buf) {
		outputFile = buf;
		IOThread = new LoggerOut();
		Thread th = new Thread(IOThread);
		th.setDaemon(true);
		th.setName("Territory Zone Logger");
		th.start();
	}

	public void log(String s) {
		s = this.getTimeStamp()+s;
		IOThread.addMessage(s);
	}

	private void flushOutput() {
		if (outputFile != null) {
			IOThread.terminated = true;
		}
	}

	private String getTimeStamp() {
		return "["+ReikaFormatHelper.getCurrentTime()+"] ";
	}

	private class LoggerOut implements Runnable {

		private ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue();
		private boolean terminated = false;

		private LoggerOut() {

		}

		private void addMessage(String s) {
			messages.add(s);
		}

		@Override
		public void run() {
			while (!terminated || !messages.isEmpty()) { //killed by MC if closes (deamon thread)
				Collection<String> printed = new ArrayList();
				for (String s : messages) {
					try {
						outputFile.write(s+NEWLINE);
						outputFile.flush();
						printed.add(s);
					}
					catch (IOException e) {
						TerritoryZone.logger.logError("Could not output logger line to its IO destination '"+destination+"'!");
						ReikaJavaLibrary.pConsole(s);
						e.printStackTrace();
					}
				}
				messages.removeAll(printed);
			}
			try {
				outputFile.close();
			}
			catch (IOException e) {
				e.printStackTrace();
				TerritoryZone.logger.logError("Could not close logger stream!");
			}
		}

	}

}
