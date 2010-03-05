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
import java.sql.SQLException;
import java.sql.Types;

import org.joda.time.LocalDate;

import com.avaje.ebean.server.core.BasicTypeConverter;

/**
 * ScalarType for Joda LocalDate. This maps to a JDBC Date.
 */
public class ScalarTypeJodaLocalDate extends ScalarTypeBase<LocalDate> {

	public ScalarTypeJodaLocalDate() {
		super(LocalDate.class, false, Types.DATE);
	}
	
	public void bind(DataBind b, LocalDate value) throws SQLException {
		if (value == null){
			b.setNull(Types.DATE);
		} else {
			java.sql.Date d = new java.sql.Date(value.toDateMidnight().getMillis());
			b.setDate(d);
		}
	}

	public LocalDate read(DataReader dataReader) throws SQLException {
		
		java.sql.Date d = dataReader.getDate();
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

	public LocalDate toBeanType(Object value) {
		if (value instanceof java.util.Date){
			return new LocalDate(((java.util.Date)value).getTime());
		}
		return (LocalDate)value;
	}

    public String format(LocalDate v) {
        Date sqlDate = new Date(v.toDateTimeAtStartOfDay().getMillis());
        return sqlDate.toString();
    }

	public LocalDate parse(String value) {
		Date ts = Date.valueOf(value);
		return new LocalDate(ts.getTime());
	}
	
	public LocalDate parseDateTime(long systemTimeMillis) {
		return new LocalDate(systemTimeMillis);
	}
	
	public boolean isDateTimeCapable() {
		return true;
	}

}
