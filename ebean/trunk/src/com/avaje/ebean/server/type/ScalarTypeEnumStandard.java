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
 * JPA standard based Enum scalar type.
 * <p>
 * Converts between bean Enum types to database string or integer columns.
 * </p>
 * <p>
 * The limitation of this class is that it converts the Enum to either the ordinal
 * or string value of the Enum. If you wish to convert the Enum to some other value
 * then you should look at the Ebean specific @EnumMapping.
 * </p>
 */
public class ScalarTypeEnumStandard {
	
	public static class StringEnum extends ScalarTypeBase implements ScalarTypeEnum {

		@SuppressWarnings("unchecked")
		private final Class enumType;
		
		private final int length;
		
		/**
		 * Create a ScalarTypeEnum.
		 */
		@SuppressWarnings("unchecked")
		public StringEnum(Class enumType) {
			super(enumType, false, Types.VARCHAR);
			this.enumType = enumType;
			this.length = maxValueLength(enumType);
		}

		/**
		 * Return the IN values for DB constraint construction.
		 */
		public String getContraintInValues(){

			StringBuilder sb = new StringBuilder();
			
			sb.append("(");
			Object[] ea = enumType.getEnumConstants();
			for (int i = 0; i < ea.length; i++) {
				Enum<?> e = (Enum<?>)ea[i];
				if (i > 0){
					sb.append(",");
				}
				sb.append("'").append(e.toString()).append("'");
			}
			sb.append(")");
			
			return sb.toString();
		}
		
		private int maxValueLength(Class<?> enumType){
			
			int maxLen = 0;
			
			Object[] ea = enumType.getEnumConstants();
			for (int i = 0; i < ea.length; i++) {
				Enum<?> e = (Enum<?>)ea[i];
				maxLen = Math.max(maxLen, e.toString().length());
			}
			
			return maxLen;
		}
		
		
		
		public int getLength() {
			return length;
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
	
	public static class OrdinalEnum extends ScalarTypeBase implements ScalarTypeEnum {


		private final Object[] enumArray;
		
		/**
		 * Create a ScalarTypeEnum.
		 */
		@SuppressWarnings("unchecked")
		public OrdinalEnum(Class enumType) {
			super(enumType, false, Types.INTEGER);
			this.enumArray = EnumSet.allOf(enumType).toArray();
		}

		/**
		 * Return the IN values for DB constraint construction.
		 */
		public String getContraintInValues(){

			StringBuilder sb = new StringBuilder();
			
			sb.append("(");
			for (int i = 0; i < enumArray.length; i++) {
				Enum<?> e = (Enum<?>)enumArray[i];
				if (i > 0){
					sb.append(",");
				}
				sb.append(e.ordinal());
			}
			sb.append(")");
			
			return sb.toString();
		}

		
		public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
			if (value == null){
				pstmt.setNull(index, Types.INTEGER);
			} else {
				Enum<?> e = (Enum<?>)value;
				pstmt.setInt(index, e.ordinal());
			}
		}
	
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
