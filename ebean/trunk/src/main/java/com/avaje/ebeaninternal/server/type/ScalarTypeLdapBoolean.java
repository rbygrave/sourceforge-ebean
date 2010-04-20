/**
 * Copyright (C) 2009  Robin Bygrave
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

/**
 * ScalarType for Boolean and boolean.
 * <p>
 * This may or may not be a native jdbc type depending on the database and jdbc
 * driver.
 * </p>
 */
public class ScalarTypeLdapBoolean extends ScalarTypeBase<Boolean> {

    private static final String trueValue = "TRUE";
    private static final String falseValue = "FALSE";

    public ScalarTypeLdapBoolean() {
        super(Boolean.class, false, Types.VARCHAR);
    }

    @Override
    public int getLength() {
        return 5;
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

        if (string.equals(trueValue)) {
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
        if (value instanceof Boolean) {
            return (Boolean) value;
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

}
