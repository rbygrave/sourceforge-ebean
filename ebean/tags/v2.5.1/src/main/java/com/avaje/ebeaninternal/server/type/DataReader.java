/**
 * Copyright (C) 2009 Authors
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

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;

public interface DataReader {

    public void resetColumnPosition();
    
    public void incrementPos(int increment);

    public byte[] getBinaryBytes() throws SQLException;

    public byte[] getBlobBytes() throws SQLException;

    public String getStringFromStream() throws SQLException;

    public String getStringClob() throws SQLException;
    
    public String getString() throws SQLException;

    public Boolean getBoolean() throws SQLException;

    public Byte getByte() throws SQLException;

    public Short getShort() throws SQLException;

    public Integer getInt() throws SQLException;

    public Long getLong() throws SQLException;

    public Float getFloat() throws SQLException;

    public Double getDouble() throws SQLException;

    public byte[] getBytes() throws SQLException;

    public java.sql.Date getDate() throws SQLException;

    public java.sql.Time getTime() throws SQLException;

    public java.sql.Timestamp getTimestamp() throws SQLException;

//    public java.io.InputStream getAsciiStream() throws SQLException;
//
//    public java.io.InputStream getBinaryStream() throws SQLException;

    // Object getObject(int columnIndex) throws SQLException;

    // --------------------------JDBC 2.0-----------------------------------

    // ---------------------------------------------------------------------
    // Getters and Setters
    // ---------------------------------------------------------------------

//    java.io.Reader getCharacterStream() throws SQLException;

    public BigDecimal getBigDecimal() throws SQLException;

    // Object getObject(int i, java.util.Map<String,Class<?>> map)
    // throws SQLException;

//  Blob getBlob() throws SQLException;
    //
//        Clob getClob() throws SQLException;

//    Ref getRef() throws SQLException;


    Array getArray() throws SQLException;

    // -------------------------- JDBC 3.0
    // ----------------------------------------
    //
    // java.net.URL getURL(int columnIndex) throws SQLException;

}
