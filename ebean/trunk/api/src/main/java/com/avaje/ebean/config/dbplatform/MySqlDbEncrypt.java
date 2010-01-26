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
 * MySql aes_encrypt aes_decrypt based encryption support.
 * 
 * @author rbygrave
 */
public class MySqlDbEncrypt implements DbEncrypt {

    public String getDecryptSql(String columnWithTableAlias) {
        return "AES_DECRYPT(" + columnWithTableAlias + ",?)";
    }

    public String getEncryptBindSql() {
        return "AES_ENCRYPT(?,?)";
    }

    public int getEncryptDbType() {
        return Types.VARBINARY;
    }

    /**
     * For AES_ENCRYPT returns true binding the data before the key.
     */
    public boolean isBindEncryptDataFirst() {
        return true;
    }
    
    
}
