package com.avaje.ebean.el;

import java.util.Comparator;

/**
 * Comparator based on a ElGetValue.
 */
public final class ElComparator<T> implements Comparator<T> {

	private final ElGetValue elGetValue;
	
	private final int nullOrder;
	
	private final int asc;
	
	public ElComparator(ElGetValue elGetValue, boolean ascending, boolean nullsHigh) {
		this.elGetValue = elGetValue;
		this.asc = ascending ? 1 : -1;
		this.nullOrder = asc * (nullsHigh ? 1 : -1);
	}
	
	@SuppressWarnings("unchecked")
	public int compare(T o1, T o2) {
		
		Object val1 = elGetValue.elGetValue(o1);
		Object val2 = elGetValue.elGetValue(o2);
		
		if (val1 == null){
			return val2 == null ? 0 : nullOrder;
		}
		if (val2 == null){
			return -1 * nullOrder;
		} 
		Comparable c = (Comparable)val1;
		return asc * c.compareTo(val2);		
	}

	
}
