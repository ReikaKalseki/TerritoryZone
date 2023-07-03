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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.codec.Charsets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

import Reika.DragonAPI.Exception.InstallationException;
import Reika.DragonAPI.Exception.RegistrationException;
import Reika.DragonAPI.Exception.UserErrorException;
import Reika.DragonAPI.IO.ReikaFileReader;
import Reika.DragonAPI.Instantiable.Data.Immutable.CommutativePair;
import Reika.DragonAPI.Instantiable.Data.Immutable.WorldLocation;
import Reika.DragonAPI.Instantiable.Data.Maps.MultiMap;
import Reika.DragonAPI.Instantiable.IO.LuaBlock;
import Reika.DragonAPI.Instantiable.IO.LuaBlock.LuaBlockDatabase;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.TerritoryZone.Territory.Protections;
import Reika.TerritoryZone.Territory.TerritoryLuaBlock;
import Reika.TerritoryZone.Territory.TerritoryShape;
import Reika.TerritoryZone.Event.TerritoryCollisionEvent;
import Reika.TerritoryZone.Event.TerritoryRegisterEvent;
import Reika.TerritoryZone.Event.TerritoryUnregisterEvent;

public class TerritoryLoader {

	public static final TerritoryLoader instance = new TerritoryLoader();

	private final MultiMap<UUID, Territory> ownerMap = new MultiMap();
	private final Collection<Territory> territories = new ArrayList();

	private final LuaBlockDatabase territoryData = new LuaBlockDatabase();

	private final Territory exampleTerritory = new Territory("example", new WorldLocation(0, 1023, 90, -1304), 128, 0xffffff, TerritoryShape.CUBE).addOwner(UUID.fromString("504e35e4-ee36-45e0-b1d3-7ad419311644"), "SomePlayer").addOwner(UUID.fromString("759fc6d2-1868-4c90-908c-81bf9b3cd973"), "User2");

	private TerritoryLoader() {
		territoryData.defaultBlockType = TerritoryLuaBlock.class;
	}

	public void addTerritory(Territory t) {
		for (UUID id : t.getOwnerIDs())
			ownerMap.addValue(id, t);
		territories.add(t);
		MinecraftForge.EVENT_BUS.post(new TerritoryRegisterEvent(t));
		if (MinecraftServer.getServer() != null && MinecraftServer.getServer().isServerRunning())
			TerritoryDispatcher.instance.sendTerritoriesToAll();
		this.writeZoneFile(this.getFullSavePath(), false);
	}

	public void removeTerritory(Territory t) {
		for (UUID id : t.getOwnerIDs())
			ownerMap.remove(id, t);
		territories.remove(t);
		MinecraftForge.EVENT_BUS.post(new TerritoryUnregisterEvent(t));
		TerritoryDispatcher.instance.sendTerritoriesToAll();
		this.writeZoneFile(this.getFullSavePath(), false);
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
		return "TerritoryZone_zones.lua";
	}

	public final File getFullSavePath() {
		return new File(TerritoryZone.config.getConfigFolder(), this.getSaveFileName());
	}

	public void load() {
		territoryData.clear();
		TerritoryZone.logger.log("Loading zone config.");
		File f = this.getFullSavePath();
		if (f.exists()) {
			try {
				territoryData.loadFromFile(f);
				LuaBlock root = territoryData.getRootBlock();
				for (LuaBlock b : root.getChildren()) {
					try {
						String type = b.getString("type");
						if (type.equalsIgnoreCase("example"))
							continue;
						TerritoryZone.logger.log("Parsing zone entry '"+type+"'");
						territoryData.addBlock(type, b);
						territories.add(this.parseTerritoryEntry(type, b));
					}
					catch (Exception e) {
						TerritoryZone.logger.logError("Could not parse config section "+b.getString("type")+": ");
						ReikaJavaLibrary.pConsole(b);
						ReikaJavaLibrary.pConsole("----------------------Cause------------------------");
						e.printStackTrace();
					}
				}
			}
			catch (Exception e) {
				if (e instanceof UserErrorException)
					throw new InstallationException(TerritoryZone.instance, "Configs could not be loaded! Correct them and try again.", e);
				else
					throw new RegistrationException(TerritoryZone.instance, "Configs could not be loaded! Correct them and try again.", e);
			}

			TerritoryZone.logger.log("Configs loaded.");
		}
		else {
			this.writeZoneFile(f, true);
		}
	}

