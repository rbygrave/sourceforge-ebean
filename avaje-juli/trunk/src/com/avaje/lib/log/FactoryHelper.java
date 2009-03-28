package com.avaje.lib.log;

import java.lang.reflect.Constructor;

/**
 * Helper for constructing objects via reflection.
 */
public class FactoryHelper {

	/**
	 * Create an object using its default (no arg) constructor.
	 */
	public static Object create(String className) {
		return create(className, null, null);
	}

	/**
	 * Create an object using a Constructor that takes arguments.
	 */
	public static Object create(String className, Class<?>[] argTypes, Object[] argValues){

		try {
			
			Class<?> clz = Class.forName(className);
			if (argTypes == null) {
				return clz.newInstance();

			} else {
				Constructor<?> c = clz.getConstructor(argTypes);
				return c.newInstance(argValues);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Find the constructor return null if one can not be found that matches the arguments.
	 */
	public static <T> Constructor<T> findConstructor(Class<T> type, Class<?>... args){
		try {
			return type.getConstructor(args);
		} catch (SecurityException e) {
			return null;
			
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
}
