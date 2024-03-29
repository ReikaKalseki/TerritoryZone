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

import java.awt.Polygon;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import Reika.DragonAPI.Instantiable.Data.Immutable.BlockBox;
import Reika.DragonAPI.Instantiable.Data.Immutable.WorldLocation;
import Reika.DragonAPI.Instantiable.IO.LuaBlock;
import Reika.DragonAPI.Instantiable.IO.LuaBlock.LuaBlockDatabase;
import Reika.DragonAPI.Libraries.ReikaNBTHelper;
import Reika.DragonAPI.Libraries.ReikaNBTHelper.NBTTypes;
import Reika.DragonAPI.Libraries.ReikaPlayerAPI;
import Reika.DragonAPI.Libraries.IO.ReikaChatHelper;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.MathSci.ReikaVectorHelper;
import Reika.TerritoryZone.Event.TerritoryCreateEvent;

public final class Territory {

	public final String id;
	public final WorldLocation origin;
	public final int radius;
	public final int color;
	private final Collection<Owner> owners = new HashSet();
	public final TerritoryShape shape;
	private final EnumSet<Protections> logging = ReikaJavaLibrary.getConditionalEnumSet(Protections.class, p -> p.logByDefault());
	private final EnumSet<Protections> chat = ReikaJavaLibrary.getConditionalEnumSet(Protections.class, p -> p.chatByDefault());
	private final EnumSet<Protections> protection = ReikaJavaLibrary.getConditionalEnumSet(Protections.class, p -> p.protectByDefault());

	//public Territory(int dim, int x, int y, int z, int r, String name, UUID uid, TerritoryShape sh) {
	//	this(new WorldLocation(dim, x, y, z), r, name, uid, sh);
	//}

	public Territory(String id, WorldLocation loc, int r, int clr, TerritoryShape sh) {
		this.id = id;
		origin = loc;
		radius = r;
		shape = sh;
		color = clr;
		MinecraftForge.EVENT_BUS.post(new TerritoryCreateEvent(this));
	}

	public static Territory getFromTwoPoints(String id, World world, int x1, int y1, int z1, int x2, int y2, int z2, EntityPlayer ep) {
		int dx = x2-x1;
		int dz = z2-z1;
		int r = (dx+dz)/2;
		int x = (x1+x2)/2;
		int y = y1 == Integer.MIN_VALUE ? Integer.MIN_VALUE : (y1+y2)/2;
		int z = (x1+z2)/2;
		TerritoryShape s = y == Integer.MIN_VALUE ? TerritoryShape.PRISM : TerritoryShape.CUBE;
		return new Territory(id, new WorldLocation(world, x, s == TerritoryShape.PRISM ? 64 : y, z), r, 0xff0000, s).addOwner(ep);
	}

	public Territory addOwner(EntityPlayer ep) {
		return this.addOwner(new Owner(ep));
	}

	public Territory addOwner(UUID id, String name) {
		return this.addOwner(new Owner(name, id));
	}

	public Territory addOwner(Owner o) {
		owners.add(o);
		return this;
	}

	public Territory setProtectionLevel(Protections p, boolean log, boolean chat, boolean protect) {
		if (log)
			logging.add(p);
		else
			logging.remove(p);
		if (chat)
			this.chat.add(p);
		else
			this.chat.remove(p);
		if (protect)
			protection.add(p);
		else
			protection.remove(p);
		return this;
	}
	/*
	public long getArea() {
		return ReikaMathLibrary.longpow(radius*2+1, 2);
	}

	public long getVolume() {
		return ReikaMathLibrary.longpow(radius*2+1, 3);
	}
	 */
	public boolean intersects(Territory t) {
		return origin.dimensionID == t.origin.dimensionID && this.intersectsComplex(t);
	}

