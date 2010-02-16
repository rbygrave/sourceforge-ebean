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

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.sql.Types;

import com.avaje.ebean.text.TextException;

/**
 * ScalarType for java.net.URI which converts to and from a VARCHAR database column.
 */
public class ScalarTypeURI extends ScalarTypeBase<URI> {

	public ScalarTypeURI() {
		super(URI.class, false, Types.VARCHAR);
	}
	
	public void bind(DataBind b, URI value) throws SQLException {
		if (value == null){
			b.setNull(Types.VARCHAR);
		} else {
			b.setString(value.toString());
		}
	}

	public URI read(DataReader dataReader) throws SQLException {
		String str = dataReader.getString();
		if (str == null){
			return null;
		} else {
			try {
				return new URI(str);
			} catch (URISyntaxException e) {
				throw new SQLException("Error with URI ["+str+"] "+e);
			}
		}
	}
	
	public URI toBeanType(Object value) {
		if (value instanceof String){
			try {
				return new URI((String)value);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Error with URI ["+value+"] "+e);
			}
		}
		return (URI)value;
	}
	
	public Object toJdbcType(Object value) {
		return value.toString();
	}
	
	public String format(URI v) {
        return v.toString();
    }

    public URI parse(String value) {
		try {
			return new URI(value);
		} catch (URISyntaxException e) {
			throw new TextException("Error with URI ["+value+"] ", e);
		}
	}
	
	public URI parseDateTime(long systemTimeMillis) {
		throw new TextException("Not Supported");
	}

	public boolean isDateTimeCapable() {
		return false;
	}
}
