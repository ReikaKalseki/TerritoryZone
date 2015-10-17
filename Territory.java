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

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import Reika.DragonAPI.Instantiable.Data.Immutable.WorldLocation;
import Reika.DragonAPI.Libraries.ReikaNBTHelper.NBTTypes;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.MathSci.ReikaVectorHelper;

public final class Territory {

	public final WorldLocation origin;
	public final int radius;
	public final int color;
	private final Collection<Owner> owners = new HashSet();
	public final TerritoryShape shape;
	public final int enforcementLevel;
	public final int loggingLevel;

	//public Territory(int dim, int x, int y, int z, int r, String name, UUID uid, TerritoryShape sh) {
	//	this(new WorldLocation(dim, x, y, z), r, name, uid, sh);
	//}

	public Territory(WorldLocation loc, int r, int clr, int enf, int log, TerritoryShape sh) {
		this(loc, r, clr, enf, log, sh, null);
	}

	public Territory(WorldLocation loc, int r, int clr, int enf, int log, TerritoryShape sh, Collection<Owner> c) {
		origin = loc;
		radius = r;
		shape = sh;
		color = clr;
		enforcementLevel = enf;
		loggingLevel = log;
		if (c != null)
			owners.addAll(c);
	}

	public boolean intersects(Territory t) {
		return origin.dimensionID == t.origin.dimensionID && this.intersectsComplex(t);
	}

	private boolean intersectsComplex(Territory t) {
		WorldLocation loc1 = origin;
		WorldLocation loc2 = t.origin;
		Vec3 vec = ReikaVectorHelper.getVec2Pt(loc1.xCoord, loc1.yCoord, loc1.zCoord, loc2.xCoord, loc2.yCoord, loc2.zCoord);
		double r1 = this.getEdgeDistanceAlong(vec);
		double r2 = t.getEdgeDistanceAlong(ReikaVectorHelper.getInverseVector(vec));
		return r1+r2 > vec.lengthVector();
	}

	private double getEdgeDistanceAlong(Vec3 vec) {
		switch(shape) {
			case CUBE:
				return 0;
			case CYLINDER: {
				double pro = Math.min(radius, ReikaVectorHelper.getXZProjection(vec).lengthVector());
				return Math.sqrt(pro*pro+vec.yCoord*vec.yCoord);
			}
			case PRISM: {
				double ang = Math.atan2(vec.xCoord, vec.zCoord);
				double pro = Math.min(1/Math.sin(ang), 1/Math.cos(ang));
				return Math.sqrt(pro*pro+vec.yCoord*vec.yCoord);
			}
			case SPHERE:
				return Math.min(radius, vec.lengthVector());
			default:
				return 0;
		}
	}

	private boolean intersectsSimple(Territory t) {
		return this.getAABB().intersectsWith(t.getAABB());
	}

	private AxisAlignedBB getAABB() {
		int ox = origin.xCoord;
		int oy = origin.yCoord;
		int oz = origin.zCoord;
		int minx = ox-radius;
		int miny = Math.max(0, oy-radius);
		int minz = oz-radius;
		int maxx = ox+radius+1;
		int maxy = Math.min(256, oy+radius+1);
		int maxz = oz+radius+1;
		return AxisAlignedBB.getBoundingBox(minx, miny, minz, maxx, maxy, maxz);
	}

	public boolean isInZone(World world, Entity e) {
		return this.isInZone(world, e.posX, e.posY, e.posZ);
	}

	public boolean isInZone(World world, double x, double y, double z) {
		return world.provider.dimensionId == origin.dimensionID && shape.isInZone(origin.xCoord, origin.yCoord, origin.zCoord, x, y, z, radius);
	}

	public boolean ownedBy(EntityPlayer ep) {
		return owners.contains(new Owner(ep));
	}

	@Override
	public String toString() {
		return radius+"-"+shape.name()+" @ "+origin.toString()+" {"+enforcementLevel+"/"+loggingLevel+"} "+" by "+owners.toString();
	}

	public String getBoundsDesc() {
		return shape.getBounds(origin, radius);
	}

	public String getOwnerNames() {
		StringBuilder sb = new StringBuilder();
		for (Owner o : owners) {
			sb.append(o.name);
			sb.append(", ");
		}
		return sb.length() > 2 ? sb.substring(0, sb.length()-2) : sb.toString();
	}

	public void writeToNBT(NBTTagCompound nbt) {
		origin.writeToNBT("loc", nbt);
		nbt.setInteger("r", radius);
		nbt.setInteger("color", color);
		nbt.setInteger("shape", shape.ordinal());
		nbt.setInteger("enforce", enforcementLevel);
		nbt.setInteger("logging", loggingLevel);
		NBTTagList li = new NBTTagList();
		for (Owner o : owners) {
			NBTTagCompound tag = new NBTTagCompound();
			o.writeToNBT(tag);
			li.appendTag(tag);
		}
		nbt.setTag("owners", li);
	}