	private boolean intersectsComplex(Territory t) {
		/*
		WorldLocation loc1 = origin;
		WorldLocation loc2 = t.origin;
		Vec3 vec = ReikaVectorHelper.getVec2Pt(loc1.xCoord, loc1.yCoord, loc1.zCoord, loc2.xCoord, loc2.yCoord, loc2.zCoord);
		double r1 = this.getEdgeDistanceAlong(vec);
		double r2 = t.getEdgeDistanceAlong(ReikaVectorHelper.getInverseVector(vec));
		return r1+r2 > vec.lengthVector();
		 */
		if (shape == TerritoryShape.SPHERE && t.shape == TerritoryShape.SPHERE) {
			double d = origin.getDistanceTo(t.origin);
			return d < Math.max(radius, t.radius);
		}
		if (shape == TerritoryShape.CYLINDER && t.shape == TerritoryShape.CYLINDER) {
			double d = origin.to2D().getDistanceTo(t.origin.to2D());
			return d < Math.max(radius, t.radius);
		}
		if (shape == t.shape && (shape == TerritoryShape.PRISM && shape == TerritoryShape.CUBE)) {
			AxisAlignedBB box1 = this.getAABB();
			AxisAlignedBB box2 = this.getAABB();
			return box1.intersectsWith(box2);
		}
		Polygon p1 = shape.getFootprintPolygon(origin, radius);
		Polygon p2 = t.shape.getFootprintPolygon(t.origin, t.radius);
		Area a = new Area(p1);
		a.intersect(new Area(p2));
		if (a.isEmpty())
			return false;
		if (t.shape.hasVerticalComponent() && shape.hasVerticalComponent()) {
			if (origin.yCoord < t.origin.yCoord) {
				int y1 = origin.yCoord+radius;
				int y2 = t.origin.yCoord-t.radius;
				if (y1 < y2)
					return false;
			}
			else {
				int y1 = t.origin.yCoord+t.radius;
				int y2 = origin.yCoord-radius;
				if (y1 < y2)
					return false;
			}
		}
		return true;
	}

	@Deprecated
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

	public boolean isInFootprint(int x, int z) {
		return shape.isInZone(origin.xCoord, 0, origin.zCoord, x, 0, z, radius);
	}

	public boolean ownedBy(EntityPlayer ep) {
		return owners.contains(new Owner(ep));
	}

	@Override
	public String toString() {
		return radius+"-"+shape.name()+" @ "+origin.toString()+" {"+logging+"/"+protection+"} "+" by "+owners.toString();
	}

	public String toSimpleString() {
		return radius+"-"+shape.name()+" @ "+origin.toString();
	}

	public BlockBox getBounds() {
		return shape.getBounds(origin, radius);
	}

	public String getBoundsDesc() {
		return shape.getBoundsAsString(origin, radius);
	}

	public String getOwnerNames() {
		StringBuilder sb = new StringBuilder();
		for (Owner o : owners) {
			sb.append(o.name);
			sb.append(", ");
		}
		return sb.length() > 2 ? sb.substring(0, sb.length()-2) : sb.toString();
	}

	public ArrayList<String> getOwnerNameList() {
		ArrayList<String> li = new ArrayList();
		for (Owner o : owners) {
			li.add(o.name);
		}
		return li;
	}

	public HashSet<UUID> getOwnerIDs() {
		HashSet<UUID> set = new HashSet();
		for (Owner o : owners) {
			set.add(o.id);
		}
		return set;
	}

	public void writeToNBT(NBTTagCompound nbt) {
		origin.writeToNBT("loc", nbt);
		nbt.setInteger("r", radius);
		nbt.setInteger("color", color);
		nbt.setInteger("shape", shape.ordinal());
		nbt.setString("id", id);

		nbt.setTag("logging", ReikaNBTHelper.getTagForObject(logging, ReikaNBTHelper.getEnumConverter(Protections.class)));
		nbt.setTag("chat", ReikaNBTHelper.getTagForObject(chat, ReikaNBTHelper.getEnumConverter(Protections.class)));
		nbt.setTag("protection", ReikaNBTHelper.getTagForObject(protection, ReikaNBTHelper.getEnumConverter(Protections.class)));

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
		TerritoryShape sh = TerritoryShape.list[nbt.getInteger("shape")];
		NBTTagList li = nbt.getTagList("owners", NBTTypes.COMPOUND.ID);
		Territory t = new Territory(nbt.getString("id"), loc, radius, clr, sh);

		t.logging.clear();
		t.chat.clear();
		t.protection.clear();
		t.logging.addAll((Collection<Protections>)ReikaNBTHelper.getValue(nbt.getTag("logging"), ReikaNBTHelper.getEnumConverter(Protections.class)));
		t.chat.addAll((Collection<Protections>)ReikaNBTHelper.getValue(nbt.getTag("chat"), ReikaNBTHelper.getEnumConverter(Protections.class)));
		t.protection.addAll((Collection<Protections>)ReikaNBTHelper.getValue(nbt.getTag("protection"), ReikaNBTHelper.getEnumConverter(Protections.class)));

		for (Object o : li.tagList) {
			NBTTagCompound dat = (NBTTagCompound)o;
			Owner own = Owner.readFromNBT(dat);
			t.owners.add(own);
		}
		return t;
	}

