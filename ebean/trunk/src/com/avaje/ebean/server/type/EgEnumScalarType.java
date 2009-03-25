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

/**
 * An example custom property converter.
 * <p>
 * This only an EXAMPLE of a PropertyConverter rather than part of the Ebean
 * proper.
 * </p>
 * <p>
 * In this case my DB holds Strings "N","A" and "I" that map to New, Active and
 * Inactive. This may be quite common due to the desire to save space in the
 * Database (hence single character codes).
 * </p>
 * <p>
 * Note that BeanToDbMap does most of the work.
 * </p>
 * <p>
 * You can set this onto either the Enum or the property using the
 * PropertyDeploy annotation.
 * </p>
 */
public class EgEnumScalarType implements ScalarType {

	private final BeanToDbMap<EgEnum, String> beanToDbMap;

	private final Class<?> type;
	
	public EgEnumScalarType() {
		type = EgEnum.class;
		beanToDbMap = new BeanToDbMap<EgEnum, String>()
			.add(EgEnum.NEW, "N")
			.add(EgEnum.ACTIVE, "A")
			.add(EgEnum.INACTIVE, "I");
	}

	/**
	 * This is never true.
	 */
	public boolean isJdbcNative() {
		return false;
	}
	
	public Class<?> getType() {
		return type;
	}
	
	public int getJdbcType() {
		// Is varchar as DB values are Strings
		return Types.VARCHAR;
	}

	
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null){
			pstmt.setNull(index, Types.VARCHAR);
		} else {
			String s = beanToDbMap.getDbValue((EgEnum) value);
			pstmt.setString(index, s);
		}
		
	}

	public Object read(ResultSet rset, int index) throws SQLException {
		String s = rset.getString(index);
		if (s == null){
			return null;
		} else {
			return beanToDbMap.getBeanValue(s);
		}
	}

	/**
	 * Return the appropriate EgEnum given the DB String value.
	 */
	public Object toBeanType(Object dbValue) {
		return beanToDbMap.getBeanValue((String) dbValue);
	}

	/**
	 * Return the appropriate DB String value given the EgEnum.
	 */
	public Object toJdbcType(Object beanValue) {

		return beanToDbMap.getDbValue((EgEnum) beanValue);
	}

	/**
	 * Return true if the value is null.
	 */
	public boolean isDbNull(Object value) {
		return value == null;
	}
	
	/**
	 * Returns the value that was passed in.
	 */
	public Object getDbNullValue(Object value) {
		return value;
	}

}
