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


public class TerritoryRegisterEvent extends TerritoryEvent {

	public TerritoryRegisterEvent(Territory t) {
		super(t);
	}

}
