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
import java.util.Iterator;



/**
 * Additional control over mapping to DB values.
 */
public class ScalarTypeEnumWithMapping implements ScalarType, ScalarTypeEnum {

	/**
	 * Data type of the database columns converted to.
	 */
	private final int dbType;

	@SuppressWarnings("unchecked")
	private final Class enumType;
	
	@SuppressWarnings("unchecked")
	private final EnumToDbValueMap beanDbMap;
	
	private final int length;
	
	/**
	 * Create with an explicit mapping of bean to database values.
	 */
	public ScalarTypeEnumWithMapping(EnumToDbValueMap<?> beanDbMap, Class<?> enumType, int length) {
		this.beanDbMap = beanDbMap;
		this.enumType = enumType;
		this.dbType = beanDbMap.getDbType();
		this.length = length;
	}
	
	
	/**
	 * Return the IN values for DB constraint construction.
	 */
	public String getContraintInValues(){

		StringBuilder sb = new StringBuilder();
		
		int i = 0;
		
		sb.append("(");		
		
		Iterator<?> it = beanDbMap.dbValues();
		while (it.hasNext()) {
			Object dbValue = it.next();
			if (i++ > 0){				
				sb.append(",");
			}
			if (!beanDbMap.isIntegerType()){
				sb.append("'");
			}
			sb.append(dbValue.toString());
			if (!beanDbMap.isIntegerType()){
				sb.append("'");
			}
		}
		
		sb.append(")");
		
		return sb.toString();
	}
	
	/**
	 * Return the DB column length for storing the enum value.
	 * <p>
	 * This is for enum's mapped to strings.
	 * </p>
	 */
	public int getLength() {
		return length;
	}



	public int getJdbcType() {
		return dbType;
	}
	
	public boolean isJdbcNative() {
		return false;
	}
	
	public Class<?> getType() {
		return enumType;
	}

	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		beanDbMap.bind(pstmt, index, value);		
	}

	public Object read(ResultSet rset, int index) throws SQLException {
		return beanDbMap.read(rset, index);	}
	
	@SuppressWarnings("unchecked")
	public Object toBeanType(Object dbValue) {
		
		return beanDbMap.getBeanValue(dbValue);
	}

	public Object toJdbcType(Object beanValue) {
		
		return beanDbMap.getDbValue(beanValue);
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
