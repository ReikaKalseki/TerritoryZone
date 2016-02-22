/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2015
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.TerritoryZone.Event;

import Reika.TerritoryZone.Territory;
import Reika.TerritoryZone.Territory.Protections;
import cpw.mods.fml.common.eventhandler.Cancelable;


@Cancelable
public class TerritoryEnforceEvent extends TerritoryEvent {

	public final Protections action;

	public TerritoryEnforceEvent(Territory t, Protections p) {
		super(t);

		action = p;
	}

}
