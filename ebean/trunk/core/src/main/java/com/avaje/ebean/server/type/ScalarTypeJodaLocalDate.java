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

import org.joda.time.LocalDate;

import com.avaje.ebean.server.core.BasicTypeConverter;

/**
 * ScalarType for Joda LocalDate. This maps to a JDBC Date.
 */
public class ScalarTypeJodaLocalDate extends ScalarTypeBase {

	public ScalarTypeJodaLocalDate() {
		super(LocalDate.class, false, Types.DATE);
	}
	
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null){
			pstmt.setNull(index, Types.DATE);
		} else {
			LocalDate ld= (LocalDate)value;
			java.sql.Date d = new java.sql.Date(ld.toDateMidnight().getMillis());
			pstmt.setDate(index, d);
		}
	}

	public Object read(ResultSet rset, int index) throws SQLException {
		
		java.sql.Date d = rset.getDate(index);
		if (d == null){
			return null;
		} else {
			return new LocalDate(d.getTime());
		}
	}
	
	public Object toJdbcType(Object value) {
		if (value instanceof LocalDate){
			return new java.sql.Date(((LocalDate)value).toDateMidnight().getMillis());
		}
		return BasicTypeConverter.toDate(value);
	}

	public Object toBeanType(Object value) {
		if (value instanceof java.util.Date){
			return new LocalDate(((java.util.Date)value).getTime());
		}
		return value;
	}

	public Object parse(String value) {
		Timestamp ts = Timestamp.valueOf(value);
		return new LocalDate(ts.getTime());
	}
	
	public Object parseDateTime(long systemTimeMillis) {
		return new LocalDate(systemTimeMillis);
	}
	
	public boolean isDateTimeCapable() {
		return true;
	}

}
