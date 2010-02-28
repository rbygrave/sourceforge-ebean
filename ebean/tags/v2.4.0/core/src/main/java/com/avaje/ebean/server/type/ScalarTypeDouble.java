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
import java.sql.Types;

import com.avaje.ebean.server.core.BasicTypeConverter;

/**
 * ScalarType for Double and double.
 */
public class ScalarTypeDouble extends ScalarTypeBase<Double> {
	
	public ScalarTypeDouble() {
		super(Double.class, true, Types.DOUBLE);
	}
	
	public void bind(DataBind b, Double value) throws SQLException {
		if (value == null){
			b.setNull(Types.DOUBLE);
		} else {
			b.setDouble(value.doubleValue());
		}
	}

	public Double read(DataReader dataReader) throws SQLException {
		
		return dataReader.getDouble();
	}
	
	public Object toJdbcType(Object value) {
		return BasicTypeConverter.toDouble(value);
	}

	public Double toBeanType(Object value) {
		return BasicTypeConverter.toDouble(value);
	}

	
	public String format(Double t) {
        return t.toString();
    }

    public Double parse(String value) {
		return Double.valueOf(value);
	}

	public Double parseDateTime(long systemTimeMillis) {
		return Double.valueOf(systemTimeMillis);
	}

	public boolean isDateTimeCapable() {
		return true;
	}

}
