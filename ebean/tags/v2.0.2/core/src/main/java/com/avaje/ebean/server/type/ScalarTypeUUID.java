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
import java.util.UUID;

import com.avaje.ebean.server.core.BasicTypeConverter;

/**
 * ScalarType for java.util.UUID which converts to and from a VARCHAR database column.
 */
public class ScalarTypeUUID extends ScalarTypeBase {

	public ScalarTypeUUID() {
		super(UUID.class, false, Types.VARCHAR);
	}
	
		
	@Override
	public int getLength() {
		return 40;
	}

	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null){
			pstmt.setNull(index, Types.VARCHAR);
		} else {
			UUID uuid = (UUID)value;
			pstmt.setString(index, uuid.toString());
		}
	}

	public Object read(ResultSet rset, int index) throws SQLException {
		String str = rset.getString(index);
		if (str == null){
			return null;
		} else {
			return UUID.fromString(str);
		}
	}
	
	public Object toBeanType(Object value) {
		return BasicTypeConverter.toUUID(value);
	}
	
	public Object toJdbcType(Object value) {
		return BasicTypeConverter.convert(value, jdbcType);
	}
}
