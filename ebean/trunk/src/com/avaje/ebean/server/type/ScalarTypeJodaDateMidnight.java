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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.joda.time.DateMidnight;

import com.avaje.ebean.bean.BasicTypeConverter;

/**
 * ScalarType for Joda DateMidnight. This maps to a JDBC Date.
 */
public class ScalarTypeJodaDateMidnight extends ScalarTypeBase {

	/**
	 * Instantiates a new scalar type joda date midnight.
	 */
	public ScalarTypeJodaDateMidnight() {
		super(DateMidnight.class, false, Types.DATE);
	}
	
	/* (non-Javadoc)
	 * @see com.avaje.ebean.server.type.ScalarType#bind(java.sql.PreparedStatement, int, java.lang.Object)
	 */
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null){
			pstmt.setNull(index, Types.DATE);
		} else {
			DateMidnight dateMidnight = (DateMidnight)value;
			pstmt.setDate(index, new Date(dateMidnight.getMillis()));
		}
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.server.type.ScalarType#read(java.sql.ResultSet, int)
	 */
	public Object read(ResultSet rset, int index) throws SQLException {
		
		final Date date = rset.getDate(index);
		if (date == null){
			return null;
		} else {
			return new DateMidnight(date.getTime());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.avaje.ebean.server.type.ScalarType#toJdbcType(java.lang.Object)
	 */
	public Object toJdbcType(Object value) {
		if (value instanceof DateMidnight){
			return new Date(((DateMidnight)value).getMillis());
		}
		return BasicTypeConverter.toDate(value);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.server.type.ScalarType#toBeanType(java.lang.Object)
	 */
	public Object toBeanType(Object value) {
		if (value instanceof java.util.Date){
			return new DateMidnight(((java.util.Date)value).getTime());
		}
		return value;
	}

}
