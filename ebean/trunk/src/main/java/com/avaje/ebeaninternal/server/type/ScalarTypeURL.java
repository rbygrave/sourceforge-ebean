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

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Types;

import com.avaje.ebean.text.TextException;

/**
 * ScalarType for java.net.URL which converts to and from a VARCHAR database column.
 */
public class ScalarTypeURL extends ScalarTypeBase<URL> {

	public ScalarTypeURL() {
		super(URL.class, false, Types.VARCHAR);
	}
	
	public void bind(DataBind b, URL value) throws SQLException {
		if (value == null){
			b.setNull(Types.VARCHAR);
		} else {
			b.setString(value.toString());
		}
	}

	public URL read(DataReader dataReader) throws SQLException {
		String str = dataReader.getString();
		if (str == null){
			return null;
		} else {
			try {
				return new URL(str);
			} catch (MalformedURLException e) {
				throw new SQLException("Error with URL ["+str+"] "+e);
			}
		}
	}
	
	public URL toBeanType(Object value) {
		if (value instanceof String){
			try {
				return new URL((String)value);
			} catch (MalformedURLException e) {
				throw new RuntimeException("Error with URL ["+value+"] "+e);
			}
		}
		return (URL)value;
	}
	
	public Object toJdbcType(Object value) {
		return value.toString();
	}

	public String format(URL v) {
        return v.toString();
    }

    public URL parse(String value) {
		try {
			return new URL(value);
		} catch (MalformedURLException e) {
			throw new TextException(e);
		}
	}
	
	public URL parseDateTime(long systemTimeMillis) {
		throw new TextException("Not Supported");
	}
	
	public boolean isDateTimeCapable() {
		return false;
	}
	
}
