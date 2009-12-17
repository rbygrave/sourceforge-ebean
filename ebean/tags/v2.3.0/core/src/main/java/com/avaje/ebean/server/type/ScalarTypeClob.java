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
 * ScalarType for String.
 */
public class ScalarTypeClob extends ScalarTypeBase<String> {

	static final int clobBufferSize = 512;
	
	static final int stringInitialSize = 512;
	
	protected ScalarTypeClob(boolean jdbcNative, int jdbcType) {
		super(String.class, jdbcNative, jdbcType);
	}
	
	public ScalarTypeClob() {
		super(String.class, true, Types.CLOB);
	}

	public void bind(DataBind b, String value) throws SQLException {
		if (value == null) {
			b.setNull(Types.VARCHAR);
		} else {
			b.setString(value);
		}
	}

	public String read(DataReader dataReader) throws SQLException {

	    return dataReader.getStringClob();
//		Clob clob = rset.getClob(index);
//		if (clob == null) {
//			return null;
//		}
//		Reader reader = clob.getCharacterStream();
//		if (reader == null) {
//			return null;
//		}
//		return readStringLob(reader);
	}

	public Object toJdbcType(Object value) {
		return BasicTypeConverter.toString(value);
	}

	public String toBeanType(Object value) {
		return BasicTypeConverter.toString(value);
	}

	public String parse(String value) {
		return value;
	}

	public String parseDateTime(long systemTimeMillis) {
		return String.valueOf(systemTimeMillis);
	}

	public boolean isDateTimeCapable() {
		return true;
	}
	
//	protected String readStringLob(Reader reader) throws SQLException {
//
//		char[] buffer = new char[clobBufferSize];
//		int readLength = 0;
//		StringBuilder out = new StringBuilder(stringInitialSize);
//		try {
//			while ((readLength = reader.read(buffer)) != -1) {
//				out.append(buffer, 0, readLength);
//			}
//			reader.close();
//		} catch (IOException e) {
//			throw new SQLException(Message.msg("persist.clob.io", e.getMessage()));
//		}
//
//		return out.toString();
//	}
}
