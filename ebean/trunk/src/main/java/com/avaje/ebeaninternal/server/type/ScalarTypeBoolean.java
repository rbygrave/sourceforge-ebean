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

import java.sql.SQLException;
import java.sql.Types;

import com.avaje.ebean.text.TextException;
import com.avaje.ebeaninternal.server.core.BasicTypeConverter;
import com.avaje.ebeaninternal.server.lucene.LLuceneTypes;

/**
 * ScalarType for Boolean and boolean.
 * <p>
 * This may or may not be a native jdbc type depending on the database and jdbc
 * driver.
 * </p>
 */
public class ScalarTypeBoolean {

	public static class Native extends ScalarTypeBase<Boolean> {

		/**
		 * Native Boolean database type.
		 */
		public Native() {
			super(Boolean.class, true, Types.BOOLEAN);
		}

		public Boolean toBeanType(Object value) {
			return BasicTypeConverter.toBoolean(value);
		}
		
		public Object toJdbcType(Object value) {
			return BasicTypeConverter.convert(value, jdbcType);
		}
		
        public String formatValue(Boolean t) {
            return t.toString();
        }

		public Boolean parse(String value) {
			return Boolean.valueOf(value);
		}
		
		public Boolean parseDateTime(long systemTimeMillis) {
			throw new TextException("Not Supported");
		}
		
		public boolean isDateTimeCapable() {
			return false;
		}

		public void bind(DataBind b, Boolean value) throws SQLException {
			if (value == null) {
				b.setNull(Types.BOOLEAN);
			} else {
				b.setBoolean(value);
			}

		}

		public Boolean read(DataReader dataReader) throws SQLException {
		    return dataReader.getBoolean();
		}
		
	    public int getLuceneType() {
	        return LLuceneTypes.STRING;
	    }

	    public Object luceneFromIndexValue(Object value) {
	        return parse((String)value);
	    }

	    public Object luceneToIndexValue(Object value) {
	        return format(value);
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
	public static class BitBoolean extends ScalarTypeBase<Boolean> {

		/**
		 * Native Boolean database type.
		 */
		public BitBoolean() {
			super(Boolean.class, true, Types.BIT);
		}

		public Boolean toBeanType(Object value) {
			return BasicTypeConverter.toBoolean(value);
		}
		
		public Object toJdbcType(Object value) {
			// use JDBC driver to convert boolean to bit
			return BasicTypeConverter.toBoolean(value);
		}
		
		public String formatValue(Boolean t) {
            return t.toString();
        }

        public Boolean parse(String value) {
			return Boolean.valueOf(value);
		}
		
		public Boolean parseDateTime(long systemTimeMillis) {
			throw new TextException("Not Supported");
		}

		public boolean isDateTimeCapable() {
			return false;
		}
		
		public void bind(DataBind b, Boolean value) throws SQLException {
			if (value == null) {
				b.setNull(Types.BIT);
			} else {
				// use JDBC driver to convert boolean to bit
				b.setBoolean(value);
			}
		}

		public Boolean read(DataReader dataReader) throws SQLException {
		    return dataReader.getBoolean();
		}
		
	    public int getLuceneType() {
	        return LLuceneTypes.STRING;
	    }

	    public Object luceneFromIndexValue(Object value) {
	        return parse((String)value);
	    }

	    public Object luceneToIndexValue(Object value) {
	        return format(value);
	    }
	}

	/**
	 * Converted to/from an Integer in the Database.
	 */
	public static class IntBoolean extends ScalarTypeBase<Boolean> {
		
		private final Integer trueValue;
		private final Integer falseValue;

		public IntBoolean(Integer trueValue, Integer falseValue) {
			super(Boolean.class, false, Types.INTEGER);
			this.trueValue = trueValue;
			this.falseValue = falseValue;
		}
		
		@Override
		public int getLength() {
			return 1;
		}

		public void bind(DataBind b, Boolean value) throws SQLException {
			if (value == null) {
				b.setNull(Types.INTEGER);
			} else {
				b.setInt(toInteger(value));
			}
		}

		public Boolean read(DataReader dataReader) throws SQLException {
		    Integer i = dataReader.getInt();
		    if (i == null){
		        return null;
		    }
			if (i.equals(trueValue)){
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
		public Boolean toBeanType(Object value) {
			if (value == null) {
				return null;
			}
			if (value instanceof Boolean){
				return (Boolean)value;
			}
			if (trueValue.equals(value)) {
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		}
		
        public String formatValue(Boolean t) {
            return t.toString();
        }

		public Boolean parse(String value) {
			return Boolean.valueOf(value);
		}
		public Boolean parseDateTime(long systemTimeMillis) {
			throw new TextException("Not Supported");
		}

		public boolean isDateTimeCapable() {
			return false;
		}
	    public int getLuceneType() {
	        return LLuceneTypes.STRING;
	    }

	    public Object luceneFromIndexValue(Object value) {
	        return parse((String)value);
	    }

	    public Object luceneToIndexValue(Object value) {
	        return format(value);
	    }
	}
	
	/**
	 * Converted to/from an Integer in the Database.
	 */
	public static class StringBoolean extends ScalarTypeBase<Boolean> {
		
		private final String trueValue;
		private final String falseValue;

		public StringBoolean(String trueValue, String falseValue) {
			super(Boolean.class, false, Types.VARCHAR);
			this.trueValue = trueValue;
			this.falseValue = falseValue;
		}

		
		
		@Override
		public int getLength() {
			// typically this will return 1
			return Math.max(trueValue.length(), falseValue.length());
		}

		public void bind(DataBind b, Boolean value) throws SQLException {
			if (value == null) {
				b.setNull(Types.VARCHAR);
			} else {
				b.setString(toString(value));
			}
		}

		public Boolean read(DataReader dataReader) throws SQLException {
			String string = dataReader.getString();
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
		public Boolean toBeanType(Object value) {
			if (value == null) {
				return null;
			}
			if (value instanceof Boolean){
				return (Boolean)value;
			}
			if (trueValue.equals(value)) {
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		}
		
        public String formatValue(Boolean t) {
            return t.toString();
        }

		public Boolean parse(String value) {
			return Boolean.valueOf(value);
		}
		
		public Boolean parseDateTime(long systemTimeMillis) {
			throw new TextException("Not Supported");
		}
		
		public boolean isDateTimeCapable() {
			return false;
		}
	    public int getLuceneType() {
	        return LLuceneTypes.STRING;
	    }

	    public Object luceneFromIndexValue(Object value) {
	        return parse((String)value);
	    }

	    public Object luceneToIndexValue(Object value) {
	        return format(value);
	    }
	}
}
