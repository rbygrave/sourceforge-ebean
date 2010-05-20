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

import com.avaje.ebean.text.json.JsonValueAdapter;
import com.avaje.ebeaninternal.server.core.BasicTypeConverter;
import com.avaje.ebeaninternal.server.lucene.LLuceneTypes;

/**
 * ScalarType for java.sql.Date.
 */
public class ScalarTypeDate extends ScalarTypeBase<java.sql.Date> {
	
	public ScalarTypeDate() {
		super(Date.class, true, Types.DATE);
	}
	
	public void bind(DataBind b, java.sql.Date value) throws SQLException {
		if (value == null){
			b.setNull(Types.DATE);
		} else {
			b.setDate(value);
		}
	}

	public java.sql.Date read(DataReader dataReader) throws SQLException {
		
		return dataReader.getDate();
	}
	
	
	public Object toJdbcType(Object value) {
		return BasicTypeConverter.toDate(value);
	}

	public java.sql.Date toBeanType(Object value) {
		return BasicTypeConverter.toDate(value);
	}

	public String formatValue(Date t) {
        return t.toString();
    }

    public java.sql.Date parse(String value) {
		return java.sql.Date.valueOf(value);
	}

	public java.sql.Date parseDateTime(long systemTimeMillis) {
		return new java.sql.Date(systemTimeMillis);
	}

	public boolean isDateTimeCapable() {
		return true;
	}

    @Override
    public String jsonToString(Date value, JsonValueAdapter ctx) {
        return ctx.jsonFromDate(value);
    }

    @Override
    public Date jsonFromString(String value, JsonValueAdapter ctx) {
        return ctx.jsonToDate(value);
    }
	
    public int getLuceneType() {
        return LLuceneTypes.DATE;
    }

    public Object luceneFromIndexValue(Object value) {
        Long l = (Long)value;
        return new Date(l);
    }

    public Object luceneToIndexValue(Object value) {
        return ((Date)value).getTime();
    }
}
