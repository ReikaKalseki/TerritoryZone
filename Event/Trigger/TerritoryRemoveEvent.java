/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2015
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.TerritoryZone.Event.Trigger;

import Reika.TerritoryZone.Territory;
import Reika.TerritoryZone.Event.TerritoryEvent;


public class TerritoryRemoveEvent extends TerritoryEvent {

	public TerritoryRemoveEvent(Territory t) {
		super(t);
	}

}
