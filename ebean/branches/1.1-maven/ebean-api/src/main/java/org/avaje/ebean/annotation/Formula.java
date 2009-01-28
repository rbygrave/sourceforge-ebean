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
package org.avaje.ebean.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Assign to a property to be based on a SQL formula.
 * <p>
 * This is typically a SQL Literal value, SQL case statement, SQL function or
 * similar.
 * </p>
 * <p>
 * Any property based on a formula becomes a read only property.
 * </p>
 */
@Target( { ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Formula {

	/**
	 * The SQL to be used in the SELECT part of the SQL to populate a property.
	 */
	String select();

	/**
	 * OPTIONAL - the SQL to be used in the JOIN part of the SQL to support the
	 * formula.
	 * <p>
	 * This is commonly used to join a 'dynamic view' to support aggregation
	 * such as count, sum etc.
	 * </p>
	 * <p>
	 * The join string should start with either "left outer join" or "join".
	 * </p>
	 * 
	 * <p>
	 * You will almost certainly use the "${ta}" as a place holder for the table
	 * alias of the table you are joining back to (the "base table" of the
	 * entity bean).
	 * </p>
	 * <p>
	 * The example below is used to support a total count of topics created by a user.
	 * </p>
	 * <pre class="code">
	 * join (select user_id, count(*) as topic_count from f_topic group by user_id) as _tc on _tc.user_id = ${ta}.id
	 * </pre>
	 */
	String join() default "";

};