	public static Territory readFromNBT(NBTTagCompound nbt) {
		WorldLocation loc = WorldLocation.readFromNBT("loc", nbt);
		int radius = nbt.getInteger("r");
		int clr = nbt.getInteger("color");
		int enf = nbt.getInteger("enforce");
		int log = nbt.getInteger("logging");
		TerritoryShape sh = TerritoryShape.list[nbt.getInteger("shape")];
		NBTTagList li = nbt.getTagList("owners", NBTTypes.COMPOUND.ID);
		Territory t = new Territory(loc, radius, clr, enf, log, sh);
		for (Object o : li.tagList) {
			NBTTagCompound dat = (NBTTagCompound)o;
			Owner own = Owner.readFromNBT(dat);
			t.owners.add(own);
		}
		return t;
	}

	public static enum TerritoryShape {
		CUBE("Cubical Zone"),
		PRISM("Full-height square perimeter"),
		SPHERE("Spherical Zone"),
		CYLINDER("Full-height circular perimeter");

		public final String desc;

		public static final TerritoryShape[] list = values();

		private TerritoryShape(String s) {
			desc = s;
		}

		private boolean isInZone(double xo, double yo, double zo, double x, double y, double z, int r) {
			switch(this) {
				case CUBE:
					return ReikaMathLibrary.isValueInsideBoundsIncl(xo-r, xo+r+1, x) && ReikaMathLibrary.isValueInsideBoundsIncl(yo-r, yo+r+1, y) && ReikaMathLibrary.isValueInsideBoundsIncl(zo-r, zo+r+1, z);
				case CYLINDER:
					return ReikaMathLibrary.py3d(x-xo, 0, z-zo) <= r+1;
				case PRISM:
					return ReikaMathLibrary.isValueInsideBoundsIncl(xo-r, xo+r+1, x) && ReikaMathLibrary.isValueInsideBoundsIncl(zo-r, zo+r+1, z);
				case SPHERE:
					return ReikaMathLibrary.py3d(x-xo, y-yo, z-zo) <= r+1;
				default:
					return false;
			}
		}

		private String getBounds(WorldLocation loc, int radius) {
			int ox = loc.xCoord;
			int oy = loc.yCoord;
			int oz = loc.zCoord;
			switch(this) {
				case CUBE: {
					int minx = ox-radius;
					int miny = oy-radius;
					int minz = oz-radius;
					int maxx = ox+radius+1;
					int maxy = oy+radius+1;
					int maxz = oz+radius+1;
					return String.format("Zone is %d, %d, %d to %d, %d, %d", minx, miny, minz, maxx, maxy, maxz);
				}
				case CYLINDER:
					return String.format("Zone is a radius %d cylinder around %d, %d", radius, ox, oz);
				case PRISM: {
					int minx = ox-radius;
					int minz = oz-radius;
					int maxx = ox+radius+1;
					int maxz = oz+radius+1;
					return String.format("Zone is %d, %d to %d, %d", minx, minz, maxx, maxz);
				}
				case SPHERE:
					return String.format("Zone is a radius %d sphere around %d, %d, %d", radius, ox, oy, oz);
				default:
					return "[ERROR]";
			}
		}
	}

	public static final class Owner {

		public final String name;
		public final UUID id;

		private Owner(String name, String id) {
			this(name, UUID.fromString(id));
		}

		private Owner(EntityPlayer ep) {
			this(ep.getCommandSenderName(), ep.getUniqueID());
		}

		public Owner(String name, UUID id) {
			if (id == null || name == null || name.isEmpty())
				throw new IllegalArgumentException("Null owner data!");
			this.name = name;
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Owner) {
				Owner ow = (Owner)o;
				return ow.name.equals(name) && ow.id.equals(id);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return name.hashCode()^id.hashCode();
		}

		private void writeToNBT(NBTTagCompound tag) {
			tag.setString("name", name);
			tag.setString("id", id.toString());
		}

		private static Owner readFromNBT(NBTTagCompound tag) {
			return new Owner(tag.getString("name"), tag.getString("id"));
		}

		@Override
		public String toString() {
			return name+" ("+id.toString()+")";
		}

	}

	public boolean enforce(Protections lvl) {
		return lvl.enabled(enforcementLevel);
	}

	public boolean log(Protections lvl) {
		return lvl.enabled(loggingLevel);
	}

	public static enum Protections {
		BREAK("Block Breaking"),
		PLACE("Block Placing"),
		GUI("Opening GUIs"),
		ANIMALS("Killing Animals"),
		PVP("PvP"),
		ITEMS("Item Pickup");

		public final String desc;

		public static Protections[] list = values();

		private Protections(String s) {
			desc = s;
		}

		protected boolean enabled(int flags) {
			return (flags & (1 << this.ordinal())) != 0;
		}
	}

}
