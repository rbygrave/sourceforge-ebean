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
package com.avaje.ebean.config.dbplatform;

import java.sql.Types;

/**
 * H2 encryption support via encrypt decrypt function .
 * 
 * @author rbygrave
 */
public class H2DbEncrypt implements DbEncrypt {

    private static final DbEncryptFunction H2_VARCHAR_ENCRYPT = new H2VarcharFunction();
    private static final DbEncryptFunction H2_DATE_ENCRYPT = new H2DateFunction();
    
    public DbEncryptFunction getDbEncryptFunction(int jdbcType) {
        switch (jdbcType) {
        case Types.VARCHAR:
            return H2_VARCHAR_ENCRYPT;
        case Types.CLOB:
            return H2_VARCHAR_ENCRYPT;
        case Types.CHAR:
            return H2_VARCHAR_ENCRYPT;
        case Types.LONGVARCHAR:
            return H2_VARCHAR_ENCRYPT;

        case Types.DATE:
            return H2_DATE_ENCRYPT;

        default:
            return null;
        }
    }


    public int getEncryptDbType() {
        return Types.VARBINARY;
    }
    
    /**
     * For H2 encrypt function returns false binding the key before the data.
     */
    public boolean isBindEncryptDataFirst() {
        return false;
    }
    
    static class H2VarcharFunction implements DbEncryptFunction {

        public String getDecryptSql(String columnWithTableAlias) {
            // Hmmm, this looks ugly - checking with H2 Database folks.
            return "TRIM(CHAR(0) FROM UTF8TOSTRING(DECRYPT('AES', STRINGTOUTF8(?), " + columnWithTableAlias + ")))";
        }

        public String getEncryptBindSql() {
            return "ENCRYPT('AES', STRINGTOUTF8(?), STRINGTOUTF8(?))";
        }
        
    }
    
    static class H2DateFunction implements DbEncryptFunction {

        public String getDecryptSql(String columnWithTableAlias) {
            return "PARSEDATETIME(TRIM(CHAR(0) FROM UTF8TOSTRING(DECRYPT('AES', STRINGTOUTF8(?), " + columnWithTableAlias + "))),'yyyyMMdd')";
        }

        public String getEncryptBindSql() {
            return "ENCRYPT('AES', STRINGTOUTF8(?), STRINGTOUTF8(FORMATDATETIME(?,'yyyyMMdd')))";
        }
        
    }
}
