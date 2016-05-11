/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2016
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.TerritoryZone.Event;

import Reika.TerritoryZone.Territory;


public class TerritoryLoggingEvent extends TerritoryEvent {

	public final String logMessage;

	public TerritoryLoggingEvent(Territory t, String log) {
		super(t);

		logMessage = log;
	}

}
