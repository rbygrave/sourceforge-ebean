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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Contains the various ElMatcher implementations.
 */
class ElMatchBuilder {

	/**
	 * Case insensitive equals.
	 */
	static class RegularExpr<T> implements ElMatcher<T> {
		
		final ElPropertyValue elGetValue;
		final String value;
		final Pattern pattern;
		
		RegularExpr(ElPropertyValue elGetValue, String value, int options){
			this.elGetValue = elGetValue;
			this.value = value;
			this.pattern = Pattern.compile(value, options);
		}
		
		public boolean isMatch(T bean) {
			String v = (String)elGetValue.elGetValue(bean);
			return pattern.matcher(v).matches();
		}
	}
	
	/**
	 * Case insensitive equals.
	 */
	static abstract class BaseString<T> implements ElMatcher<T> {
		
		final ElPropertyValue elGetValue;
		final String value;
		
		public BaseString(ElPropertyValue elGetValue, String value){
			this.elGetValue = elGetValue;
			this.value = value;
		}
		
		public abstract boolean isMatch(T bean);
	}
	
	static class Ieq<T> extends BaseString<T> {
		Ieq(ElPropertyValue elGetValue, String value) {
			super(elGetValue, value);
		}

		public boolean isMatch(T bean) {
			String v = (String)elGetValue.elGetValue(bean);
			return value.equalsIgnoreCase(v);
		}
	}

	/**
	 * Case insensitive starts with matcher.
	 */
	static class IStartsWith<T> implements ElMatcher<T> {

		final ElPropertyValue elGetValue;
		final CharMatch charMatch;
		
		IStartsWith(ElPropertyValue elGetValue, String value) {
			this.elGetValue = elGetValue;
			this.charMatch = new CharMatch(value);
		}
		
		public boolean isMatch(T bean) {
			
			String v = (String)elGetValue.elGetValue(bean);
			return charMatch.startsWith(v);
		}
	}

	/**
	 * Case insensitive ends with matcher.
	 */
	static class IEndsWith<T> implements ElMatcher<T> {

		final ElPropertyValue elGetValue;
		final CharMatch charMatch;
		
		IEndsWith(ElPropertyValue elGetValue, String value) {
			this.elGetValue = elGetValue;
			this.charMatch = new CharMatch(value);
		}
		
		public boolean isMatch(T bean) {
			
			String v = (String)elGetValue.elGetValue(bean);
			return charMatch.endsWith(v);
		}
	}

	static class StartsWith<T> extends BaseString<T> {
		StartsWith(ElPropertyValue elGetValue, String value) {
			super(elGetValue, value);
		}

		public boolean isMatch(T bean) {
			String v = (String)elGetValue.elGetValue(bean);
			return value.startsWith(v);
		}
	}
	
	static class EndsWith<T> extends BaseString<T> {
		EndsWith(ElPropertyValue elGetValue, String value) {
			super(elGetValue, value);
		}

		public boolean isMatch(T bean) {
			String v = (String)elGetValue.elGetValue(bean);
			return value.endsWith(v);
		}
	}	
	
	static class IsNull<T> implements ElMatcher<T> {
		
		final ElPropertyValue elGetValue;
		
		public IsNull(ElPropertyValue elGetValue){
			this.elGetValue = elGetValue;
		}
		
		public boolean isMatch(T bean) {
			return (null == elGetValue.elGetValue(bean));
		}
	}

	static class IsNotNull<T> implements ElMatcher<T> {
		
		final ElPropertyValue elGetValue;
		
		public IsNotNull(ElPropertyValue elGetValue){
			this.elGetValue = elGetValue;
		}
		
		public boolean isMatch(T bean) {
			return (null != elGetValue.elGetValue(bean));
		}
	}

	static abstract class Base<T> implements ElMatcher<T> {
		
		final Object filterValue;
		
		final ElComparator<T> comparator;
		
		public Base(Object filterValue, ElComparator<T> comparator){
			this.filterValue = filterValue;
			this.comparator = comparator;
		}
		
		public abstract boolean isMatch(T value);
	}

	static class InSet<T> implements ElMatcher<T> {

		final Set<?> set;
		final ElPropertyValue elGetValue;
		
		@SuppressWarnings("unchecked")
		public InSet(Set<?> set, ElPropertyValue elGetValue){
			this.set = new HashSet(set);
			this.elGetValue = elGetValue;
		}

		public boolean isMatch(T bean) {

			Object value = elGetValue.elGetValue(bean);
			if (value == null){
				return false;
			}
			
			return set.contains(value);
		}

	}
	/**
	 * Equal To.
	 */
	static class Eq<T> extends Base<T> {
		
		public Eq(Object filterValue, ElComparator<T> comparator){
			super(filterValue, comparator);
		}
		
		public boolean isMatch(T value) {
			return comparator.compareValue(filterValue, value) == 0;
		}
	}
	
	/**
	 * Not Equal To.
	 */
	static class Ne<T> extends Base<T> {
		
		public Ne(Object filterValue, ElComparator<T> comparator){
			super(filterValue, comparator);
		}
		
		public boolean isMatch(T value) {
			return comparator.compareValue(filterValue, value) != 0;
		}
	}
	
	/**
	 * Between.
	 */
	static class Between<T> implements ElMatcher<T> {
		
		final Object min; 
		final Object max;
		final ElComparator<T> comparator;
		
		Between(Object min, Object max, ElComparator<T> comparator){
			this.min = min;
			this.max = max;
			this.comparator = comparator;
		}
		
		public boolean isMatch(T value) {

			return (comparator.compareValue(min, value) <= 0 
					&& comparator.compareValue(max, value) >= 0);
		}
	}
	
	/**
	 * Greater Than.
	 */
	static class Gt<T> extends Base<T> {
		Gt(Object filterValue, ElComparator<T> comparator){
			super(filterValue, comparator);
		}
		
		public boolean isMatch(T value) {
			return comparator.compareValue(filterValue, value) == -1;
		}
	}
	
	/**
	 * Greater Than or Equal To.
	 */
	static class Ge<T> extends Base<T> {
		Ge(Object filterValue, ElComparator<T> comparator){
			super(filterValue, comparator);
		}
		
		public boolean isMatch(T value) {
			return comparator.compareValue(filterValue, value) >= 0;
		}
	}
	
	/**
	 * Less Than or Equal To.
	 */
	static class Le<T> extends Base<T> {
		Le(Object filterValue, ElComparator<T> comparator){
			super(filterValue, comparator);
		}
		
		public boolean isMatch(T value) {
			return comparator.compareValue(filterValue, value) <= 0;
		}
	}
	
	/**
	 * Less Than.
	 */
	static class Lt<T> extends Base<T> {
		Lt(Object filterValue, ElComparator<T> comparator){
			super(filterValue, comparator);
		}
		
		public boolean isMatch(T value) {
			return comparator.compareValue(filterValue, value) == 1;
		}
	}
}
