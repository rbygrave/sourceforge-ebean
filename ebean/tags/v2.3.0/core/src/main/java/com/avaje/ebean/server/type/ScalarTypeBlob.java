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

import com.avaje.ebean.text.TextException;

/**
 * ScalarType for String.
 */
public class ScalarTypeBlob extends ScalarTypeBase<byte[]> {

//	private static final int bufferSize = 512;
	
	//private static final int initialSize = 512;
	
	protected ScalarTypeBlob(Class<byte[]> type, boolean jdbcNative, int jdbcType) {
		super(type, jdbcNative, jdbcType);
	}
	
	public ScalarTypeBlob() {
		super(byte[].class, true, Types.BLOB);
	}

	public void bind(DataBind b, byte[] value) throws SQLException {
		if (value == null) {
			b.setNull(Types.BLOB);
		} else {
			b.setBytes(value);
		}
	}

	public Object toJdbcType(Object value) {
		return value;
	}

	public byte[] toBeanType(Object value) {
		return (byte[])value;
	}

	public byte[] parse(String value) {
		throw new TextException("Not supported");
	}
	
	public byte[] parseDateTime(long systemTimeMillis) {
		throw new TextException("Not supported");
	}

	public boolean isDateTimeCapable() {
		return false;
	}
	
	public byte[] read(DataReader dataReader) throws SQLException {

	    return dataReader.getBlobBytes();
	}
	
//	protected byte[] getBinaryLob(InputStream in) throws SQLException {
//
//		try {
//			if (in == null) {
//				return null;
//			}
//			ByteArrayOutputStream out = new ByteArrayOutputStream();
//		
//			byte[] buf = new byte[bufferSize];
//			int len;
//			while ((len = in.read(buf, 0, buf.length)) != -1) {
//				out.write(buf, 0, len);
//			}
//			byte[] data = out.toByteArray();
//		
//			if (data.length == 0) {
//				data = null;
//			}
//			in.close();
//			out.close();
//			return data;
//		
//		} catch (IOException e) {
//			throw new SQLException(e.getClass().getName() + ":" + e.getMessage());
//		}
//	}
}
