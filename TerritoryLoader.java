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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import Reika.DragonAPI.IO.ReikaFileReader;
import Reika.DragonAPI.Instantiable.Data.Immutable.CommutativePair;
import Reika.DragonAPI.Instantiable.Data.Immutable.WorldLocation;
import Reika.DragonAPI.Instantiable.Data.Maps.MultiMap;
import Reika.DragonAPI.Libraries.Java.ReikaStringParser;
import Reika.TerritoryZone.Territory.Owner;
import Reika.TerritoryZone.Territory.Protections;
import Reika.TerritoryZone.Territory.TerritoryShape;
import Reika.TerritoryZone.Event.TerritoryCollisionEvent;
import Reika.TerritoryZone.Event.TerritoryRegisterEvent;
import Reika.TerritoryZone.Event.TerritoryUnregisterEvent;

public class TerritoryLoader {

	public static final TerritoryLoader instance = new TerritoryLoader();

	private final MultiMap<UUID, Territory> ownerMap = new MultiMap();
	private final Collection<Territory> territories = new ArrayList();

	private TerritoryLoader() {

	}

	public void addTerritory(Territory t) {
		this.addTerritory(t, true);
	}

	private void addTerritory(Territory t, boolean fileIO) {
		for (UUID id : t.getOwnerIDs())
			ownerMap.addValue(id, t);
		territories.add(t);
		MinecraftForge.EVENT_BUS.post(new TerritoryRegisterEvent(t));
		if (MinecraftServer.getServer() != null && MinecraftServer.getServer().isServerRunning())
			TerritoryDispatcher.instance.sendTerritoriesToAll();
		if (fileIO) {
			File f = new File(this.getFullSavePath());
			ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true);
			li.add(t.getFileString());
			try {
				f.delete();
				f.createNewFile();
			}
			catch (IOException e) {
				TerritoryZone.logger.logError("Could not refresh territory file: "+e.toString());
				e.printStackTrace();
			}
			ReikaFileReader.writeLinesToFile(f, li, true);
		}
	}

	public void removeTerritory(Territory t) {
		for (UUID id : t.getOwnerIDs())
			ownerMap.remove(id, t);
		territories.remove(t);
		MinecraftForge.EVENT_BUS.post(new TerritoryUnregisterEvent(t));
		TerritoryDispatcher.instance.sendTerritoriesToAll();
		File f = new File(this.getFullSavePath());
		ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true);
		li.remove(t.getFileString());
		try {
			f.delete();
			f.createNewFile();
		}
		catch (IOException e) {
			TerritoryZone.logger.logError("Could not refresh territory file: "+e.toString());
			e.printStackTrace();
		}
		ReikaFileReader.writeLinesToFile(f, li, true);
	}

	public Collection<Territory> getTerritoriesFor(EntityPlayer ep) {
		return this.getTerritoriesFor(ep.getUniqueID());
	}

	public Collection<Territory> getTerritoriesFor(UUID uid) {
		return Collections.unmodifiableCollection(ownerMap.get(uid));
	}

	public Collection<Territory> getTerritories() {
		return Collections.unmodifiableCollection(territories);
	}

	public final String getSaveFileName() {
		return "TerritoryZone_zones.cfg";
	}

	public final String getFullSavePath() {
		return TerritoryZone.config.getConfigFolder().getAbsolutePath()+"/"+this.getSaveFileName();
	}

	public void load() {
		territories.clear();
		TerritoryZone.logger.log("Loading zone config.");
		File f = new File(this.getFullSavePath());
		if (!f.exists())
			if (!this.createZoneFile(f))
				return;
		try {
			BufferedReader p = ReikaFileReader.getReader(f);
			String line = "";
			while (line != null) {
				line = p.readLine();
				if (line != null && !line.isEmpty() && !line.startsWith("//")) {
					try {
						Territory entry = this.parseString(line);
						if (entry != null) {
							this.addTerritory(entry, false);
							TerritoryZone.logger.log("Added zone entry "+entry);
						}
						else {
							TerritoryZone.logger.logError("Malformed zone entry: "+line);
						}
					}
					catch (Exception e) {
						TerritoryZone.logger.logError("Malformed zone entry ["+e.getLocalizedMessage()+"]: '"+line+"'");
						e.printStackTrace();
					}
				}
			}
			p.close();
		}
		catch (Exception e) {
			TerritoryZone.logger.log(e.getMessage()+", and it caused the read to fail!");
			e.printStackTrace();
		}
		this.verify();
	}

	private void verify() {
		Collection<CommutativePair<Territory>> pairs = new HashSet();
		for (Territory t : territories) {
			for (Territory t2 : territories) {
				if (t != t2 && !pairs.contains(new CommutativePair(t, t2)) && t.intersects(t2)) {
					TerritoryZone.logger.log("Two zones intersect! "+t+" & "+t2);
					pairs.add(new CommutativePair(t, t2));
					MinecraftForge.EVENT_BUS.post(new TerritoryCollisionEvent(t, t2));
				}
			}
		}
	}

	private boolean createZoneFile(File f) {
		try {
			f.createNewFile();
			PrintWriter p = new PrintWriter(f);
			this.writeCommentLine(p, "-------------------------------");
			this.writeCommentLine(p, " TerritoryZone Territory Loader ");
			this.writeCommentLine(p, "-------------------------------");
			this.writeCommentLine(p, "");
			this.writeCommentLine(p, "Use this file to specify the territory zones.");
			this.writeCommentLine(p, "Specify one per line, and format them as 'DimID, x, y, z, size, color, enforcement, logging, shape, [ownerName, ownerUUID]...'");
			this.writeCommentLine(p, "Zone Shapes:");
			for (int i = 0; i < TerritoryShape.list.length; i++) {
				TerritoryShape pr = TerritoryShape.list[i];
				this.writeCommentLine(p, pr.name()+" - "+pr.desc);
			}
			this.writeCommentLine(p, "");
			this.writeCommentLine(p, "Colors must be hex codes.");
			this.writeCommentLine(p, "");
			this.writeCommentLine(p, "Enforcement and Logging choose protections, and are bitflag-based. Each protection type has a number associated with it;");
			this.writeCommentLine(p, "Each type's number is twice that of the previous type. Specify the sum of all selected numbers to choose desired protections.");
			this.writeCommentLine(p, "Selecting zero will disable all protection types. Negative logging numbers will trigger a chat message to the territory owner.");
			for (int i = 0; i < Protections.list.length; i++) {
				Protections pr = Protections.list[i];
				this.writeCommentLine(p, pr.desc+" - "+(1 << pr.ordinal()));
			}
			this.writeCommentLine(p, "");
			this.writeCommentLine(p, "Sample Lines:");
			this.writeCommentLine(p, "\t0, -10, 64, 120, 32, 0xff0000, 11, 0, CUBE, SomePlayer, 504e35e4-ee36-45e0-b1d3-7ad419311644");
			this.writeCommentLine(p, "\t7, 1023, 90, -1304, 128, 0xffffff, 15, 16, CYLINDER, TheAdmin, 9f25640a-0e1a-4eef-ba21-66a99b1de20a, User2, 759fc6d2-1868-4c90-908c-81bf9b3cd973");
			this.writeCommentLine(p, "");
			this.writeCommentLine(p, "Entries missing any element, or with less than one owner, are incorrect.");
			this.writeCommentLine(p, "Incorrectly formatted lines will be ignored and will log an error in the console.");
			this.writeCommentLine(p, "Lines beginning with '//' are comments and will be ignored, as will empty lines. Spaces are stripped.");
			this.writeCommentLine(p, "====================================================================================");
			p.append("\n");
			p.close();
			return true;
		}
		catch (Exception e) {
			TerritoryZone.logger.logError("Could not generate Territory Config.");
			e.printStackTrace();
			return false;
		}
	}

	private Territory parseString(String s) throws Exception {
		s = ReikaStringParser.stripSpaces(s);
		String[] parts = s.split(",");
		if (parts.length < 11)
			throw new IllegalArgumentException("Invalid parameter count.");
		TerritoryShape sh = TerritoryShape.valueOf(parts[8].toUpperCase());
		if (sh == null)
			throw new IllegalArgumentException("Invalid shape.");
		int dim = Integer.parseInt(parts[0]);
		int x = Integer.parseInt(parts[1]);
		int y = Integer.parseInt(parts[2]);
		int z = Integer.parseInt(parts[3]);
		int r = Integer.parseInt(parts[4]);
		if (parts[5].startsWith("0x"))
			parts[5] = parts[5].substring(2);
		int clr = Integer.parseInt(parts[5], 16);
		int enf = Integer.parseInt(parts[6]);
		int log = Integer.parseInt(parts[7]);
		return new Territory(new WorldLocation(dim, x, y, z), r, clr, enf, log, sh, this.parseOwners(parts));
	}

	private Collection<Owner> parseOwners(String[] parts) {
		Collection<Owner> c = new ArrayList();
		for (int i = 9; i < parts.length; i += 2) {
			String name = parts[i];
			if (name.isEmpty())
				throw new IllegalArgumentException("Empty name is invalid.");
			String id = parts[i+1];
			c.add(new Owner(name, UUID.fromString(id)));
		}
		return c;
	}

	private static void writeCommentLine(PrintWriter p, String line) {
		p.append("// "+line+"\n");
	}

}