	public static enum TerritoryShape {
		CUBE("Cubic Zone"),
		PRISM("Full-height square perimeter"),
		SPHERE("Spherical Zone"),
		CYLINDER("Full-height circular perimeter");

		public final String desc;

		public static final TerritoryShape[] list = values();

		private TerritoryShape(String s) {
			desc = s;
		}

		public boolean hasVerticalComponent() {
			return this == CUBE || this == SPHERE;
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

		private Polygon getFootprintPolygon(WorldLocation loc, int radius) {
			Polygon p = new Polygon();
			switch(this) {
				case CUBE:
				case PRISM:
					p.addPoint(loc.xCoord-radius, loc.zCoord+radius+1);
					p.addPoint(loc.xCoord+radius+1, loc.zCoord+radius+1);
					p.addPoint(loc.xCoord+radius+1, loc.zCoord-radius);
					p.addPoint(loc.xCoord-radius, loc.zCoord-radius);
					break;
				case CYLINDER:
				case SPHERE:
					for (double d = 0; d < 360; d += 0.5) {
						double ang = Math.toRadians(d);
						int x = loc.xCoord+(int)(radius*Math.cos(ang));
						int z = loc.zCoord+(int)(radius*Math.sin(ang));
						p.addPoint(x, z);
					}
					break;
			}
			return p;
		}

		private BlockBox getBounds(WorldLocation loc, int radius) {
			int ox = loc.xCoord;
			int oy = loc.yCoord;
			int oz = loc.zCoord;
			int minx = ox-radius;
			int miny = this == CYLINDER || this == PRISM ? 0 : oy-radius;
			int minz = oz-radius;
			int maxx = ox+radius+1;
			int maxy = this == CYLINDER || this == PRISM ? Integer.MAX_VALUE : oy+radius+1;
			int maxz = oz+radius+1;
			return new BlockBox(minx, miny, minz, maxx, maxy, maxz);
		}

		private String getBoundsAsString(WorldLocation loc, int radius) {
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

		public boolean isAtEdge(double xo, double zo, double x, double z, int r) {
			switch(this) {
				case CYLINDER:
				case SPHERE:
					double dd = ReikaMathLibrary.py3d(x-xo, 0, z-zo);
					return dd <= r+1 && dd >= r-1;
				case PRISM:
				case CUBE:
					return x == xo-r || x == xo+r+1 || z == zo-r || z == zo+r+1;
				default:
					return false;
			}
		}
	}

	public static final class Owner {

		public final String name;
		public final UUID id;

		public Owner(String name, String id) {
			this(name, UUID.fromString(id));
		}

		public Owner(EntityPlayer ep) {
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
		return protection.contains(lvl);
	}

	public boolean log(Protections lvl) {
		return logging.contains(lvl);
	}

	public boolean chat(Protections lvl) {
		return chat.contains(lvl);
	}

	public static enum Protections {
		BREAK("Block Breaking"),
		PLACE("Block Placing"),
		GUI("Opening GUIs"),
		ANIMALS("Killing Animals"),
		PVP("PvP"),
		ITEMS("Item Pickup"),
		FIRESPREAD("Fire Spread"),
		RESOURCE("Resources");

		public final String desc;

		public static Protections[] list = values();

		private Protections(String s) {
			desc = s;
		}

		private String getFormattedNotification(Object... args) {
			switch(this) {
				case ANIMALS:
					return "Attacked "+args[0];
				case BREAK:
					return "Broke block "+((Block)args[0]).getLocalizedName()+" at "+args[1]+", "+args[2]+", "+args[3];
				case GUI:
					return "Opened a GUI at "+args[0]+", "+args[1]+", "+args[2];
				case ITEMS:
					return "Picked up an item "+((ItemStack)args[0]).getDisplayName()+" at "+args[1]+", "+args[2]+", "+args[3];
				case PLACE:
					return "Placed block "+((Block)args[0]).getLocalizedName()+" at "+args[1]+", "+args[2]+", "+args[3];
				case PVP:
					return "Attacked the owner '"+((Entity)args[0]).getCommandSenderName()+"'";
				case FIRESPREAD:
					return "Fire spread to "+args[0]+", "+args[1]+", "+args[2];
				case RESOURCE:
					return "Harvested resource "+args[0]+" @ "+args[1]+", "+args[2]+", "+args[3];
			}
			return "";
		}

		public boolean logByDefault() {
			switch(this) {
				case FIRESPREAD:
					return false;
				default:
					return true;
			}
		}

		public boolean chatByDefault() {
			switch(this) {
				case ITEMS:
				case PVP:
				case FIRESPREAD:
					return false;
				default:
					return true;
			}
		}

		public boolean protectByDefault() {
			switch(this) {
				case BREAK:
				case PLACE:
				case RESOURCE:
					return true;
				default:
					return false;
			}
		}
	}

	public void sendChatToOwner(Protections p, EntityPlayer ep, Object... args) {
		String s = (ep != null ? ep.getCommandSenderName() : "")+" "+p.getFormattedNotification(args);
		for (Owner o : owners) {
			EntityPlayer epo = ReikaPlayerAPI.getPlayerByIDAnyWorld(o.id);
			if (epo != null)
				ReikaChatHelper.sendChatToPlayer(epo, s);
		}
	}

	public boolean isAtEdge(int x, int z) {
		return shape.isAtEdge(origin.xCoord, origin.zCoord, x, z, radius);
	}

	@Override
	public int hashCode() {
		return (origin.hashCode() ^ radius) + owners.hashCode() * (1+shape.ordinal());
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Territory))
			return false;
		Territory t = (Territory)o;
		return t.origin.equals(origin) && t.radius == radius && t.shape == shape && t.owners.equals(owners);
	}

