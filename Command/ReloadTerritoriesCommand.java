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

import net.minecraft.command.ICommandSender;
import Reika.DragonAPI.Command.DragonCommandBase;
import Reika.TerritoryZone.Territory;
import Reika.TerritoryZone.TerritoryLoader;
import Reika.TerritoryZone.TerritoryZone;

public class ReloadTerritoriesCommand extends DragonCommandBase {

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		TerritoryZone.reloadTerritories();
		this.sendChatToSender(ics, "Territories Reloaded. "+TerritoryLoader.instance.getTerritories().size()+" Territories:");
		for (Territory t : TerritoryLoader.instance.getTerritories()) {
			this.sendChatToSender(ics, t.toString());
		}
	}

	@Override
	public String getCommandString() {
		return "reloadterritories";
	}

	@Override
	protected boolean isAdminOnly() {
		return true;
	}

}
