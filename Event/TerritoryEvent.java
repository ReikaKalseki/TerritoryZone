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


public abstract class TerritoryEvent extends Event {

	public final Territory territory;

	public TerritoryEvent(Territory t) {
		territory = t;
	}

}
