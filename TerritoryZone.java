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
import Reika.DragonAPI.Instantiable.Data.Immutable.WorldLocation;
import Reika.DragonAPI.Instantiable.Event.PlayerPlaceBlockEvent;
import Reika.DragonAPI.Instantiable.IO.ControlledConfig;
import Reika.DragonAPI.Instantiable.IO.ModLogger;
import Reika.DragonAPI.Libraries.ReikaPlayerAPI;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.TerritoryZone.Territory.Owner;
import Reika.TerritoryZone.Territory.Protections;
import Reika.TerritoryZone.Event.TerritoryEnforceEvent;
import Reika.TerritoryZone.Event.TerritoryLoggingEvent;
import Reika.TerritoryZone.Event.TerritoryReloadedEvent;
import Reika.TerritoryZone.Event.Trigger.TerritoryCreationEvent;
import Reika.TerritoryZone.Event.Trigger.TerritoryReloadEvent;
import Reika.TerritoryZone.Event.Trigger.TerritoryRemoveEvent;
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
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import cpw.mods.fml.relauncher.Side;


@Mod( modid = "TerritoryZone", name="TerritoryZone", certificateFingerprint = "@GET_FINGERPRINT@", dependencies="required-after:DragonAPI")

public class TerritoryZone extends DragonAPIMod {

	public static final String packetChannel = "TerritoryData";

	static final Random rand = new Random();

	public static String currentVersion = "v@MAJOR_VERSION@@MINOR_VERSION@";

	@Instance("TerritoryZone")
	public static TerritoryZone instance = new TerritoryZone();

	public static final ControlledConfig config = new ControlledConfig(instance, TerritoryOptions.optionList, null);

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
		FMLCommonHandler.instance().bus().register(this);

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

	@SubscribeEvent
	public void triggerReload(TerritoryReloadEvent evt) {
		reloadTerritories();
	}

	@SubscribeEvent
	public void triggerCreate(TerritoryCreationEvent.CreateTwoPoints evt) {
		Territory t = Territory.getFromTwoPoints(evt.world, evt.x1, evt.y1, evt.z1, evt.x2, evt.y2, evt.z2, evt.player, evt.enforcement, evt.logging);
		TerritoryLoader.instance.addTerritory(t);
	}

	@SubscribeEvent
	public void triggerCreate(TerritoryCreationEvent.CreateDirect evt) {
		Territory t = new Territory(new WorldLocation(evt.world, evt.x, evt.y, evt.z), evt.radius, 0xff0000, evt.enforcement, evt.logging, evt.shape, ReikaJavaLibrary.makeListFrom(new Owner(evt.player)));
		TerritoryLoader.instance.addTerritory(t);
	}

