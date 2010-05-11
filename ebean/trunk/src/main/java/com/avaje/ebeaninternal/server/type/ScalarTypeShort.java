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
import java.sql.Types;

import com.avaje.ebean.text.TextException;
import com.avaje.ebeaninternal.server.core.BasicTypeConverter;
import com.avaje.ebeaninternal.server.lucene.LuceneTypes;

/**
 * ScalarType for Short and short.
 */
public class ScalarTypeShort extends ScalarTypeBase<Short> {

	public ScalarTypeShort() {
		super(Short.class, true, Types.SMALLINT);
	}
	
	public void bind(DataBind b, Short value) throws SQLException {
		if (value == null){
			b.setNull(Types.SMALLINT);
		} else {
			b.setShort(value.shortValue());
		}
	}

	public Short read(DataReader dataReader) throws SQLException {
		
		return dataReader.getShort();
	}
	
	public Object toJdbcType(Object value) {
		return BasicTypeConverter.toShort(value);
	}

	public Short toBeanType(Object value) {
		return BasicTypeConverter.toShort(value);
	}
	
	public String formatValue(Short v) {
        return v.toString();
    }

    public Short parse(String value) {
		return Short.valueOf(value);
	}

	public Short parseDateTime(long systemTimeMillis) {
		throw new TextException("Not Supported");
	}

	public boolean isDateTimeCapable() {
		return false;
	}

    public int getLuceneType() {
        return LuceneTypes.INT;
    }

    public Object luceneFromIndexValue(Object value) {
        return Short.valueOf(value.toString());
    }

    public Object luceneToIndexValue(Object value) {
        return ((Short)value).intValue();
    }
	
}
