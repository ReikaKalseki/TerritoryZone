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
import cpw.mods.fml.common.eventhandler.Cancelable;

@Cancelable
public class TerritoryModifyEvent extends TerritoryEvent {

	public TerritoryModifyEvent(Territory t) {
		super(t);
	}

}
