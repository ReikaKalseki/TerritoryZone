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

import net.minecraft.command.ICommandSender;
import Reika.DragonAPI.Command.DragonCommandBase;

public class ReloadTerritoriesCommand extends DragonCommandBase {

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		TerritoryLoader.instance.load();
		TerritoryDispatcher.instance.sendTerritoriesToAll();
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