	@SubscribeEvent
	public void triggerRemoval(TerritoryRemoveEvent evt) {
		TerritoryLoader.instance.removeTerritory(evt.territory);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void trackPlace(PlayerPlaceBlockEvent ev) {
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
					if (t.enforce(Protections.PLACE) && !MinecraftForge.EVENT_BUS.post(new TerritoryEnforceEvent(t, Protections.PLACE)))
						ev.setCanceled(true);
					if (t.log(Protections.PLACE)) {
						log(t, "Player "+ep.getCommandSenderName()+" placed a block "+ev.block.getLocalizedName()+" at "+x+", "+y+", "+z+" in "+t);
						if (t.chatMessages) {
							t.sendChatToOwner(Protections.PLACE, ep, ev.block, x, y, z);
						}
					}
					break;
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void trackGUIs(PlayerInteractEvent ev) {
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
						if (t.enforce(Protections.GUI) && !MinecraftForge.EVENT_BUS.post(new TerritoryEnforceEvent(t, Protections.GUI)))
							ev.setCanceled(true);
						if (t.log(Protections.GUI))
							log(t, "Player "+ep.getCommandSenderName()+" used GUI at "+x+", "+y+", "+z+" in "+t);
						if (t.chatMessages) {
							t.sendChatToOwner(Protections.GUI, ep, x, y, z);
						}
						break;
					}
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void trackBreak(BlockEvent.BreakEvent ev) {
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
					if (t.enforce(Protections.BREAK) && !MinecraftForge.EVENT_BUS.post(new TerritoryEnforceEvent(t, Protections.BREAK)))
						ev.setCanceled(true);
					if (t.log(Protections.BREAK)) {
						String b = Block.blockRegistry.getNameForObject(ev.block);
						log(t, "Player "+ep.getCommandSenderName()+" broke "+b+":"+ev.blockMetadata+" at "+x+", "+y+", "+z+" in "+t);
						if (t.chatMessages) {
							t.sendChatToOwner(Protections.BREAK, ep, ev.block, x, y, z);
						}
					}
					break;
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void trackAnimals(LivingHurtEvent ev) {
		if (!(ev.entityLiving instanceof EntityMob) && ev.source.getEntity() instanceof EntityPlayer) {
			EntityPlayer ep = (EntityPlayer)ev.source.getEntity();
			if (!TerritoryOptions.FAKEPLAYER.getState() && ReikaPlayerAPI.isFake(ep))
				return;
			World world = ep.worldObj;
			if (!world.isRemote) {
				for (Territory t : TerritoryLoader.instance.getTerritories()) {
					if (t.isInZone(world, ep) && !t.ownedBy(ep)) {
						if (t.enforce(Protections.ANIMALS) && !MinecraftForge.EVENT_BUS.post(new TerritoryEnforceEvent(t, Protections.ANIMALS)))
							ev.setCanceled(true);
						if (t.log(Protections.ANIMALS)) {
							log(t, "Player "+ep.getCommandSenderName()+" attacked "+ev.entityLiving+" in "+t);
							if (t.chatMessages) {
								t.sendChatToOwner(Protections.ANIMALS, ep, ev.entityLiving);
							}
						}
						break;
					}
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void trackItems(ItemPickupEvent ev) {
		EntityPlayer ep = ev.player;
		if (!TerritoryOptions.FAKEPLAYER.getState() && ReikaPlayerAPI.isFake(ep))
			return;
		World world = ep.worldObj;
		if (!world.isRemote) {
			for (Territory t : TerritoryLoader.instance.getTerritories()) {
				if (t.isInZone(world, ep) && !t.ownedBy(ep)) {
					if (t.enforce(Protections.ITEMS) && !MinecraftForge.EVENT_BUS.post(new TerritoryEnforceEvent(t, Protections.ITEMS)))
						ev.setCanceled(true);
					if (t.log(Protections.ITEMS)) {
						log(t, "Player "+ep.getCommandSenderName()+" tried picking up "+ev.pickedUp+" in "+t);
						if (t.chatMessages) {
							t.sendChatToOwner(Protections.ITEMS, ep, ev.pickedUp.getEntityItem(), ev.pickedUp.posX, ev.pickedUp.posY, ev.pickedUp.posZ);
						}
					}
					break;
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void trackPVP(LivingHurtEvent ev) {
		if (ev.entityLiving instanceof EntityPlayer && ev.source.getEntity() instanceof EntityPlayer) {
			EntityPlayer ep = (EntityPlayer)ev.source.getEntity();
			if (!TerritoryOptions.FAKEPLAYER.getState() && ReikaPlayerAPI.isFake(ep))
				return;
			World world = ep.worldObj;
			if (!world.isRemote) {
				for (Territory t : TerritoryLoader.instance.getTerritories()) {
					if (t.isInZone(world, ep) && !t.ownedBy(ep)) {
						if (t.enforce(Protections.PVP) && !MinecraftForge.EVENT_BUS.post(new TerritoryEnforceEvent(t, Protections.PVP)))
							ev.setCanceled(true);
						if (t.log(Protections.PVP)) {
							log(t, "Player "+ep.getCommandSenderName()+" tried attacking "+ev.entityLiving.getCommandSenderName()+" in "+t);
							if (t.chatMessages) {
								t.sendChatToOwner(Protections.PVP, ep, ev.entityLiving);
							}
						}
						break;
					}
				}
			}
		}
	}
}
