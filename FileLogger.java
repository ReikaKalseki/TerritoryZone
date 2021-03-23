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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.IO.ReikaFileReader;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.DragonAPI.Libraries.MathSci.ReikaDateHelper;


public class FileLogger {

	public static final FileLogger instance = new FileLogger();

	private static final String NEWLINE = System.getProperty("line.separator");

	private BufferedWriter outputFile;
	private LoggerOut IOThread;
	private String destination;

	private FileLogger() {

	}

	public void init() {
		this.setOutput(new File(DragonAPICore.getMinecraftDirectory(), "logs/TerritoryZone/"+this.getFilename()+".log"));
	}

	private String getFilename() {
		return "Launch ["+ReikaDateHelper.getFormattedTimeFilesafe(DragonAPICore.getLaunchTime())+"]";
	}

	private void setOutput(File f) {
		for (int i = 0; i < 20; i++) {
			try {
				this.flushOutput();
				File par = new File(f.getParent());
				if (!par.exists())
					par.mkdirs();
				f.createNewFile();
				destination = f.getCanonicalPath();
				this.setOutput(new BufferedWriter(new PrintWriter(f)));
				this.purgeEmptyFiles(f);
				return;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		throw new RuntimeException("TERRITORYZONE: Could not create log file!");
	}

	private void purgeEmptyFiles(File f) throws IOException {
		File[] arr = f.getParentFile().listFiles();
		for (int i = 0; i < arr.length; i++) {
			if (ReikaFileReader.isEmpty(arr[i])) {
				arr[i].delete();
			}
		}
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
		return "["+ReikaDateHelper.getCurrentTime()+"] ";
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
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					//e.printStackTrace();
				}
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
