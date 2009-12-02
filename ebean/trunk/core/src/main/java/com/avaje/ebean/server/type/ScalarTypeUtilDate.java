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
import java.util.Date;

import com.avaje.ebean.server.core.BasicTypeConverter;

/**
 * ScalarType for java.util.Date.
 */
public class ScalarTypeUtilDate {

	public static class TimestampType extends ScalarTypeBase {

		public TimestampType() {
			super(java.util.Date.class, false, Types.TIMESTAMP);
		}

		public Object read(ResultSet rset, int index) throws SQLException {
			Timestamp timestamp = rset.getTimestamp(index);
			if (timestamp == null) {
				return null;
			} else {
				return new java.util.Date(timestamp.getTime());
			}
		}

		public void bind(PreparedStatement pstmt, int index, Object value)
				throws SQLException {
			if (value == null) {
				pstmt.setNull(index, Types.TIMESTAMP);
			} else {
				java.util.Date date = (java.util.Date) value;

				Timestamp timestamp = new Timestamp(date.getTime());
				pstmt.setTimestamp(index, timestamp);
			}
		}

		public Object toJdbcType(Object value) {
			return BasicTypeConverter.toTimestamp(value);
		}

		public Object toBeanType(Object value) {
			return BasicTypeConverter.toUtilDate(value);
		}

		public Object parse(String value) {
			Timestamp ts = Timestamp.valueOf(value);
			return new java.util.Date(ts.getTime());
		}

		public Object parseDateTime(long systemTimeMillis) {
			return new java.util.Date(systemTimeMillis);
		}

		public boolean isDateTimeCapable() {
			return true;
		}
	}

	public static class DateType extends ScalarTypeBase {

		public DateType() {
			super(Date.class, false, Types.DATE);
		}

		public Object read(ResultSet rset, int index) throws SQLException {
			java.sql.Date d = rset.getDate(index);
			if (d != null) {
				return new java.util.Date(d.getTime());
			}

			return null;
		}

		public void bind(PreparedStatement pstmt, int index, Object value)
				throws SQLException {
			if (value == null) {
				pstmt.setNull(index, Types.TIMESTAMP);
			} else {
				java.util.Date date = (java.util.Date) value;

				java.sql.Date d = new java.sql.Date(date.getTime());
				pstmt.setDate(index, d);
			}
		}

		public Object toJdbcType(Object value) {
			return BasicTypeConverter.toDate(value);
		}

		public Object toBeanType(Object value) {
			return BasicTypeConverter.toUtilDate(value);
		}

		public Object parse(String value) {
			java.sql.Date ts = java.sql.Date.valueOf(value);
			return new java.util.Date(ts.getTime());
		}

		public Object parseDateTime(long systemTimeMillis) {
			return new java.util.Date(systemTimeMillis);
		}

		public boolean isDateTimeCapable() {
			return true;
		}

	}
}