	private Territory parseTerritoryEntry(String type, LuaBlock b) throws NumberFormatException, IllegalArgumentException, IllegalStateException {
		int r = b.getInt("radius");
		int color = b.getInt("color");
		TerritoryShape shape = TerritoryShape.valueOf(b.getString("shape").toUpperCase(Locale.ENGLISH));
		LuaBlock pos = b.getChild("position");
		if (pos == null || pos.isEmpty())
			throw new IllegalArgumentException("No position specified!");
		WorldLocation loc = new WorldLocation(b.getInt("dimension"), pos.getInt("x"), pos.getInt("y"), pos.getInt("z"));
		Territory t = new Territory(type, loc, r, color, shape);

		LuaBlock owners = b.getChild("owners");
		if (owners == null)
			throw new IllegalArgumentException("No owner definitions!");
		if (owners.isEmpty())
			throw new IllegalArgumentException("No owners specified!");
		for (LuaBlock item : owners.getChildren()) {
			if (!item.containsKey("uuid"))
				throw new IllegalArgumentException("Missing uuid: "+item);
			if (!item.containsKey("username"))
				throw new IllegalArgumentException("Missing username: "+item);
			t.addOwner(UUID.fromString(item.getString("uuid")), item.getString("username"));
		}

		LuaBlock settings = b.getChild("settings");
		if (settings == null)
			throw new IllegalArgumentException("No protection type definitions!");
		if (settings.isEmpty())
			throw new IllegalArgumentException("No protection types specified!");
		for (LuaBlock item : settings.getChildren()) {
			Protections p = Protections.valueOf(item.name.toUpperCase(Locale.ENGLISH));
			t.setProtectionLevel(p, item.getBoolean("log"), item.getBoolean("chat"), item.getBoolean("protect"));
		}

		return t;
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

	private boolean writeZoneFile(File f, boolean isExample) {
		ArrayList<String> p = new ArrayList();
		this.writeCommentLine(p, "-------------------------------");
		this.writeCommentLine(p, " TerritoryZone Territory Loader ");
		this.writeCommentLine(p, "-------------------------------");
		this.writeCommentLine(p, "");
		this.writeCommentLine(p, "Consult the example entry below, or the TerritoryZone page on the site for detailed documentation of the format.");
		this.writeCommentLine(p, "");
		this.writeCommentLine(p, "Use this file to specify the territory zones.");
		this.writeCommentLine(p, "Zone Shapes:");
		for (int i = 0; i < TerritoryShape.list.length; i++) {
			TerritoryShape pr = TerritoryShape.list[i];
			this.writeCommentLine(p, pr.name()+" - "+pr.desc);
		}
		this.writeCommentLine(p, "");
		this.writeCommentLine(p, "Colors must be hex codes.");
		this.writeCommentLine(p, "");
		this.writeCommentLine(p, "Protection Types:");
		for (int i = 0; i < Protections.list.length; i++) {
			Protections pr = Protections.list[i];
			this.writeCommentLine(p, pr.name()+" - "+pr.desc);
		}
		this.writeCommentLine(p, "");
		this.writeCommentLine(p, "Entries missing any element, or with less than one owner, are incorrect.");
		this.writeCommentLine(p, "Incorrectly formatted entries will be ignored and will log an error in the console.");
		this.writeCommentLine(p, "Lines beginning with '--' are comments and will be ignored, as will empty lines.");
		this.writeCommentLine(p, "====================================================================================");
		p.addAll(this.getExampleTerritory().writeToStrings());
		for (Territory t : territories)
			p.addAll(t.toLuaBlock(territoryData).writeToStrings());
		return ReikaFileReader.writeLinesToFile(f, p, true, Charsets.UTF_8);
	}

	private LuaBlock getExampleTerritory() {
		LuaBlock bk = exampleTerritory.toLuaBlock(territoryData);
		LuaBlock settings = bk.getChild("settings");
		LuaBlock prot = settings.getChild(Protections.list[0].name());
		prot.setComment("log", "Whether to log non-owner actions of this type. This is the most basic kind of protection/monitoring.");
		prot.setComment("chat", "Whether to also send chat messages to owners when the above logging occurs. Generally used for more urgent matters.");
		prot.setComment("protect", "Whether to outright prevent non-owners from doing this inside the territory. The most invasive option.");
		return bk;
	}

	private static void writeCommentLine(ArrayList<String> li, String line) {
		li.add("-- "+line);
	}

}
