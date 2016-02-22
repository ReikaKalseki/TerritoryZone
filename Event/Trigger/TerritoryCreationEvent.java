/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2015
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
	public final EntityPlayer player;
	public final int enforcement;
	public final int logging;

	public TerritoryCreationEvent(World world, EntityPlayer ep, int enforce, int log) {
		this.world = world;
		player = ep;
		enforcement = enforce;
		logging = log;
	}

	public static class CreateTwoPoints extends TerritoryCreationEvent {

		public final int x1;
		public final int y1;
		public final int z1;
		public final int x2;
		public final int y2;
		public final int z2;

		public CreateTwoPoints(World world, int x1, int y1, int z1, int x2, int y2, int z2, int enforce, int log, EntityPlayer ep) {
			super(world, ep, enforce, log);

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

		public CreateDirect(World world, int x, int y, int z, int r, TerritoryShape s, int enforce, int log, EntityPlayer ep) {
			super(world, ep, enforce, log);

			this.x = x;
			this.y = y;
			this.z = z;

			radius = r;
			shape = s;
		}

	}

}
