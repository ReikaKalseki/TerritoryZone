/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.TerritoryZone;

import java.util.Collection;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import Reika.DragonAPI.Command.DragonCommandBase;
import Reika.DragonAPI.Libraries.ReikaEntityHelper;


public class TerritoryTeleportCommand extends DragonCommandBase {

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		EntityPlayerMP ep = this.getCommandSenderAsPlayer(ics);
		Collection<Territory> c = TerritoryLoader.instance.getTerritoriesFor(ep);
		if (c.isEmpty()) {
			this.sendChatToSender(ics, EnumChatFormatting.RED+"You have no territories.");
		}
		else {
			Territory t = c.iterator().next();
			if (t.isInZone(ep.worldObj, ep)) {
				this.sendChatToSender(ics, EnumChatFormatting.RED+"You are already in your territory!");
			}
			else {
				if (ep.worldObj.provider.dimensionId != t.origin.dimensionID)
					ReikaEntityHelper.transferEntityToDimension(ep, t.origin.dimensionID);
				ep.setPositionAndUpdate(t.origin.xCoord, t.origin.yCoord, t.origin.zCoord);
			}
		}
	}

	@Override
	public String getCommandString() {
		return "gototerritory";
	}

	@Override
	protected boolean isAdminOnly() {
		return TerritoryOptions.teleportCommandAdminOnly();
	}

}
