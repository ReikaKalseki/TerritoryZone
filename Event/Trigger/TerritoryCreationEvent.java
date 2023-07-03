/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.TerritoryZone.Event.Trigger;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import Reika.TerritoryZone.Territory.TerritoryShape;

import cpw.mods.fml.common.eventhandler.Event;


public abstract class TerritoryCreationEvent extends Event {

	public final World world;
	public final String id;
	public final EntityPlayer player;

	public TerritoryCreationEvent(String id, World world, EntityPlayer ep) {
		this.id = id;
		this.world = world;
		player = ep;
	}

	public static class CreateTwoPoints extends TerritoryCreationEvent {

		public final int x1;
		public final int y1;
		public final int z1;
		public final int x2;
		public final int y2;
		public final int z2;

		public CreateTwoPoints(String id, World world, int x1, int y1, int z1, int x2, int y2, int z2, EntityPlayer ep) {
			super(id, world, ep);

			this.x1 = x1;
			this.y1 = y1;
			this.z1 = z1;
			this.x2 = x2;
			this.y2 = y2;
			this.z2 = z2;
		}

	}

	public static class CreateDirect extends TerritoryCreationEvent {

		public final int x;
		public final int y;
		public final int z;
		public final int radius;
		public final TerritoryShape shape;

		public CreateDirect(String id, World world, int x, int y, int z, int r, TerritoryShape s, EntityPlayer ep) {
			super(id, world, ep);

			this.x = x;
			this.y = y;
			this.z = z;

			radius = r;
			shape = s;
		}

	}

}
