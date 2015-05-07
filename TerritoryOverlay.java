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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderWorldEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import Reika.DragonAPI.Instantiable.Event.Client.EntityRenderingLoopEvent;
import Reika.DragonAPI.Libraries.IO.ReikaRenderHelper;
import Reika.DragonAPI.Libraries.IO.ReikaTextureHelper;
import Reika.DragonAPI.Libraries.Java.ReikaGLHelper.BlendMode;
import Reika.TerritoryZone.Territory.TerritoryShape;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TerritoryOverlay {

	public static final TerritoryOverlay instance = new TerritoryOverlay();

	private static final boolean smallRender = TerritoryOptions.SMALLOVERLAY.getState();
	private static final boolean fadeRender = TerritoryOptions.FADEOUT.getState();

	private int overlayAlpha = 1024;
	private int overlayAlphaDir = 0;

	private TerritoryOverlay() {

	}

	@SubscribeEvent
	public void renderInWorld(EntityRenderingLoopEvent evt) {
		if (MinecraftForgeClient.getRenderPass() == 1) {

			GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

			GL11.glDisable(GL11.GL_LIGHTING);
			BlendMode.DEFAULT.apply();
			ReikaRenderHelper.disableEntityLighting();
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glAlphaFunc(GL11.GL_GEQUAL, 1/256F);
			if (Keyboard.isKeyDown(Keyboard.KEY_TAB))
				GL11.glDisable(GL11.GL_CULL_FACE);

			double p2 = -TileEntityRendererDispatcher.staticPlayerX;
			double p4 = -TileEntityRendererDispatcher.staticPlayerY;
			double p6 = -TileEntityRendererDispatcher.staticPlayerZ;

			GL11.glPushMatrix();

			GL11.glTranslated(p2, p4, p6);

			Tessellator v5 = Tessellator.instance;
			EntityPlayer ep = Minecraft.getMinecraft().thePlayer;
			double x = ep.posX;
			double y = ep.posY;
			double z = ep.posZ;
			for (Territory t : TerritoryCache.instance.getTerritories()) {
				if (t.origin.dimensionID == ep.worldObj.provider.dimensionId) {
					int ox = t.origin.xCoord;
					int oy = t.origin.yCoord;
					int oz = t.origin.zCoord;
					int minx = ox-t.radius;
					int miny = t.shape == TerritoryShape.PRISM ? 0 : Math.max(0, oy-t.radius);
					int minz = oz-t.radius;
					int maxx = ox+t.radius+1;
					int maxy = t.shape == TerritoryShape.PRISM ? 256 : Math.min(256, oy+t.radius+1);
					int maxz = oz+t.radius+1;

					//double d = ReikaMathLibrary.py3d(ox-x, oy-y, oz-z)-t.radius;

					ReikaTextureHelper.bindTexture(TerritoryZone.class, "Textures/inworld3g.png");

					double u = 4;
					double v = 4;

					v5.startDrawingQuads();
					v5.setBrightness(240);
					int a = 255;//d > 0 ? (int)(255-d*2) : 255;
					v5.setColorRGBA_I(t.color, a);
					v5.addVertexWithUV(minx, maxy, minz, 0, v);
					v5.addVertexWithUV(maxx, maxy, minz, u, v);
					v5.addVertexWithUV(maxx, miny, minz, u, 0);
					v5.addVertexWithUV(minx, miny, minz, 0, 0);

					v5.addVertexWithUV(minx, miny, maxz, 0, 0);
					v5.addVertexWithUV(maxx, miny, maxz, u, 0);
					v5.addVertexWithUV(maxx, maxy, maxz, u, v);
					v5.addVertexWithUV(minx, maxy, maxz, 0, v);

					v5.addVertexWithUV(minx, miny, minz, 0, 0);
					v5.addVertexWithUV(minx, miny, maxz, u, 0);
					v5.addVertexWithUV(minx, maxy, maxz, u, v);
					v5.addVertexWithUV(minx, maxy, minz, 0, v);

					v5.addVertexWithUV(maxx, maxy, minz, 0, v);
					v5.addVertexWithUV(maxx, maxy, maxz, u, v);
					v5.addVertexWithUV(maxx, miny, maxz, u, 0);
					v5.addVertexWithUV(maxx, miny, minz, 0, 0);
					v5.draw();
				}
			}

			GL11.glPopMatrix();
			GL11.glPopAttrib();
		}
	}

	@SubscribeEvent
	public void renderHUD(RenderGameOverlayEvent evt) {
		if (evt.type == ElementType.HELMET && TerritoryOptions.OVERLAY.getState()) {
			EntityPlayer ep = Minecraft.getMinecraft().thePlayer;

			World world = Minecraft.getMinecraft().theWorld;
			int x = MathHelper.floor_double(ep.posX);
			int y = MathHelper.floor_double(ep.posY);
			int z = MathHelper.floor_double(ep.posZ);


			boolean flag = false;
			FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
			for (Territory t : TerritoryCache.instance.getTerritories()) {
				if (t.isInZone(ep.worldObj, ep)) {
					if (t.ownedBy(ep)) {
						if (Keyboard.isKeyDown(Keyboard.KEY_TAB)) {
							ReikaTextureHelper.bindTexture(TerritoryZone.class, "Textures/HUD.png");
							GL11.glEnable(GL11.GL_BLEND);
							GL11.glAlphaFunc(GL11.GL_GREATER, 1/255F);
							int h = evt.resolution.getScaledHeight();
							int s = 16;
							int dd = 8;
							int dy = h-s-dd;
							Tessellator.instance.startDrawingQuads();
							Tessellator.instance.setColorRGBA_I(0xffffff, overlayAlpha);
							Tessellator.instance.addVertexWithUV(dd, dy+s, 0, 0, 1);
							Tessellator.instance.addVertexWithUV(dd+s, dy+s, 0, 1, 1);
							Tessellator.instance.addVertexWithUV(dd+s, dy, 0, 1, 0);
							Tessellator.instance.addVertexWithUV(dd, dy, 0, 0, 0);
							Tessellator.instance.draw();
							fr.drawString("In your territory", dd+s+4, dy+4, 0xffffff, true);
							GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
						}
					}
					else {
						flag = true;
						GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
						GL11.glPushMatrix();
						if (smallRender) {
							double sz = 0.5;
							GL11.glScaled(sz, sz, sz);
							GL11.glTranslated(0, evt.resolution.getScaledHeight_double()*0.9, 0);
						}
						//fr.drawString(t.toString(), 0, 0, 0);
						ReikaTextureHelper.bindTexture(TerritoryZone.class, "Textures/HUD.png");
						GL11.glEnable(GL11.GL_BLEND);
						GL11.glAlphaFunc(GL11.GL_GREATER, 1/255F);
						int h = evt.resolution.getScaledHeight();
						int s = 64;
						int dd = 16;
						int dy = h-s-dd;

						if (fadeRender) {
							int max = 8192;//384;
							int min = -65536;//-256;
							if (overlayAlpha >= max)
								overlayAlphaDir = -1;
							else if (overlayAlpha == min)
								overlayAlphaDir = 1;
							overlayAlpha += overlayAlphaDir;
						}

						if (overlayAlpha >= 0) {
							Tessellator.instance.startDrawingQuads();
							Tessellator.instance.setColorRGBA_I(0xffffff, overlayAlpha);
							Tessellator.instance.addVertexWithUV(dd, dy+s, 0, 0, 1);
							Tessellator.instance.addVertexWithUV(dd+s, dy+s, 0, 1, 1);
							Tessellator.instance.addVertexWithUV(dd+s, dy, 0, 1, 0);
							Tessellator.instance.addVertexWithUV(dd, dy, 0, 0, 0);
							Tessellator.instance.draw();
							GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
						}

						if ((overlayAlpha >= -1024 && overlayAlphaDir == -1) || overlayAlpha >= 0) {
							fr.drawString("In zone owned by "+t.getOwnerNames()+"!", dd+s+4, dy+4, 0xffffff, true);
							fr.drawString(t.getBoundsDesc(), dd+s+4, dy+4+fr.FONT_HEIGHT+2, 0xffffff, true);
							fr.drawString("Do not mine or harvest without permission!", dd+s+4, dy+4+(fr.FONT_HEIGHT+2)*2, 0xffffff, true);
							fr.drawString("Any actions you take here may be logged.", dd+s+4, dy+4+(fr.FONT_HEIGHT+2)*3, 0xffffff, true);
						}

						//break;

						GL11.glPopAttrib();
						GL11.glPopMatrix();
					}

					/*
				if (t.origin.dimensionID == world.provider.dimensionId) {
					GL11.glPushMatrix();
					double p2 = x-TileEntityRendererDispatcher.staticPlayerX;
					double p4 = y-TileEntityRendererDispatcher.staticPlayerY;
					double p6 = z-TileEntityRendererDispatcher.staticPlayerZ;
					GL11.glTranslated(x, y, z);

					int ox = t.origin.xCoord;
					int oz = t.origin.zCoord;
					int minx = ox-t.radius;
					int minz = oz-t.radius;
					int maxx = ox+t.radius+1;
					int maxz = oz+t.radius+1;

					Tessellator v5 = Tessellator.instance;
					ReikaTextureHelper.bindTerrainTexture();

					v5.startDrawingQuads();
					v5.setColorOpaque_I(0xffffff);
					v5.addVertexWithUV(minx, 256, minz, 0, 1);
					v5.addVertexWithUV(maxx, 256, minz, 1, 1);
					v5.addVertexWithUV(maxx, 0, minz, 1, 0);
					v5.addVertexWithUV(minx, 0, minz, 0, 0);

					v5.addVertexWithUV(minx, 0, maxz, 0, 0);
					v5.addVertexWithUV(maxx, 0, maxz, 1, 0);
					v5.addVertexWithUV(maxx, 256, maxz, 1, 1);
					v5.addVertexWithUV(minx, 256, maxz, 0, 1);

					v5.addVertexWithUV(minx, 0, minz, 0, 0);
					v5.addVertexWithUV(minx, 0, maxz, 1, 0);
					v5.addVertexWithUV(minx, 256, maxz, 1, 1);
					v5.addVertexWithUV(minx, 256, minz, 0, 1);

					v5.addVertexWithUV(maxx, 256, minz, 0, 1);
					v5.addVertexWithUV(maxx, 256, maxz, 1, 1);
					v5.addVertexWithUV(maxx, 0, maxz, 1, 0);
					v5.addVertexWithUV(maxx, 0, minz, 0, 0);
					v5.draw();
					GL11.glPopMatrix();
				}*/
				}
			}

			if (!flag || Keyboard.isKeyDown(Keyboard.KEY_TAB)) {
				overlayAlpha = 4096;
			}
		}
	}

	@SubscribeEvent
	public void renderWorld(RenderWorldEvent.Post evt) {
		/*
		if (evt.pass == 0 && false) {
			World world = Minecraft.getMinecraft().theWorld;
			EntityPlayer ep = Minecraft.getMinecraft().thePlayer;
			int x = MathHelper.floor_double(ep.posX);
			int y = MathHelper.floor_double(ep.posY);
			int z = MathHelper.floor_double(ep.posZ);
			for (Territory t : TerritoryCache.instance.getTerritories()) {
				if (t.origin.dimensionID == world.provider.dimensionId) {
					boolean inx = ReikaMathLibrary.isValueInsideBoundsIncl(x, x+16, evt.renderer.posX);
					boolean inz = ReikaMathLibrary.isValueInsideBoundsIncl(z, z+16, evt.renderer.posZ);
					ReikaJavaLibrary.pConsole(x+"/"+evt.renderer.posX+", "+z+"/"+evt.renderer.posZ+" ; "+inx+"&"+inz);
					if (inx && inz) {
						GL11.glPushMatrix();
						//double p2 = x-TileEntityRendererDispatcher.staticPlayerX;
						//double p4 = y-TileEntityRendererDispatcher.staticPlayerY;
						//double p6 = z-TileEntityRendererDispatcher.staticPlayerZ;
						int ox = t.origin.xCoord;
						int oz = t.origin.zCoord;
						int minx = ox-t.radius;
						int minz = oz-t.radius;
						int maxx = ox+t.radius+1;
						int maxz = oz+t.radius+1;
						//GL11.glTranslated(p2, p4, p6);
						//ReikaRenderHelper.prepareGeoDraw(true);
						Tessellator v5 = Tessellator.instance;
						double o = 0.0125;
						int r = 255;
						int g = 255;
						int b = 255;
						ReikaTextureHelper.bindTerrainTexture();
						v5.startDrawingQuads();
						v5.setColorOpaque_I(0xffffff);
						v5.addVertexWithUV(minx, 256, minz, 0, 1);
						v5.addVertexWithUV(maxx, 256, minz, 1, 1);
						v5.addVertexWithUV(maxx, 0, minz, 1, 0);
						v5.addVertexWithUV(minx, 0, minz, 0, 0);

						v5.addVertexWithUV(minx, 0, maxz, 0, 0);
						v5.addVertexWithUV(maxx, 0, maxz, 1, 0);
						v5.addVertexWithUV(maxx, 256, maxz, 1, 1);
						v5.addVertexWithUV(minx, 256, maxz, 0, 1);

						v5.addVertexWithUV(minx, 0, minz, 0, 0);
						v5.addVertexWithUV(minx, 0, maxz, 1, 0);
						v5.addVertexWithUV(minx, 256, maxz, 1, 1);
						v5.addVertexWithUV(minx, 256, minz, 0, 1);

						v5.addVertexWithUV(maxx, 256, minz, 0, 1);
						v5.addVertexWithUV(maxx, 256, maxz, 1, 1);
						v5.addVertexWithUV(maxx, 0, maxz, 1, 0);
						v5.addVertexWithUV(maxx, 0, minz, 0, 0);
						v5.draw();
						//ReikaRenderHelper.exitGeoDraw();
						GL11.glPopMatrix();
					}
				}
			}
		}*/
	}

}
