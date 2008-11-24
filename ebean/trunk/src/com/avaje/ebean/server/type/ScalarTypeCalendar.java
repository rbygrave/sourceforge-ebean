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

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

import com.avaje.ebean.util.BasicTypeConverter;

/**
 * ScalarType for java.util.Calendar.
 */
public class ScalarTypeCalendar extends ScalarTypeBase {
	
	public ScalarTypeCalendar(int jdbcType) {
		super(Calendar.class, false, jdbcType);
	}
	
	public Object read(ResultSet rset, int index) throws SQLException {
		Timestamp timestamp = rset.getTimestamp(index);
		if (timestamp == null){
			return null;
		} else {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(timestamp.getTime());
			return cal;
		}
	}
	
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null){
			pstmt.setNull(index, Types.TIMESTAMP);
		} else {
			Calendar date = (Calendar)value;
			if (jdbcType == Types.TIMESTAMP){
				Timestamp timestamp = new Timestamp(date.getTimeInMillis());
				pstmt.setTimestamp(index, timestamp);
			} else {
				Date d = new Date(date.getTimeInMillis());
				pstmt.setDate(index, d);	
			}
		}
	}
	
	public Object toJdbcType(Object value) {
		return BasicTypeConverter.convert(value, jdbcType);
	}

	public Object toBeanType(Object value) {
		return BasicTypeConverter.toCalendar(value);
	}

}
