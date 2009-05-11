package com.avaje.ebean.el;

import java.util.Comparator;

/**
 * Comparator based on multiple ordered comparators.
 * <p>
 * eg.  "name, orderDate desc, id"
 * </p>
 */
public final class ElCompoundComparator<T> implements Comparator<T> {

	private final Comparator<T>[] array;
	
	public ElCompoundComparator(Comparator<T>[] array) {
		this.array = array;
	}
	
	public int compare(T o1, T o2) {
		
		for (int i = 0; i < array.length; i++) {
			int ret = array[i].compare(o1, o2);
			if (ret != 0){
				return ret;
			}
		}
		
		return 0;
	}

	
	
}
