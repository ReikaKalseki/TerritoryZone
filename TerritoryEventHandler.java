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

import net.minecraft.block.Block;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.world.BlockEvent;
import Reika.DragonAPI.Instantiable.Data.Immutable.WorldLocation;
import Reika.DragonAPI.Instantiable.Event.FireSpreadEvent;
import Reika.DragonAPI.Instantiable.Event.PlayerPlaceBlockEvent;
import Reika.DragonAPI.Libraries.ReikaPlayerAPI;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.TerritoryZone.Territory.Owner;
import Reika.TerritoryZone.Territory.Protections;
import Reika.TerritoryZone.Event.TerritoryEnforceEvent;
import Reika.TerritoryZone.Event.Trigger.TerritoryCreationEvent;
import Reika.TerritoryZone.Event.Trigger.TerritoryReloadEvent;
import Reika.TerritoryZone.Event.Trigger.TerritoryRemoveEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;


public class TerritoryEventHandler {

	public static final TerritoryEventHandler instance = new TerritoryEventHandler();

	private TerritoryEventHandler() {

	}

	@SubscribeEvent
	public void triggerReload(TerritoryReloadEvent evt) {
		TerritoryZone.reloadTerritories();
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
						TerritoryZone.log(t, "Player "+ep.getCommandSenderName()+" placed a block "+ev.block.getLocalizedName()+" at "+x+", "+y+", "+z+" in "+t);
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
							TerritoryZone.log(t, "Player "+ep.getCommandSenderName()+" used GUI at "+x+", "+y+", "+z+" in "+t);
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
						TerritoryZone.log(t, "Player "+ep.getCommandSenderName()+" broke "+b+":"+ev.blockMetadata+" at "+x+", "+y+", "+z+" in "+t);
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
							TerritoryZone.log(t, "Player "+ep.getCommandSenderName()+" attacked "+ev.entityLiving+" in "+t);
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
						TerritoryZone.log(t, "Player "+ep.getCommandSenderName()+" tried picking up "+ev.pickedUp+" in "+t);
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
							TerritoryZone.log(t, "Player "+ep.getCommandSenderName()+" tried attacking "+ev.entityLiving.getCommandSenderName()+" in "+t);
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

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void trackFire(FireSpreadEvent ev) {
		World world = ev.world;
		if (!world.isRemote) {
			for (Territory t : TerritoryLoader.instance.getTerritories()) {
				if (t.isInZone(world, ev.xCoord, ev.yCoord, ev.zCoord)) {
					if (t.enforce(Protections.FIRESPREAD) && !MinecraftForge.EVENT_BUS.post(new TerritoryEnforceEvent(t, Protections.FIRESPREAD)))
						ev.setCanceled(true);
					if (t.log(Protections.FIRESPREAD)) {
						TerritoryZone.log(t, "Fire spread @ "+ev.xCoord+", "+ev.yCoord+", "+ev.zCoord);
						if (t.chatMessages) {
							t.sendChatToOwner(Protections.FIRESPREAD, null, ev.xCoord, ev.yCoord, ev.zCoord);
						}
					}
					break;
				}
			}
		}
	}

}
