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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import Reika.DragonAPI.Auxiliary.PacketTypes;
import Reika.DragonAPI.Interfaces.PacketHandler;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper.DataPacket;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper.PacketObj;
import Reika.DragonAPI.Libraries.IO.ReikaSoundHelper;
import Reika.RotaryCraft.Registry.SoundRegistry;
import Reika.TerritoryZone.TerritoryDispatcher.PacketInfo;

public class TerritoryPacketHandler implements PacketHandler {

	protected PacketInfo pack;

	private static final Random rand = new Random();


	public void handleData(PacketObj packet, World world, EntityPlayer ep) {
		DataInputStream inputStream = packet.getDataIn();
		int control = Integer.MIN_VALUE;
		int len;
		int[] data = new int[0];
		long longdata = 0;
		float floatdata = 0;
		int x = 0;
		int y = 0;
		int z = 0;
		double dx = 0;
		double dy = 0;
		double dz = 0;
		NBTTagCompound NBT = null;
		String stringdata = null;
		UUID id = null;
		//System.out.print(packet.length);
		try {
			//ReikaJavaLibrary.pConsole(inputStream.readInt()+":"+inputStream.readInt()+":"+inputStream.readInt()+":"+inputStream.readInt()+":"+inputStream.readInt()+":"+inputStream.readInt()+":"+inputStream.readInt());
			PacketTypes packetType = packet.getType();
			switch(packetType) {
				case FULLSOUND:
					break;
				case SOUND:
					control = inputStream.readInt();
					SoundRegistry s = SoundRegistry.soundList[control];
					double sx = inputStream.readDouble();
					double sy = inputStream.readDouble();
					double sz = inputStream.readDouble();
					float v = inputStream.readFloat();
					float p = inputStream.readFloat();
					boolean att = inputStream.readBoolean();
					ReikaSoundHelper.playClientSound(s, sx, sy, sz, v, p, att);
					return;
				case STRING:
					stringdata = packet.readString();
					control = inputStream.readInt();
					pack = PacketInfo.list[control];
					break;
				case DATA:
					control = inputStream.readInt();
					pack = PacketInfo.list[control];
					len = 1;
					data = new int[len];
					for (int i = 0; i < len; i++)
						data[i] = inputStream.readInt();
					break;
				case POS:
					control = inputStream.readInt();
					dx = inputStream.readDouble();
					dy = inputStream.readDouble();
					dz = inputStream.readDouble();
					len = 1;
					data = new int[len];
					for (int i = 0; i < len; i++)
						data[i] = inputStream.readInt();
					break;
				case UPDATE:
					control = inputStream.readInt();
					pack = PacketInfo.list[control];
					break;
				case FLOAT:
					control = inputStream.readInt();
					pack = PacketInfo.list[control];
					floatdata = inputStream.readFloat();
					break;
				case SYNC:
					String name = packet.readString();
					x = inputStream.readInt();
					y = inputStream.readInt();
					z = inputStream.readInt();
					ReikaPacketHelper.updateTileEntityData(world, x, y, z, name, inputStream);
					return;
				case TANK:
					String tank = packet.readString();
					x = inputStream.readInt();
					y = inputStream.readInt();
					z = inputStream.readInt();
					int level = inputStream.readInt();
					ReikaPacketHelper.updateTileEntityTankData(world, x, y, z, tank, level);
					return;
				case RAW:
					control = inputStream.readInt();
					pack = PacketInfo.list[control];
					len = 1;
					data = new int[len];
					for (int i = 0; i < len; i++)
						data[i] = inputStream.readInt();
					break;
				case PREFIXED:
					control = inputStream.readInt();
					pack = PacketInfo.list[control];
					len = inputStream.readInt();
					data = new int[len];
					for (int i = 0; i < len; i++)
						data[i] = inputStream.readInt();
					break;
				case NBT:
					control = inputStream.readInt();
					pack = PacketInfo.list[control];
					NBT = ((DataPacket)packet).asNBT();
					break;
				case STRINGINT:
					stringdata = packet.readString();
					control = inputStream.readInt();
					pack = PacketInfo.list[control];
					data = new int[1];
					for (int i = 0; i < data.length; i++)
						data[i] = inputStream.readInt();
					break;
				case UUID:
					control = inputStream.readInt();
					pack = PacketInfo.list[control];
					long l1 = inputStream.readLong(); //most
					long l2 = inputStream.readLong(); //least
					id = new UUID(l1, l2);
					break;
			}
			if (packetType.hasCoordinates()) {
				x = inputStream.readInt();
				y = inputStream.readInt();
				z = inputStream.readInt();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
		try {
			switch (pack) {
				case CLEAR:
					if (world.isRemote)
						TerritoryCache.instance.clear();
					break;
				case TERRITORY:
					if (world.isRemote) {
						Territory t = Territory.readFromNBT(NBT);
						TerritoryCache.instance.addTerritory(t);
					}
					break;
				case VERIFY:
					if (world.isRemote) {
						TerritoryCache.instance.verifyTerritories();
					}
					break;
			}
		}
		catch (NullPointerException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
