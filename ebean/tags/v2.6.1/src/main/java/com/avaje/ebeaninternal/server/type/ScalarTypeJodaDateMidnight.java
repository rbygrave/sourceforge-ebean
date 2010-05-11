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
import java.sql.Types;

import org.joda.time.DateMidnight;

import com.avaje.ebean.text.json.JsonValueAdapter;
import com.avaje.ebeaninternal.server.core.BasicTypeConverter;
import com.avaje.ebeaninternal.server.lucene.LuceneTypes;

/**
 * ScalarType for Joda DateMidnight. This maps to a JDBC Date.
 */
public class ScalarTypeJodaDateMidnight extends ScalarTypeBase<DateMidnight> {

	/**
	 * Instantiates a new scalar type joda date midnight.
	 */
	public ScalarTypeJodaDateMidnight() {
		super(DateMidnight.class, false, Types.DATE);
	}
	
	public void bind(DataBind b, DateMidnight value) throws SQLException {
		if (value == null){
			b.setNull(Types.DATE);
		} else {
		    b.setDate(new Date(value.getMillis()));
		}
	}

	public DateMidnight read(DataReader dataReader) throws SQLException {
		
		final Date date = dataReader.getDate();
		if (date == null){
			return null;
		} else {
			return new DateMidnight(date.getTime());
		}
	}
	
	public Object toJdbcType(Object value) {
		if (value instanceof DateMidnight){
			return new Date(((DateMidnight)value).getMillis());
		}
		return BasicTypeConverter.toDate(value);
	}

	public DateMidnight toBeanType(Object value) {
		if (value instanceof java.util.Date){
			return new DateMidnight(((java.util.Date)value).getTime());
		}
		return (DateMidnight)value;
	}
	
    public String formatValue(DateMidnight v) {
        Date sqlDate = new Date(v.getMillis());
        return sqlDate.toString();
    }

	public DateMidnight parse(String value) {
		Date sqlDate = Date.valueOf(value);
		return new DateMidnight(sqlDate.getTime());
	}

	public DateMidnight parseDateTime(long systemTimeMillis) {
		return new DateMidnight(systemTimeMillis);
	}

	public boolean isDateTimeCapable() {
		return true;
	}

    @Override
    public String jsonToString(DateMidnight value, JsonValueAdapter ctx) {
        java.sql.Date d = (java.sql.Date)toJdbcType(value);
        return ctx.jsonFromDate(d);
    }
    
    public int getLuceneType() {
        return LuceneTypes.DATE;
    }

    public Object luceneFromIndexValue(Object value) {
        Long l = (Long)value;
        return toBeanType(new Date(l));
    }

    public Object luceneToIndexValue(Object value) {
        Date v = (Date)toJdbcType(value);
        return v.getTime();
    }  
}
