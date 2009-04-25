package com.avaje.ebean.util;

import java.math.BigDecimal;
import java.net.URL;

public class ValueUtil {

	/**
	 * Helper method to check if two objects are equal.
	 */
	@SuppressWarnings("unchecked")
	public static boolean areEqual(Object obj1, Object obj2) {
		if (obj1 == null) {
			return (obj2 == null);
		}
		if (obj2 == null) {
			return false;
		}
		if (obj1 == obj2) {
			return true;
		}
		if (obj1 instanceof BigDecimal) {
			// Use comparable for BigDecimal as equals
			// uses scale in comparison...
			if (obj2 instanceof BigDecimal) {
				Comparable<Object> com1 = (Comparable<Object>) obj1;
				return (com1.compareTo(obj2) == 0);

			} else {
				return false;
			}

		} 
		if (obj1 instanceof URL){
			// use the string format to determine if dirty
			return obj1.toString().equals(obj2.toString());
		}
		return obj1.equals(obj2);
	}
}
