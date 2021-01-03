/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.TerritoryZone.Command;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.Command.DragonClientCommand;
import Reika.DragonAPI.Instantiable.Data.Immutable.BlockBox;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.Rendering.ReikaColorAPI;
import Reika.TerritoryZone.Territory;
import Reika.TerritoryZone.TerritoryCache;

public class PlotTerritoriesCommand extends DragonClientCommand {

	public static final int IMAGE_SIZE_LIMIT = 8192;

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		EntityPlayer ep = Minecraft.getMinecraft().thePlayer;
		int minX = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		Collection<Territory> c = new ArrayList();
		Collection<Territory> c2 = TerritoryCache.instance.getTerritories();
		for (Territory t : c2) {
			if (t.origin.dimensionID == ep.worldObj.provider.dimensionId) {
				BlockBox box = t.getBounds();
				minX = Math.min(minX, box.minX);
				minZ = Math.min(minZ, box.minZ);
				maxX = Math.max(maxX, box.maxX);
				maxZ = Math.max(maxZ, box.maxZ);
				c.add(t);
			}
		}
		if (c.isEmpty()) {
			this.sendChatToSender(ics, EnumChatFormatting.RED+"No territories to plot.");
			return;
		}
		if (args.length > 0) {
			minX = Integer.parseInt(args[0]);
			minZ = Integer.parseInt(args[1]);
			maxX = Integer.parseInt(args[2]);
			maxZ = Integer.parseInt(args[3]);
		}
		minX = ReikaMathLibrary.roundDownToX(512, minX);
		minZ = ReikaMathLibrary.roundDownToX(512, minZ);
		maxX = ReikaMathLibrary.roundUpToX(512, maxX);
		maxZ = ReikaMathLibrary.roundUpToX(512, maxZ);
		int sizeX = maxX-minX+1;
		int sizeZ = maxZ-minZ+1;
		int size = Math.max(sizeX, sizeZ);
		int scale = 1;
		while (size > IMAGE_SIZE_LIMIT*scale) {
			scale *= 2;
		}
		sizeX /= scale;
		sizeZ /= scale;
		BufferedImage img = new BufferedImage(sizeX+1, sizeZ+1, BufferedImage.TYPE_INT_ARGB);
		for (int x = minX; x <= maxX; x += scale) {
			for (int z = minZ; z <= maxZ; z += scale) {
				int over = 0xffffffff;
				int i = (x-minX)/scale;
				int k = (z-minZ)/scale;
				int b = 255;
				for (Territory t : c) {
					if (t.isAtEdge(x, z)) {
						over = 0xff000000 | t.color;
					}
					else if (t.isInFootprint(x, z)) {
						b *= 0.85F;
					}
				}
				if ((x-minX)%1024 == 0 && (z-minZ)%1024 == 0)
					over = 0xff000000;
				int clr = over == 0xffffffff ? (0xff000000 | ReikaColorAPI.GStoHex(b)) : over;
				img.setRGB(i, k, clr);
			}
		}

		Graphics graphics = img.getGraphics();
		Font ft = graphics.getFont();
		graphics.setFont(new Font(ft.getName(), ft.getStyle(), (int)(ft.getSize()*2.5)));
		graphics.setColor(new Color(0xff000000));
		int textSize = graphics.getFont().getSize();
		for (Territory t : c) {
			ArrayList<String> li = t.getOwnerNameList();
			for (int i = 0; i < li.size(); i++) {
				String s = li.get(i);
				int x = (t.origin.xCoord-minX)/scale-s.length()*textSize/4;
				int y = (t.origin.zCoord-minZ)/scale+i*textSize;
				graphics.drawString(s, x, y);
			}
		}
		graphics.dispose();

		try {
			File f = new File(DragonAPICore.getMinecraftDirectory(), "TerritoryMap/"+System.nanoTime()+".png");
			if (f.exists())
				f.delete();
			f.getParentFile().mkdirs();
			f.createNewFile();
			ImageIO.write(img, "png", f);
			this.sendChatToSender(ics, EnumChatFormatting.GREEN+"Plotted "+c.size()+" territories on a "+sizeX+" x "+sizeZ+" image.");
		}
		catch (Exception e) {
			this.sendChatToSender(ics, EnumChatFormatting.RED+"Could not write file: "+e.toString());
			e.printStackTrace();
		}
	}

	@Override
	public String getCommandString() {
		return "plotterritories";
	}

}
