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
import java.util.EnumSet;


/**
 * Converts between bean Enum types to database string or integer columns.
 * <p>
 * An instance of this is set to every BeanProperty associated with an Enum type
 * (unless a custom PropertyConverter is used).
 * </p>
 * <p>
 * The limitation of this class is that it converts the Enum to either the ordinal
 * or string value of the Enum. If you wish to convert the Enum to some other value
 * then you will need to use your own custom PropertyConverter.
 * </p>
 */
public class ScalarTypeEnum  {
	
	public static class StringEnum extends ScalarTypeBase {

		@SuppressWarnings("unchecked")
		private final Class enumType;
		
		/**
		 * Create a ScalarTypeEnum.
		 */
		@SuppressWarnings("unchecked")
		public StringEnum(Class enumType) {
			super(enumType, false, Types.VARCHAR);
			this.enumType = enumType;
		}
		
		public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
			if (value == null){
				pstmt.setNull(index, Types.VARCHAR);
			} else {
				pstmt.setString(index, value.toString());
			}
		}
	
		@SuppressWarnings("unchecked")
		public Object read(ResultSet rset, int index) throws SQLException {
			
			String string = rset.getString(index);
			if (string == null){
				return null;
			} else {
				return Enum.valueOf(enumType, string);
			}
		}
		
		/**
		 * Convert the Boolean value to the db value.
		 */
		@SuppressWarnings("unchecked")
		public Object toJdbcType(Object beanValue) {
			if (beanValue == null) {
				return null;
			}
			Enum e = (Enum)beanValue;
			return e.toString();
		}
	
		/**
		 * Convert the db value to the Boolean value.
		 */
		@SuppressWarnings("unchecked")
		public Object toBeanType(Object dbValue) {
			if (dbValue == null) {
				return null;
			}
			
			return Enum.valueOf(enumType, (String)dbValue);
		}
	}
	
	public static class OrdinalEnum extends ScalarTypeBase {


		private final Object[] enumArray;
		
		/**
		 * Create a ScalarTypeEnum.
		 */
		@SuppressWarnings("unchecked")
		public OrdinalEnum(Class enumType) {
			super(enumType, false, Types.INTEGER);
			this.enumArray = EnumSet.allOf(enumType).toArray();
		}
		
		public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
			if (value == null){
				pstmt.setNull(index, Types.INTEGER);
			} else {
				Enum<?> e = (Enum<?>)value;
				pstmt.setInt(index, e.ordinal());
			}
		}
	
		@SuppressWarnings("unchecked")
		public Object read(ResultSet rset, int index) throws SQLException {
			
			int ordinal = rset.getInt(index);
			if (rset.wasNull()){
				return null;
			} else {
				if (ordinal < 0 || ordinal >= enumArray.length){
					String m = "Unexpected ordinal ["+ordinal+"] out of range ["+enumArray.length+"]";
					throw new IllegalStateException(m);
				}
				return enumArray[ordinal];
			}
		}
		
		/**
		 * Convert the Boolean value to the db value.
		 */
		@SuppressWarnings("unchecked")
		public Object toJdbcType(Object beanValue) {
			if (beanValue == null) {
				return null;
			}
			Enum e = (Enum)beanValue;
			return e.ordinal();
		}
	
		/**
		 * Convert the db value to the Boolean value.
		 */
		@SuppressWarnings("unchecked")
		public Object toBeanType(Object dbValue) {
			if (dbValue == null) {
				return null;
			}
			
			int ordinal = ((Integer)dbValue).intValue();
			if (ordinal < 0 || ordinal >= enumArray.length){
				String m = "Unexpected ordinal ["+ordinal+"] out of range ["+enumArray.length+"]";
				throw new IllegalStateException(m);
			}
			return enumArray[ordinal];
		}
	}
}
