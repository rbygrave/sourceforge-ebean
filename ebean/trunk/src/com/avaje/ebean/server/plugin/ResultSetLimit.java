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
package com.avaje.ebean.server.plugin;

/**
 * The options for limiting a query result set.
 * <p>
 * This can be done 3 ways.
 * <ul>
 * <li>Using the ROW_NUMBER() database function</li>
 * <li>Using the LIMIT OFFSET clause</li>
 * <li>By JDBC row navigation skipping the unwanted rows up to firstRow</li>
 * </ul>
 * </p>
 */
public enum ResultSetLimit {

	/**
	 * Using the ROW_NUMBER() database function.
	 */
	RowNumber(false),
	
	/**
	 * Using a LIMIT OFFSET clause.
	 */
	LimitOffset(false),
	
	/**
	 * Using JDBC row navigation.
	 */
	JdbcRowNavigation(true);
	
	private final boolean useJdbcResultSetLimit;
	
	private ResultSetLimit(boolean useJdbcResultSetLimit){
		this.useJdbcResultSetLimit = useJdbcResultSetLimit;
	}
	
	/**
	 * Return true if you can use JDBC resultSet limits.
	 */
	public boolean useJdbcResultSetLimit() {
		return useJdbcResultSetLimit;
	}


    /**
     * If the String contains "rownumber" return RowNumber.
     * If the String contains "limit" return LimitOffset.
     * Return JdbcRowNavigation.
     */
    public static ResultSetLimit parse(String s){
        s = s.toLowerCase();
        if (s.indexOf("rownumber")>-1){
            return RowNumber;
        }
        if (s.indexOf("limit")>-1){
            return LimitOffset;
        }
        
        return JdbcRowNavigation;
    }
}
