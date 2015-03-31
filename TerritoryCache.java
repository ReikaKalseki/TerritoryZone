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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import net.minecraft.client.Minecraft;
import Reika.DragonAPI.Libraries.IO.ReikaChatHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TerritoryCache {

	public static final TerritoryCache instance = new TerritoryCache();

	private final Collection<Territory> zones = new ArrayList();

	private TerritoryCache() {

	}

	public void clear() {
		zones.clear();
	}

	public void addTerritory(Territory t) {
		zones.add(t);
	}

	public void verifyTerritories() {
		for (Territory t : zones) {
			if (t.ownedBy(Minecraft.getMinecraft().thePlayer)) {
				for (Territory t2 : zones) {
					if (t != t2 && t.intersects(t2)) {
						this.notifyPlayerOfIntersection(t, t2);
					}
				}
			}
		}
	}

	private void notifyPlayerOfIntersection(Territory t, Territory t2) {
		ReikaChatHelper.write("Your territory "+t+" overlaps territory "+t2+"!");
	}

	public Collection<Territory> getTerritories() {
		return Collections.unmodifiableCollection(zones);
	}

}
