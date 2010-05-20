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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import org.joda.time.LocalDateTime;

import com.avaje.ebean.text.json.JsonValueAdapter;
import com.avaje.ebeaninternal.server.core.BasicTypeConverter;
import com.avaje.ebeaninternal.server.lucene.LLuceneTypes;

/**
 * ScalarType for Joda LocalDateTime. This maps to a JDBC Timestamp.
 */
public class ScalarTypeJodaLocalDateTime extends ScalarTypeBase<LocalDateTime> {

	public ScalarTypeJodaLocalDateTime() {
		super(LocalDateTime.class, false, Types.TIMESTAMP);
	}
	
	public void bind(DataBind b, LocalDateTime value) throws SQLException {
		if (value == null){
			b.setNull(Types.TIMESTAMP);
		} else {
			Timestamp ts = new Timestamp(value.toDateTime().getMillis());
			b.setTimestamp(ts);
		}
	}

	public LocalDateTime read(DataReader dataReader) throws SQLException {
		
		Timestamp ts = dataReader.getTimestamp();
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

	public LocalDateTime toBeanType(Object value) {
		if (value instanceof java.util.Date){
			return new LocalDateTime(((java.util.Date)value).getTime());
		}
		return (LocalDateTime)value;
	}

    public String formatValue(LocalDateTime t) {
        Timestamp ts = new Timestamp(t.toDateTime().getMillis());
        return ts.toString();
    }

	public LocalDateTime parse(String value) {
		Timestamp ts = Timestamp.valueOf(value);
		return new LocalDateTime(ts.getTime());
	}

	public LocalDateTime parseDateTime(long systemTimeMillis) {
		return new LocalDateTime(systemTimeMillis);
	}

	public boolean isDateTimeCapable() {
		return true;
	}

    @Override
    public String jsonToString(LocalDateTime value, JsonValueAdapter ctx) {
        Timestamp d = (Timestamp)toJdbcType(value);
        return ctx.jsonFromTimestamp(d);
    }
    
    public int getLuceneType() {
        return LLuceneTypes.TIMESTAMP;
    }

    public Object luceneFromIndexValue(Object value) {
        Long l = (Long)value;
        return toBeanType(new Timestamp(l));
    }

    public Object luceneToIndexValue(Object value) {
        Timestamp v = (Timestamp)toJdbcType(value);
        return v.getTime();
    }   
}
