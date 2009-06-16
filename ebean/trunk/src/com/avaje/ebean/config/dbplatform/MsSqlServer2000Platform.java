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
package com.avaje.ebean.config.dbplatform;

import java.sql.Types;


/**
 * Microsoft SQL Server 2000 plugin.
 * <p>
 * <ul>
 * <li>supportsGetGeneratedKeys = false</li>
 * <li>Use select @@IDENTITY to return the generated Id instead</li>
 * <li>Uses LIMIT OFFSET clause</li>
 * <li>Uses [ & ] for quoted identifiers</li>
 * </ul>
 * </p>
 */
public class MsSqlServer2000Platform extends DatabasePlatform {

    public MsSqlServer2000Platform(){
        super();
        
        this.dbIdentity.setIdType(IdType.IDENTITY);
        this.dbIdentity.setSupportsGetGeneratedKeys(false);
        this.dbIdentity.setSelectLastInsertedIdTemplate("select @@IDENTITY as X");
        
        this.openQuote = "[";
        this.closeQuote = "]";
        
        dbTypeMap.put(Types.BOOLEAN, new DbType("bit default 0"));

        dbTypeMap.put(Types.BIGINT, new DbType("numeric", 19));
        dbTypeMap.put(Types.REAL, new DbType("float(16)"));
        dbTypeMap.put(Types.DOUBLE, new DbType("float(32)"));
        dbTypeMap.put(Types.TINYINT, new DbType("smallint"));
        dbTypeMap.put(Types.DECIMAL, new DbType("numeric", 28));

        dbTypeMap.put(Types.BLOB, new DbType("image"));
        dbTypeMap.put(Types.CLOB, new DbType("text"));
        dbTypeMap.put(Types.LONGVARBINARY, new DbType("image"));
        dbTypeMap.put(Types.LONGVARCHAR, new DbType("text"));

        dbTypeMap.put(Types.DATE, new DbType("datetime"));
        dbTypeMap.put(Types.TIME, new DbType("datetime"));
        dbTypeMap.put(Types.TIMESTAMP, new DbType("datetime"));

    }    

}
