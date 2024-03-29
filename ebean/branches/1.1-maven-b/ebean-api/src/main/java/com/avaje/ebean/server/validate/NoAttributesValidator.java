package com.avaje.ebean.server.validate;

/**
 * Abstract validator that has no attributes.
 */
public abstract class NoAttributesValidator implements Validator {

	private static final Object[] EMPTY = new Object[0];

	/**
	 * Returns an empty array.
	 */
	public Object[] getAttributes(){
		return EMPTY;
	}
	
	
}
