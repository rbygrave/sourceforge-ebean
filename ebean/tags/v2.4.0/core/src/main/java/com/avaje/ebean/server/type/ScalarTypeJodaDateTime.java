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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import org.joda.time.DateTime;

import com.avaje.ebean.server.core.BasicTypeConverter;

/**
 * ScalarType for Joda DateTime. This maps to a JDBC Timestamp.
 */
public class ScalarTypeJodaDateTime extends ScalarTypeBase<DateTime> {

	public ScalarTypeJodaDateTime() {
		super(DateTime.class, false, Types.TIMESTAMP);
	}
	
	public void bind(DataBind b, DateTime value) throws SQLException {
		if (value == null){
			b.setNull(Types.TIMESTAMP);
		} else {
			Timestamp ts = new Timestamp(value.getMillis());
			b.setTimestamp(ts);
		}
	}

	public DateTime read(DataReader dataReader) throws SQLException {
		
		Timestamp ts = dataReader.getTimestamp();
		if (ts == null){
			return null;
		} else {
			return new DateTime(ts.getTime());
		}
	}
	
	public Object toJdbcType(Object value) {
		if (value instanceof DateTime){
			return new Timestamp(((DateTime)value).getMillis());
		}
		return BasicTypeConverter.toTimestamp(value);
	}

	public DateTime toBeanType(Object value) {
		if (value instanceof java.util.Date){
			return new DateTime(((java.util.Date)value).getTime());
		}
		return (DateTime)value;
	}

    public String format(DateTime t) {
        Timestamp ts = new Timestamp(t.getMillis());
        return ts.toString();
    }

	public DateTime parse(String value) {
		Timestamp ts = Timestamp.valueOf(value);
		return new DateTime(ts.getTime());
	}

	public DateTime parseDateTime(long systemTimeMillis) {
		return new DateTime(systemTimeMillis);
	}

	public boolean isDateTimeCapable() {
		return true;
	}

}
