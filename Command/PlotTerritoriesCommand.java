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
import Reika.DragonAPI.Libraries.IO.ReikaColorAPI;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.TerritoryZone.Territory;
import Reika.TerritoryZone.TerritoryLoader;

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
		Collection<Territory> c2 = TerritoryLoader.instance.getTerritories();
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
				int i = (x-minX)/scale;
				int k = (z-minZ)/scale;
				int b = 255;
				for (Territory t : c) {
					if (t.isInFootprint(x, z)) {
						b *= 0.85F;
					}
				}
				if ((x-minX)%1024 == 0 && (z-minZ)%1024 == 0)
					b &= 0xffff0000;
				img.setRGB(i, k, 0xff000000 | ReikaColorAPI.GStoHex(b));
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
