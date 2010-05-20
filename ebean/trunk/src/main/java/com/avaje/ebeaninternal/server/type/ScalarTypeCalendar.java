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
package com.avaje.ebeaninternal.server.type;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

import com.avaje.ebeaninternal.server.core.BasicTypeConverter;
import com.avaje.ebeaninternal.server.lucene.LLuceneTypes;

/**
 * ScalarType for java.util.Calendar.
 */
public class ScalarTypeCalendar extends ScalarTypeBase<Calendar> {
	
	public ScalarTypeCalendar(int jdbcType) {
		super(Calendar.class, false, jdbcType);
	}
	
	public Calendar read(DataReader dataReader) throws SQLException {
		Timestamp timestamp = dataReader.getTimestamp();
		if (timestamp == null){
			return null;
		} else {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(timestamp.getTime());
			return cal;
		}
	}
	
	public void bind(DataBind b, Calendar value) throws SQLException {
		if (value == null){
			b.setNull(Types.TIMESTAMP);
		} else {
			Calendar date = (Calendar)value;
			if (jdbcType == Types.TIMESTAMP){
				Timestamp timestamp = new Timestamp(date.getTimeInMillis());
				b.setTimestamp(timestamp);
			} else {
				Date d = new Date(date.getTimeInMillis());
				b.setDate(d);	
			}
		}
	}
	
	public Object toJdbcType(Object value) {
		return BasicTypeConverter.convert(value, jdbcType);
	}

	public Calendar toBeanType(Object value) {
		return BasicTypeConverter.toCalendar(value);
	}
	
	public String formatValue(Calendar t) {
	    Timestamp ts = new Timestamp(t.getTimeInMillis());
	    return ts.toString();
    }

    public Calendar parse(String value) {
		Timestamp ts = Timestamp.valueOf(value);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(ts.getTime());
		return calendar;
	}
	
	public Calendar parseDateTime(long systemTimeMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(systemTimeMillis);
		return calendar;
	}

	public boolean isDateTimeCapable() {
		return true;
	}

    public int getLuceneType() {
        return LLuceneTypes.TIMESTAMP;
    }

    public Object luceneFromIndexValue(Object value) {
        Long l = (Long)value;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(l);
        return c;
    }

    public Object luceneToIndexValue(Object value) {
        Calendar c = (Calendar)value;
        return c.getTime().getTime();
    }
    
	
}
