/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2015
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.TerritoryZone;

import java.net.URL;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.world.BlockEvent;
import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.DragonOptions;
import Reika.DragonAPI.Auxiliary.Trackers.CommandableUpdateChecker;
import Reika.DragonAPI.Auxiliary.Trackers.PlayerHandler;
import Reika.DragonAPI.Base.DragonAPIMod;
import Reika.DragonAPI.Base.DragonAPIMod.LoadProfiler.LoadPhase;
import Reika.DragonAPI.Instantiable.Event.PlayerPlaceBlockEvent;
import Reika.DragonAPI.Instantiable.IO.ControlledConfig;
import Reika.DragonAPI.Instantiable.IO.ModLogger;
import Reika.DragonAPI.Libraries.ReikaPlayerAPI;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;


@Mod( modid = "TerritoryZone", name="TerritoryZone", certificateFingerprint = "@GET_FINGERPRINT@", dependencies="required-after:DragonAPI")

public class TerritoryZone extends DragonAPIMod {

	public static final String packetChannel = "TerritoryData";

	static final Random rand = new Random();

	public static String currentVersion = "v@MAJOR_VERSION@@MINOR_VERSION@";

	@Instance("TerritoryZone")
	public static TerritoryZone instance = new TerritoryZone();

	public static final ControlledConfig config = new ControlledConfig(instance, TerritoryOptions.optionList, null, 0);

	public static ModLogger logger;

	//private String version;

	@SidedProxy(clientSide="Reika.TerritoryZone.TerritoryClient", serverSide="Reika.TerritoryZone.TerritoryCommon")
	public static TerritoryCommon proxy;


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

		this.basicSetup(evt);
		this.finishTiming();
	}

	@Override
	@EventHandler
	public void load(FMLInitializationEvent event) {
		this.startTiming(LoadPhase.LOAD);

		PlayerHandler.instance.registerTracker(TerritoryDispatcher.instance);

		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
			MinecraftForge.EVENT_BUS.register(TerritoryOverlay.instance);
		}

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

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void track(PlayerPlaceBlockEvent ev) {
		EntityPlayer ep = ev.player;
		if (ep == null) {
			TerritoryZone.logger.logError("Something tried a null-player interact event!");
			ReikaJavaLibrary.dumpStack();
			return;
		}
		else if (!TerritoryOptions.FAKEPLAYER.getState() && ReikaPlayerAPI.isFake(ep))
			return;
		World world = ep.worldObj;
		int x = ev.x;
		int y = ev.y;
		int z = ev.z;
		if (!world.isRemote) {
			for (Territory t : TerritoryLoader.instance.getTerritories()) {
				if (t.isInZone(world, x, y, z) && !t.ownedBy(ep)) {
					if (TerritoryOptions.enforceBlockPlace())
						ev.setCanceled(true);
					if (TerritoryOptions.logBlockPlace())
						logger.log("Player "+ep.getCommandSenderName()+" used GUI at "+x+", "+y+", "+z+" in "+t);
					break;
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void track(PlayerInteractEvent ev) {
		EntityPlayer ep = ev.entityPlayer;
		if (ep == null) {
			TerritoryZone.logger.logError("Something tried a null-player interact event!");
			ReikaJavaLibrary.dumpStack();
			return;
		}
		else if (!TerritoryOptions.FAKEPLAYER.getState() && ReikaPlayerAPI.isFake(ep))
			return;
		World world = ep.worldObj;
		int x = ev.x;
		int y = ev.y;
		int z = ev.z;
		if (!world.isRemote) {
			if (ev.action == Action.RIGHT_CLICK_BLOCK) {
				for (Territory t : TerritoryLoader.instance.getTerritories()) {
					if (t.isInZone(world, x, y, z) && !t.ownedBy(ep)) {
						if (TerritoryOptions.enforceGui())
							ev.setCanceled(true);
						if (TerritoryOptions.logGui())
							logger.log("Player "+ep.getCommandSenderName()+" used GUI at "+x+", "+y+", "+z+" in "+t);
						break;
					}
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void track(BlockEvent.BreakEvent ev) {
		EntityPlayer ep = ev.getPlayer();
		if (ep == null) {
			TerritoryZone.logger.logError("Something tried a null-player break block event!");
			ReikaJavaLibrary.dumpStack();
			return;
		}
		else if (!TerritoryOptions.FAKEPLAYER.getState() && ReikaPlayerAPI.isFake(ep))
			return;
		World world = ep.worldObj;
		int x = ev.x;
		int y = ev.y;
		int z = ev.z;
		if (!world.isRemote) {
			for (Territory t : TerritoryLoader.instance.getTerritories()) {
				if (t.isInZone(world, x, y, z) && !t.ownedBy(ep)) {
					if (TerritoryOptions.enforceBlockBreak())
						ev.setCanceled(true);
					if (TerritoryOptions.logBlockBreak()) {
						String b = Block.blockRegistry.getNameForObject(ev.block);
						logger.log("Player "+ep.getCommandSenderName()+" broke "+b+":"+ev.blockMetadata+" at "+x+", "+y+", "+z+" in "+t);
					}
					break;
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void track(LivingHurtEvent ev) {
		if (!(ev.entityLiving instanceof EntityMob) && ev.source.getEntity() instanceof EntityPlayer) {
			EntityPlayer ep = (EntityPlayer)ev.source.getEntity();
			if (!TerritoryOptions.FAKEPLAYER.getState() && ReikaPlayerAPI.isFake(ep))
				return;
			World world = ep.worldObj;
			if (!world.isRemote) {
				for (Territory t : TerritoryLoader.instance.getTerritories()) {
					if (t.isInZone(world, ep) && !t.ownedBy(ep)) {
						if (TerritoryOptions.enforceAnimalKill())
							ev.setCanceled(true);
						if (TerritoryOptions.logAnimalKill()) {
							logger.log("Player "+ep.getCommandSenderName()+" attacked "+ev.entityLiving+" in "+t);
						}
						break;
					}
				}
			}
		}
	}
}
