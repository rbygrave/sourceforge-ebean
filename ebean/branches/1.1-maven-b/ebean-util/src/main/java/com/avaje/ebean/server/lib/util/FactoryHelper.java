package com.avaje.ebean.server.lib.util;

import java.lang.reflect.Constructor;

/**
 * Helper for constructing objects via reflection.
 */
public class FactoryHelper {

	/**
	 * Create an object using its default (no arg) constructor.
	 */
	public static Object create(String className) throws CreateObjectException {
		return create(className, null, null);

	}

	/**
	 * Create an object using a Constructor that takes arguments.
	 */
	public static Object create(String className, Class<?>[] argTypes, Object[] argValues)
			throws CreateObjectException {

		try {
			
			Class<?> clz = Class.forName(className);
			if (argTypes == null) {
				return clz.newInstance();

			} else {
				Constructor<?> c = clz.getConstructor(argTypes);
				return c.newInstance(argValues);
			}
		} catch (Exception e) {
			throw new CreateObjectException(e);
		}
	}
}
