package com.avaje.ebean.server.type;

/**
 * Marker interface for the Enum scalar types.
 */
public interface ScalarTypeEnum {

	/**
	 * Return the IN values for DB constraint construction.
	 */
	public String getContraintInValues();
	
}
