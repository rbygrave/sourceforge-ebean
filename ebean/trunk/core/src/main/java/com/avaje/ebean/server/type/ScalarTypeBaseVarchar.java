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

import com.avaje.ebean.text.TextException;

/**
 * Base ScalarType for types which converts to and from a VARCHAR database column.
 */
public abstract class ScalarTypeBaseVarchar extends ScalarTypeBase {

	public ScalarTypeBaseVarchar(Class<?> type) {
		super(type, false, Types.VARCHAR);
	}

    public abstract Object parse(String value);
    
    public abstract Object convertFromDbString(String dbValue);
    
    public abstract String convertToDbString(Object beanValue);
    
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
	    
	    String s = convertToDbString(value);
	    
		if (s == null){
			pstmt.setNull(index, Types.VARCHAR);
		} else {
			pstmt.setString(index, s);
		}
	}

	public Object read(ResultSet rset, int index) throws SQLException {
		String s = rset.getString(index);
		if (s == null){
			return null;
		} else {
		    return convertFromDbString(s);
		}
	}
	
	public Object toBeanType(Object value) {
		if (value instanceof String){
		    return parse((String)value);
		}
		return value;
	}
	
	public Object toJdbcType(Object value){
	    if (value instanceof String){
	        return parse((String)value);
	    }
	    return value;
	}
	
	public Object parseDateTime(long systemTimeMillis) {
		throw new TextException("Not Supported");
	}
	
	public boolean isDateTimeCapable() {
		return false;
	}
	
}
