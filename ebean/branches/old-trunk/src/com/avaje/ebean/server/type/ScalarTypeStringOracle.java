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
package com.avaje.ebean.server.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.avaje.ebean.server.core.BasicTypeConverter;

/**
 * ScalarType for String where empty strings are treated as nulls.
 */
public final class ScalarTypeStringOracle extends ScalarTypeBase {

	public ScalarTypeStringOracle() {
		super(String.class, true, Types.VARCHAR);
	}
	
	/**
	 * Return the value as a string also converting empty strings to null.
	 */
	private String convertEmptyString(Object value){
		if (value == null){
			return null;
		}
		String s = (String)value;
		if (s.length()==0){
			// empty string treated as null
			return null;
		}
		return s;
	}
	
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null){
			pstmt.setNull(index, Types.VARCHAR);
		} else {
			String s = convertEmptyString(value);
			if (s == null){
				pstmt.setNull(index, Types.VARCHAR);				
			} else {
				pstmt.setString(index, (String)value);				
			}
		}
	}

	public Object read(ResultSet rset, int index) throws SQLException {
		
		return rset.getString(index);
	}
	
	public Object toJdbcType(Object value) {
		return convertEmptyString(BasicTypeConverter.toString(value));
	}

	public Object toBeanType(Object value) {
		return BasicTypeConverter.toString(value);
	}

	/**
	 * Return true if the value is null or an empty string.
	 */
	public boolean isDbNull(Object value) {
		return convertEmptyString(value) == null;
	}
	
	/**
	 * Returns the value converting empty string into null.
	 */
	public Object getDbNullValue(Object value) {
		return convertEmptyString(value);
	}

}
