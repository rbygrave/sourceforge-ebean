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
import java.sql.Time;
import java.sql.Types;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import com.avaje.ebean.server.core.BasicTypeConverter;

/**
 * ScalarType for Joda LocalTime. This maps to a JDBC Time.
 */
public class ScalarTypeJodaLocalTime extends ScalarTypeBase {

	public ScalarTypeJodaLocalTime() {
		super(LocalTime.class, false, Types.TIME);
	}
	
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null){
			pstmt.setNull(index, Types.TIME);
		} else {
			LocalTime lt = (LocalTime)value;
			Time sqlTime = new Time(lt.getMillisOfDay());
			pstmt.setTime(index, sqlTime);
		}
	}

	public Object read(ResultSet rset, int index) throws SQLException {
		
		Time sqlTime = rset.getTime(index);
		if (sqlTime == null){
			return null;
		} else {
			return new LocalTime(sqlTime, DateTimeZone.UTC);
		}
	}
	
	public Object toJdbcType(Object value) {
		if (value instanceof LocalTime){
			return new Time(((LocalTime)value).getMillisOfDay());
		}
		return BasicTypeConverter.toTime(value);
	}

	public Object toBeanType(Object value) {
		if (value instanceof java.util.Date){
			return new LocalTime(value, DateTimeZone.UTC);
		}
		return value;
	}

	public Object parse(String value) {
		Time ts = Time.valueOf(value);
		return new LocalTime(ts.getTime());
	}
	
	public Object parseDateTime(long systemTimeMillis) {
		return new LocalTime(systemTimeMillis);
	}

	public boolean isDateTimeCapable() {
		return true;
	}

}
