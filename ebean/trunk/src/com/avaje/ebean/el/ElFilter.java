/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebean.el;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.avaje.ebean.Filter;
import com.avaje.ebean.server.deploy.BeanDescriptor;

/**
 * Default implementation of the Filter interface.
 */
public final class ElFilter<T> implements Filter<T>  {

	final BeanDescriptor<T> beanDescriptor;
	
	ArrayList<ElMatcher<T>> matches = new ArrayList<ElMatcher<T>>();

	int maxRows;
	
	String sortByClause;
	
	public ElFilter(BeanDescriptor<T> beanDescriptor) {
		this.beanDescriptor = beanDescriptor;
	}

	private Object convertValue(String propertyName, Object value) {
		// convert type of value to match expected type
		ElGetValue elGetValue = beanDescriptor.getElGetValue(propertyName);
		return elGetValue.elConvertType(value);
	}
	
	private ElComparator<T> getElComparator(String propertyName) {
		
		return beanDescriptor.getElComparator(propertyName);
	}
	
	private ElGetValue getElGetValue(String propertyName) {
		
		return beanDescriptor.getElGetValue(propertyName);
	}
	
	public Filter<T> sort(String sortByClause) {
		this.sortByClause = sortByClause;
		return this;
	}
	
	protected boolean isMatch(T bean) {
		for (int i = 0; i < matches.size(); i++) {
			ElMatcher<T> matcher = matches.get(i);
			if (!matcher.isMatch(bean)){
				return false;
			}
		}
		return true;
	}
	

	public Filter<T> in(String propertyName, Set<?> matchingValues) {
		
		ElGetValue elGetValue = getElGetValue(propertyName);
		
		matches.add(new ElMatchBuilder.InSet<T>(matchingValues, elGetValue));
		return this;
	}

	public Filter<T> eq(String propertyName, Object value) {
		
		value = convertValue(propertyName, value);		
		ElComparator<T> comparator = getElComparator(propertyName);

		matches.add(new ElMatchBuilder.Eq<T>(value, comparator));
		return this;
	}
	

	public Filter<T> ne(String propertyName, Object value) {

		value = convertValue(propertyName, value);		
		ElComparator<T> comparator = getElComparator(propertyName);
		
		matches.add(new ElMatchBuilder.Ne<T>(value, comparator));
		return this;
	}
	
	public Filter<T> between(String propertyName, Object min, Object max) {

		ElGetValue elGetValue = getElGetValue(propertyName);
		min = elGetValue.elConvertType(min);
		max = elGetValue.elConvertType(max);
		
		ElComparator<T> elComparator = getElComparator(propertyName);
		
		matches.add(new ElMatchBuilder.Between<T>(min, max, elComparator));
		return this;
	}


	public Filter<T> gt(String propertyName, Object value) {
		
		value = convertValue(propertyName, value);		
		ElComparator<T> comparator = getElComparator(propertyName);
		
		matches.add(new ElMatchBuilder.Gt<T>(value, comparator));
		return this;
	}
	
	public Filter<T> ge(String propertyName, Object value) {
	
		value = convertValue(propertyName, value);		
		ElComparator<T> comparator = getElComparator(propertyName);
		
		matches.add(new ElMatchBuilder.Ge<T>(value, comparator));
		return this;
	}

	public Filter<T> ieq(String propertyName, String value) {

		ElGetValue elGetValue = getElGetValue(propertyName);
		
		matches.add(new ElMatchBuilder.Ieq<T>(elGetValue, value));
		return this;
	}


	public Filter<T> isNotNull(String propertyName) {

		ElGetValue elGetValue = getElGetValue(propertyName);
		
		matches.add(new ElMatchBuilder.IsNotNull<T>(elGetValue));
		return this;
	}


	public Filter<T> isNull(String propertyName) {
		
		ElGetValue elGetValue = getElGetValue(propertyName);
		
		matches.add(new ElMatchBuilder.IsNull<T>(elGetValue));
		return this;
	}

	
	public Filter<T> le(String propertyName, Object value) {

		value = convertValue(propertyName, value);		
		ElComparator<T> comparator = getElComparator(propertyName);
		
		matches.add(new ElMatchBuilder.Le<T>(value, comparator));
		return this;
	}

	
	public Filter<T> lt(String propertyName, Object value) {

		value = convertValue(propertyName, value);		
		ElComparator<T> comparator = getElComparator(propertyName);
		
		matches.add(new ElMatchBuilder.Lt<T>(value, comparator));
		return this;
	}
	
	
	public Filter<T> regex(String propertyName, String regEx) {
		return regex(propertyName, regEx, 0);
	}
	
	public Filter<T> regex(String propertyName, String regEx, int options) {
		
		ElGetValue elGetValue = getElGetValue(propertyName);
		
		matches.add(new ElMatchBuilder.RegularExpr<T>(elGetValue, regEx, options));
		return this;
	}

	public Filter<T> contains(String propertyName, String value) {
		
		String quote = ".*"+Pattern.quote(value)+".*";
		
		ElGetValue elGetValue = getElGetValue(propertyName);
		matches.add(new ElMatchBuilder.RegularExpr<T>(elGetValue, quote, 0));
		return this;
	}

	public Filter<T> icontains(String propertyName, String value) {
		
		String quote = ".*"+Pattern.quote(value)+".*";
		
		ElGetValue elGetValue = getElGetValue(propertyName);
		matches.add(new ElMatchBuilder.RegularExpr<T>(elGetValue, quote, Pattern.CASE_INSENSITIVE));
		return this;
	}

	
	public Filter<T> endsWith(String propertyName, String value) {

		ElGetValue elGetValue = getElGetValue(propertyName);
		matches.add(new ElMatchBuilder.EndsWith<T>(elGetValue, value));
		return this;
	}

	public Filter<T> startsWith(String propertyName, String value) {
		
		ElGetValue elGetValue = getElGetValue(propertyName);		
		matches.add(new ElMatchBuilder.StartsWith<T>(elGetValue, value));
		return this;
	}
	
	public Filter<T> iendsWith(String propertyName, String value) {

		ElGetValue elGetValue = getElGetValue(propertyName);		
		matches.add(new ElMatchBuilder.IEndsWith<T>(elGetValue, value));
		return this;
	}

	public Filter<T> istartsWith(String propertyName, String value) {

		ElGetValue elGetValue = getElGetValue(propertyName);		
		matches.add(new ElMatchBuilder.IStartsWith<T>(elGetValue, value));
		return this;
	}

	public Filter<T> maxRows(int maxRows) {
		this.maxRows = maxRows;
		return this;
	}

	public List<T> filter(List<T> list) {

		if (sortByClause != null){
			beanDescriptor.sort(list, sortByClause);
		}
		
		ArrayList<T> filterList = new ArrayList<T>();

		for (int i = 0; i < list.size(); i++) {
			T t = list.get(i);
			if (isMatch(t)) {
				filterList.add(t);
				if (maxRows > 0 && filterList.size() >= maxRows){
					break;
				}
			}
		}

		return filterList;
	}
	
}
