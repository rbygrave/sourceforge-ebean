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

/**
 * ScalarType for byte[].
 */
public class ScalarTypeByteArray extends ScalarTypeBase {

	public ScalarTypeByteArray() {
		super(byte[].class, true, Types.BINARY);
	}
	
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null){
			pstmt.setNull(index, Types.BINARY);
		} else {
			if (value instanceof byte[]){
				pstmt.setBytes(index, (byte[])value);					
			} else {
				pstmt.setByte(index, (Byte)value);				
			}
		}
	}

	public Object read(ResultSet rset, int index) throws SQLException {
		byte[] b = rset.getBytes(index);
		if (rset.wasNull()){
			return null;
		} else {
			return b;
		}
	}
	
	public Object toJdbcType(Object value) {
		return value;
	}

	public Object toBeanType(Object value) {
		return value;
	}

}
