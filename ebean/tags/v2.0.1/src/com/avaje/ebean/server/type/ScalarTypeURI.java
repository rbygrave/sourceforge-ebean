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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * ScalarType for java.net.URI which converts to and from a VARCHAR database column.
 */
public class ScalarTypeURI extends ScalarTypeBase {

	public ScalarTypeURI() {
		super(URI.class, false, Types.VARCHAR);
	}
	
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null){
			pstmt.setNull(index, Types.VARCHAR);
		} else {
			URI uri = (URI)value;
			pstmt.setString(index, uri.toString());
		}
	}

	public Object read(ResultSet rset, int index) throws SQLException {
		String str = rset.getString(index);
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
	
	public Object toBeanType(Object value) {
		if (value instanceof String){
			try {
				return new URI((String)value);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Error with URI ["+value+"] "+e);
			}
		}
		return value;
	}
	
	public Object toJdbcType(Object value) {
		return value.toString();
	}
}
