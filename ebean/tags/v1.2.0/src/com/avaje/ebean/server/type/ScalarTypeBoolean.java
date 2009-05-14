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
import java.sql.Types;

import com.avaje.ebean.util.BasicTypeConverter;

/**
 * ScalarType for Boolean and boolean.
 * <p>
 * This may or may not be a native jdbc type depending on the database and jdbc
 * driver.
 * </p>
 */
public class ScalarTypeBoolean {

	public static class Native extends ScalarTypeBase {

		/**
		 * Native Boolean database type.
		 */
		public Native() {
			super(Boolean.class, true, Types.BOOLEAN);
		}

		public Object toBeanType(Object value) {
			return BasicTypeConverter.toBoolean(value);
		}
		
		public Object toJdbcType(Object value) {
			return BasicTypeConverter.convert(value, jdbcType);
		}
		
		public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
			if (value == null) {
				pstmt.setNull(index, Types.BOOLEAN);
			} else {
				pstmt.setBoolean(index, (Boolean) value);
			}

		}

		public Object read(ResultSet rset, int index) throws SQLException {
			boolean b = rset.getBoolean(index);
			if (rset.wasNull()) {
				return null;
			}
			return b;
		}
	}
	
	/**
	 * The Class BitBoolean converts a JDBC type BIT to a java boolean
	 * 
	 * <p>
	 * Sometimes booleans may be mapped to the JDBC type BIT. To use
	 * the BitBoolean specify type.boolean.dbtype="bit" in the ebean configuration  
	 * </p>
	 */
	public static class BitBoolean extends ScalarTypeBase {

		/**
		 * Native Boolean database type.
		 */
		public BitBoolean() {
			super(Boolean.class, true, Types.BIT);
		}

		public Object toBeanType(Object value) {
			return BasicTypeConverter.toBoolean(value);
		}
		
		public Object toJdbcType(Object value) {
			// use JDBC driver to convert boolean to bit
			return BasicTypeConverter.toBoolean(value);
		}
		
		public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
			if (value == null) {
				pstmt.setNull(index, Types.BIT);
			} else {
				// use JDBC driver to convert boolean to bit
				pstmt.setBoolean(index, (Boolean) value);
			}
		}

		public Object read(ResultSet rset, int index) throws SQLException {
			boolean b = rset.getBoolean(index);
			if (rset.wasNull()) {
				return null;
			}
			return b;
		}
	}

	/**
	 * Converted to/from an Integer in the Database.
	 */
	public static class IntBoolean extends ScalarTypeBase {
		
		final Integer trueValue;
		final Integer falseValue;

		public IntBoolean(Integer trueValue, Integer falseValue) {
			super(Boolean.class, false, Types.INTEGER);
			this.trueValue = trueValue;
			this.falseValue = falseValue;
		}

		public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
			if (value == null) {
				pstmt.setNull(index, Types.INTEGER);
			} else {
				pstmt.setInt(index, toInteger(value));
			}
		}

		public Object read(ResultSet rset, int index) throws SQLException {
			int i = rset.getInt(index);
			if (rset.wasNull()) {
				return null;
			}
			Integer integer = Integer.valueOf(i);
			if (integer.equals(trueValue)){
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		}

		public Object toJdbcType(Object value) {
			return toInteger(value);
		}
		
		/**
		 * Convert the Boolean value to the db value.
		 */
		public Integer toInteger(Object value) {
			if (value == null) {
				return null;
			}
			Boolean b = (Boolean) value;
			if (b.booleanValue()) {
				return trueValue;
			} else {
				return falseValue;
			}
		}

		/**
		 * Convert the db value to the Boolean value.
		 */
		public Object toBeanType(Object value) {
			if (value == null) {
				return null;
			}
			if (value instanceof Boolean){
				return value;
			}
			if (trueValue.equals(value)) {
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		}
	}
	
	/**
	 * Converted to/from an Integer in the Database.
	 */
	public static class StringBoolean extends ScalarTypeBase {
		
		final String trueValue;
		final String falseValue;

		public StringBoolean(String trueValue, String falseValue) {
			super(Boolean.class, false, Types.VARCHAR);
			this.trueValue = trueValue;
			this.falseValue = falseValue;
		}

		public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
			if (value == null) {
				pstmt.setNull(index, Types.VARCHAR);
			} else {
				pstmt.setString(index, toString(value));
			}
		}

		public Object read(ResultSet rset, int index) throws SQLException {
			String string = rset.getString(index);
			if (string == null) {
				return null;
			}
			
			if (string.equals(trueValue)){
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		}

		public Object toJdbcType(Object value) {
			return toString(value);
		}
		
		/**
		 * Convert the Boolean value to the db value.
		 */
		public String toString(Object value) {
			if (value == null) {
				return null;
			}
			Boolean b = (Boolean) value;
			if (b.booleanValue()) {
				return trueValue;
			} else {
				return falseValue;
			}
		}

		/**
		 * Convert the db value to the Boolean value.
		 */
		public Object toBeanType(Object value) {
			if (value == null) {
				return null;
			}
			if (value instanceof Boolean){
				return value;
			}
			if (trueValue.equals(value)) {
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		}
	}
}
