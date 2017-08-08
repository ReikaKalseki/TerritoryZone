/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.TerritoryZone.Event;

import Reika.TerritoryZone.Territory;
import cpw.mods.fml.common.eventhandler.Event;


public class TerritoryCollisionEvent extends Event {

	public final Territory territoryA;
	public final Territory territoryB;

	public TerritoryCollisionEvent(Territory t1, Territory t2) {
		territoryA = t1;
		territoryB = t2;
	}

}
