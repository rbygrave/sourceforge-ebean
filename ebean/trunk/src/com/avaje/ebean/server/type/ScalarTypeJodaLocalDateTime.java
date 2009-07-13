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
import java.sql.Timestamp;
import java.sql.Types;

import org.joda.time.LocalDateTime;

import com.avaje.ebean.server.core.BasicTypeConverter;

/**
 * ScalarType for Joda LocalDateTime. This maps to a JDBC Timestamp.
 */
public class ScalarTypeJodaLocalDateTime extends ScalarTypeBase {

	public ScalarTypeJodaLocalDateTime() {
		super(LocalDateTime.class, false, Types.TIMESTAMP);
	}
	
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null){
			pstmt.setNull(index, Types.TIMESTAMP);
		} else {
			LocalDateTime dateTime = (LocalDateTime)value;
			Timestamp ts = new Timestamp(dateTime.toDateTime().getMillis());
			pstmt.setTimestamp(index, ts);
		}
	}

	public Object read(ResultSet rset, int index) throws SQLException {
		
		Timestamp ts = rset.getTimestamp(index);
		if (ts == null){
			return null;
		} else {
			return new LocalDateTime(ts.getTime());
		}
	}
	
	public Object toJdbcType(Object value) {
		if (value instanceof LocalDateTime){
			return new Timestamp(((LocalDateTime)value).toDateTime().getMillis());
		}
		return BasicTypeConverter.toTimestamp(value);
	}

	public Object toBeanType(Object value) {
		if (value instanceof java.util.Date){
			return new LocalDateTime(((java.util.Date)value).getTime());
		}
		return value;
	}

}