	public TerritoryLuaBlock toLuaBlock(LuaBlockDatabase db) {
		TerritoryLuaBlock bk = new TerritoryLuaBlock(id, null, db);
		bk.putData("type", id);
		bk.putData("dimension", origin.dimensionID);

		TerritoryLuaBlock pos = new TerritoryLuaBlock("position", bk, db);
		pos.putData("x", origin.xCoord);
		pos.putData("y", origin.yCoord);
		pos.putData("z", origin.zCoord);
		pos.setOrdering(null);

		bk.putData("color", "0x"+Integer.toHexString(color));
		bk.putData("radius", radius);
		bk.putData("shape", shape.name());

		TerritoryLuaBlock owners = new TerritoryLuaBlock("owners", bk, db);
		for (Owner o : this.owners) {
			TerritoryLuaBlock owner = new TerritoryLuaBlock("{", owners, db);
			owner.putData("uuid", o.id.toString());
			owner.putData("username", o.name);
		}
		owners.setOrdering(null);

		TerritoryLuaBlock settings = new TerritoryLuaBlock("settings", bk, db);
		for (Protections p : Protections.list) {
			TerritoryLuaBlock prot = new TerritoryLuaBlock(p.name(), settings, db);
			prot.putData("log", this.log(p));
			prot.putData("chat", this.chat(p));
			prot.putData("protect", this.enforce(p));
		}
		settings.setOrdering((s1, s2) -> Protections.valueOf(s1).compareTo(Protections.valueOf(s2)));
		bk.setOrdering(null);
		return bk;
	}

	public static class TerritoryLuaBlock extends LuaBlock {

		protected TerritoryLuaBlock(String n, LuaBlock parent, LuaBlockDatabase db) {
			super(n, parent, db);
			requiredElements.add("position");
			requiredElements.add("owners");
		}

	}

}
