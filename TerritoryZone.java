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
import java.net.URL;
import java.util.Map;
import java.util.Random;

import net.minecraftforge.common.MinecraftForge;
import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.DragonOptions;
import Reika.DragonAPI.Auxiliary.Trackers.CommandableUpdateChecker;
import Reika.DragonAPI.Auxiliary.Trackers.PlayerHandler;
import Reika.DragonAPI.Base.DragonAPIMod;
import Reika.DragonAPI.Base.DragonAPIMod.LoadProfiler.LoadPhase;
import Reika.DragonAPI.Instantiable.IO.ControlledConfig;
import Reika.DragonAPI.Instantiable.IO.ModLogger;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper;
import Reika.TerritoryZone.Event.TerritoryLoggingEvent;
import Reika.TerritoryZone.Event.TerritoryReloadedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;


@Mod( modid = "TerritoryZone", name="TerritoryZone", version = "v@MAJOR_VERSION@@MINOR_VERSION@", certificateFingerprint = "@GET_FINGERPRINT@", dependencies="required-after:DragonAPI", acceptableRemoteVersions="*")

public class TerritoryZone extends DragonAPIMod {

	public static final String packetChannel = "TerritoryData";

	static final Random rand = new Random();

	public static final String currentVersion = "v@MAJOR_VERSION@@MINOR_VERSION@";

	@Instance("TerritoryZone")
	public static TerritoryZone instance = new TerritoryZone();

	public static final ControlledConfig config = new ControlledConfig(instance, TerritoryOptions.optionList, null);

	public static ModLogger logger;

	//private String version;

	@SidedProxy(clientSide="Reika.TerritoryZone.TerritoryClient", serverSide="Reika.TerritoryZone.TerritoryCommon")
	public static TerritoryCommon proxy;

	@NetworkCheckHandler
	public boolean checkModList(Map<String, String> versions, Side side) {
		if (side == Side.CLIENT) {
			String v = versions.get("TerritoryZone");
			if (v != null) {
				return v.equals(currentVersion);
			}
		}
		return true;
	}

	@Override
	@EventHandler
	public void preload(FMLPreInitializationEvent evt) {
		this.startTiming(LoadPhase.PRELOAD);
		this.verifyInstallation();

		config.loadSubfolderedConfigFile(evt);
		config.initProps(evt);

		logger = new ModLogger(instance, false);
		if (DragonOptions.FILELOG.getState())
			logger.setOutput("**_Loading_Log.log");

		ReikaPacketHelper.registerPacketHandler(instance, packetChannel, new TerritoryPacketHandler());
		FMLCommonHandler.instance().bus().register(this);

		this.basicSetup(evt);
		this.finishTiming();
	}

	@Override
	@EventHandler
	public void load(FMLInitializationEvent event) {
		this.startTiming(LoadPhase.LOAD);

		PlayerHandler.instance.registerTracker(TerritoryDispatcher.instance);

		MinecraftForge.EVENT_BUS.register(TerritoryEventHandler.instance);
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
			MinecraftForge.EVENT_BUS.register(TerritoryOverlay.instance);
		}

		FileLogger.instance.init();

		this.finishTiming();
	}

	@Override
	@EventHandler
	public void postload(FMLPostInitializationEvent evt) {
		this.startTiming(LoadPhase.POSTLOAD);

		TerritoryLoader.instance.load();

		this.finishTiming();
	}

	@EventHandler
	public void registerCommands(FMLServerStartingEvent evt) {
		evt.registerServerCommand(new ReloadTerritoriesCommand());
		if (TerritoryOptions.registerTeleportCommand())
			evt.registerServerCommand(new TerritoryTeleportCommand());
	}

	@Override
	public String getDisplayName() {
		return "TerritoryZone";
	}

	@Override
	public String getModAuthorName() {
		return "Reika";
	}

	@Override
	public URL getDocumentationSite() {
		return DragonAPICore.getReikaForumPage();
	}

	@Override
	public String getWiki() {
		return null;
	}

	@Override
	public ModLogger getModLogger() {
		return logger;
	}

	@Override
	public String getUpdateCheckURL() {
		return CommandableUpdateChecker.reikaURL;
	}

	@Override
	public File getConfigFolder() {
		return config.getConfigFolder();
	}

	public static void log(Territory t, String s) {
		MinecraftForge.EVENT_BUS.post(new TerritoryLoggingEvent(t, s));
		if (TerritoryOptions.FILELOG.getState()) {
			FileLogger.instance.log(s);
		}
		else {
			logger.log(s);
		}
	}

	public static void reloadTerritories() {
		TerritoryLoader.instance.load();
		TerritoryDispatcher.instance.sendTerritoriesToAll();
		MinecraftForge.EVENT_BUS.post(new TerritoryReloadedEvent());
		logger.log("Territories Reloaded. "+TerritoryLoader.instance.getTerritories().size()+" Territories:");
		for (Territory t : TerritoryLoader.instance.getTerritories()) {
			logger.log(t.toString());
		}
	}
}
