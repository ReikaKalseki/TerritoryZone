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

import net.minecraftforge.common.config.Configuration;
import Reika.DragonAPI.Exception.RegistrationException;
import Reika.DragonAPI.Interfaces.ConfigList;

public enum TerritoryOptions implements ConfigList {

	ENFORCE("Enforcement Level", 0),
	LOG("Logging Level", 4),
	OVERLAY("Do in-zone overlay", true),
	FADEOUT("Fade Overlay In and Out", false),
	SMALLOVERLAY("Shrink Overlay", false),
	FAKEPLAYER("Intercept Fake Players", true);

	private String label;
	private boolean defaultState;
	private int defaultValue;
	private float defaultFloat;
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

	public boolean isDecimal() {
		return type == float.class;
	}

	public float setDecimal(Configuration config) {
		if (!this.isDecimal())
			throw new RegistrationException(TerritoryZone.instance, "Config Property \""+this.getLabel()+"\" is not decimal!");
		return (float)config.get("Control Setup", this.getLabel(), defaultFloat).getDouble(defaultFloat);
	}

	public float getFloat() {
		return (Float)TerritoryZone.config.getControl(this.ordinal());
	}

	public Class getPropertyType() {
		return type;
	}

	public int setValue(Configuration config) {
		if (!this.isNumeric())
			throw new RegistrationException(TerritoryZone.instance, "Config Property \""+this.getLabel()+"\" is not numerical!");
		return config.get("Control Setup", this.getLabel(), defaultValue).getInt();
	}

	public String getLabel() {
		return label;
	}

	public boolean setState(Configuration config) {
		if (!this.isBoolean())
			throw new RegistrationException(TerritoryZone.instance, "Config Property \""+this.getLabel()+"\" is not boolean!");
		return config.get("Control Setup", this.getLabel(), defaultState).getBoolean(defaultState);
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
	public float getDefaultFloat() {
		return defaultFloat;
	}

	@Override
	public boolean isEnforcingDefaults() {
		return false;
	}

	@Override
	public boolean shouldLoad() {
		return true;
	}

	public static boolean enforceBlockBreak() {
		return ENFORCE.getValue() >= 1;
	}

	public static boolean enforceBlockPlace() {
		return ENFORCE.getValue() >= 2;
	}

	public static boolean enforceGui() {
		return ENFORCE.getValue() >= 3;
	}

	public static boolean enforceAnimalKill() {
		return ENFORCE.getValue() >= 4;
	}

	public static boolean logBlockBreak() {
		return LOG.getValue() >= 1;
	}

	public static boolean logBlockPlace() {
		return LOG.getValue() >= 2;
	}

	public static boolean logGui() {
		return LOG.getValue() >= 3;
	}

	public static boolean logAnimalKill() {
		return LOG.getValue() >= 4;
	}

}
