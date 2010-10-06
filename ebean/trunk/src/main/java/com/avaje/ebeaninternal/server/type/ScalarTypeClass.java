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
package com.avaje.ebeaninternal.server.type;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Logger;

import com.avaje.ebeaninternal.server.lucene.LLuceneTypes;

/**
 * ScalarType for Class<?>.
 */
public class ScalarTypeClass extends ScalarTypeBase<Class<?>> {
	
	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(ScalarTypeClass.class.getName());
	
	
	/**
	 * Instantiates a new scalar type class.
	 */
	public ScalarTypeClass() {
		super(null, false, Types.VARCHAR);
	}
	
	/* (non-Javadoc)
	 * @see com.avaje.ebeaninternal.server.type.ScalarType#bind(com.avaje.ebeaninternal.server.type.DataBind, java.lang.Object)
	 */
	public void bind(DataBind b, Class<?> value) throws SQLException {
		if (value == null){
			b.setNull(Types.VARCHAR);
		} else {
			b.setString(value.getCanonicalName());
		}
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebeaninternal.server.type.ScalarType#read(com.avaje.ebeaninternal.server.type.DataReader)
	 */
	public Class<?> read(DataReader dataReader) throws SQLException {
		final String s = dataReader.getString();
		
		if (s != null){
			return parse(s);
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.avaje.ebeaninternal.server.type.ScalarType#toJdbcType(java.lang.Object)
	 */
	public Object toJdbcType(Object value) {
		Class<?> c= (Class<?>) value;
		return c != null ? c.getCanonicalName() : null;
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebeaninternal.server.type.ScalarType#toBeanType(java.lang.Object)
	 */
	public Class<?> toBeanType(Object value) {
		if (value != null){
			return parse(value.toString());
		}
		
		return null;
	}

    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.type.ScalarType#formatValue(java.lang.Object)
     */
    public String formatValue(Class<?> t) {
        return t != null ? t.getCanonicalName() : null;
    }
	
	/* (non-Javadoc)
	 * @see com.avaje.ebeaninternal.server.type.ScalarType#parse(java.lang.String)
	 */
	public Class<?> parse(String value) {
		try {
			return Class.forName(value.toString());
		} catch (ClassNotFoundException e) {
			// ~EMG should we throw an runtime exception here??
			logger.fine(e.getMessage());
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.avaje.ebeaninternal.server.type.ScalarType#isDateTimeCapable()
	 */
	public boolean isDateTimeCapable() {
		return false;
	}

    /**
     * To json string.
     * 
     * @param value the value
     * 
     * @return the string
     */
    public String toJsonString(Class<?> value) {
        if(value == null) {
            return "null";
        } else {
            return value.getCanonicalName();
        }
    }
    
    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.type.ScalarType#getLuceneType()
     */
    public int getLuceneType() {
        return LLuceneTypes.STRING;
    }

    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.type.ScalarType#luceneFromIndexValue(java.lang.Object)
     */
    public Object luceneFromIndexValue(Object value) {
        return value;
    }

    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.type.ScalarType#luceneToIndexValue(java.lang.Object)
     */
    public Object luceneToIndexValue(Object value) {
        return value;
    }
    
    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.type.ScalarType#readData(java.io.DataInput)
     */
    public Object readData(DataInput dataInput) throws IOException {
        if (!dataInput.readBoolean()) {
            return null;
        } else {
            return dataInput.readUTF();
        }
    }

    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.type.ScalarType#writeData(java.io.DataOutput, java.lang.Object)
     */
    public void writeData(DataOutput dataOutput, Object v) throws IOException {
        
        Class<?> value = (Class<?>)v;
        if (value == null){
            dataOutput.writeBoolean(false);
        } else {
            dataOutput.writeBoolean(true);
            dataOutput.writeUTF(value.getCanonicalName());            
        }
    }

	/* (non-Javadoc)
	 * @see com.avaje.ebeaninternal.server.type.ScalarType#parseDateTime(long)
	 */
	public Class<?> parseDateTime(long dateTime) {
		return null;
	}
}
