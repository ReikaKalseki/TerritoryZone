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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import Reika.DragonAPI.Auxiliary.Trackers.PlayerHandler.PlayerTracker;
import Reika.DragonAPI.Instantiable.IO.PacketTarget;
import Reika.DragonAPI.Instantiable.IO.PacketTarget.PlayerTarget;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper;

public class TerritoryDispatcher implements PlayerTracker {

	public static final TerritoryDispatcher instance = new TerritoryDispatcher();

	private TerritoryDispatcher() {

	}

	public static enum PacketInfo {
		TERRITORY(),
		CLEAR(),
		VERIFY();

		public static final PacketInfo[] list = values();
	}

	public void sendTerritoriesToAll() {
		ReikaPacketHelper.sendDataPacketToEntireServer(TerritoryZone.packetChannel, PacketInfo.CLEAR.ordinal(), 0);
		for (Territory t : TerritoryLoader.instance.getTerritories()) {
			this.sendTerritory(t, null);
		}
		ReikaPacketHelper.sendDataPacketToEntireServer(TerritoryZone.packetChannel, PacketInfo.VERIFY.ordinal(), 0);
	}

	public void sendTerritoriesToPlayer(EntityPlayerMP ep) {
		ReikaPacketHelper.sendDataPacket(TerritoryZone.packetChannel, PacketInfo.CLEAR.ordinal(), ep, 0);
		for (Territory t : TerritoryLoader.instance.getTerritories()) {
			this.sendTerritory(t, ep);
		}
		ReikaPacketHelper.sendDataPacket(TerritoryZone.packetChannel, PacketInfo.VERIFY.ordinal(), ep, 0);
	}

	private void sendTerritory(Territory t, EntityPlayerMP ep) {
		if (ep == null) {
			ReikaPacketHelper.sendNBTPacket(TerritoryZone.packetChannel, PacketInfo.TERRITORY.ordinal(), this.getTerritoryData(t), new PacketTarget.AllPlayersTarget());
		}
		else {
			ReikaPacketHelper.sendNBTPacket(TerritoryZone.packetChannel, PacketInfo.TERRITORY.ordinal(), this.getTerritoryData(t), new PlayerTarget(ep));
		}
	}

	private NBTTagCompound getTerritoryData(Territory t) {
		NBTTagCompound nbt = new NBTTagCompound();
		t.writeToNBT(nbt);
		return nbt;
	}

	@Override
	public void onPlayerLogin(EntityPlayer ep) {
		if (ep instanceof EntityPlayerMP)
			this.sendTerritoriesToPlayer((EntityPlayerMP)ep);
	}

	@Override
	public void onPlayerLogout(EntityPlayer player) {

	}

	@Override
	public void onPlayerChangedDimension(EntityPlayer player, int dimFrom, int dimTo) {

	}

	@Override
	public void onPlayerRespawn(EntityPlayer player) {

	}

}
