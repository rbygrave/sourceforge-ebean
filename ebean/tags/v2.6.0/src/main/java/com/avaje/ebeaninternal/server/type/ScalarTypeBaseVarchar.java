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
package com.avaje.ebeaninternal.server.type;

import java.sql.SQLException;
import java.sql.Types;

import com.avaje.ebean.text.TextException;
import com.avaje.ebean.text.json.JsonValueAdapter;

/**
 * Base ScalarType for types which converts to and from a VARCHAR database column.
 */
public abstract class ScalarTypeBaseVarchar<T> extends ScalarTypeBase<T> {

	public ScalarTypeBaseVarchar(Class<T> type) {
		super(type, false, Types.VARCHAR);
	}

    public abstract T parse(String value);
    
    public abstract T convertFromDbString(String dbValue);
    
    public abstract String convertToDbString(T beanValue);
    
	public void bind(DataBind b, T value) throws SQLException {
	    
	    String s = convertToDbString(value);
	    
		if (s == null){
			b.setNull(Types.VARCHAR);
		} else {
			b.setString(s);
		}
	}

	public T read(DataReader dataReader) throws SQLException {
		String s = dataReader.getString();
		if (s == null){
			return null;
		} else {
		    return convertFromDbString(s);
		}
	}
	
	@SuppressWarnings("unchecked")
    public T toBeanType(Object value) {
	    if (value == null){
	        return null;
	    }
		if (value instanceof String){
		    return parse((String)value);
		}
		return (T)value;
	}
	
	public Object toJdbcType(Object value){
	    if (value instanceof String){
	        return parse((String)value);
	    }
	    return value;
	}
	
	public T parseDateTime(long systemTimeMillis) {
		throw new TextException("Not Supported");
	}
	
	public boolean isDateTimeCapable() {
		return false;
	}
	
    public T jsonFromString(String value, JsonValueAdapter ctx) {
        return parse(value);
    }

    public String toJsonString(Object value, JsonValueAdapter ctx) {
        String s = format(value);
        return EscapeJson.escapeQuote(s);
    }
}
