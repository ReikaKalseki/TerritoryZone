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

import Reika.DragonAPI.Interfaces.Configuration.BooleanConfig;
import Reika.DragonAPI.Interfaces.Configuration.IntegerConfig;
import Reika.DragonAPI.Interfaces.Configuration.UserSpecificConfig;

public enum TerritoryOptions implements IntegerConfig, BooleanConfig, UserSpecificConfig {

	//ENFORCE("Enforcement Level", 0),
	//LOG("Logging Level", 4),
	OVERLAY("Do in-zone overlay", true),
	FADEOUT("Fade Overlay In and Out", false),
	SMALLOVERLAY("Shrink Overlay", false),
	FAKEPLAYER("Intercept Fake Players", true),
	FILELOG("Log to Dedicated File", true),
	TELECOMMAND("Teleport Command State", 2), //0 = Disable the command, 1 = Admins only, 2 = Anyone can use
	TELECOMMANDDIM("Allow GotoTerritory Command Interdimensionally", false);

	private String label;
	private boolean defaultState;
	private int defaultValue;
	private Class type;

	public static final TerritoryOptions[] optionList = TerritoryOptions.values();

	private TerritoryOptions(String l, boolean d) {
		label = l;
		defaultState = d;
		type = boolean.class;
	}

	private TerritoryOptions(String l, int d) {
		label = l;
		defaultValue = d;
		type = int.class;
	}

	public boolean isBoolean() {
		return type == boolean.class;
	}

	public boolean isNumeric() {
		return type == int.class;
	}

	public Class getPropertyType() {
		return type;
	}

	public String getLabel() {
		return label;
	}

	public boolean getState() {
		return (Boolean)TerritoryZone.config.getControl(this.ordinal());
	}

	public int getValue() {
		return (Integer)TerritoryZone.config.getControl(this.ordinal());
	}

	public boolean isDummiedOut() {
		return type == null;
	}

	@Override
	public boolean getDefaultState() {
		return defaultState;
	}

	@Override
	public int getDefaultValue() {
		return defaultValue;
	}

	@Override
	public boolean isEnforcingDefaults() {
		return false;
	}

	@Override
	public boolean shouldLoad() {
		return true;
	}

	@Override
	public boolean isUserSpecific() {
		switch(this) {
			default:
				return false;
		}
	}

	public static boolean registerTeleportCommand() {
		return TELECOMMAND.getValue() > 0;
	}

	public static boolean teleportCommandAdminOnly() {
		return TELECOMMAND.getValue() < 2;
	}

}
