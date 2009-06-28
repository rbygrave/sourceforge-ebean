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
 * MySQL specific plugin.
 * <p>
 * <ul>
 * <li>supportsGetGeneratedKeys = true</li>
 * <li>Uses LIMIT OFFSET clause</li>
 * <li>Uses ` for quoted identifiers</li>
 * </ul>
 * </p>
 */
public class MySqlPlatform extends DatabasePlatform {

    public MySqlPlatform(){
        super();
        this.name = "mysql";
        this.dbIdentity.setIdType(IdType.IDENTITY);
        this.dbIdentity.setSupportsGetGeneratedKeys(true);
        
        this.openQuote = "`";
        this.closeQuote = "`";
        
        this.booleanDbType = Types.BIT;
        
        dbTypeMap.put(Types.BIT, new DbType("tinyint(1) default 0"));
        dbTypeMap.put(Types.BOOLEAN, new DbType("tinyint(1) default 0"));
        dbTypeMap.put(Types.CLOB, new DbType("text"));
        dbTypeMap.put(Types.TIMESTAMP, new DbType("datetime"));
        
        dbDdlSyntax.setDisableReferentialIntegrity("SET FOREIGN_KEY_CHECKS=0");
        dbDdlSyntax.setEnableReferentialIntegrity("SET FOREIGN_KEY_CHECKS=1");
        dbDdlSyntax.setForeignKeySuffix("on delete restrict on update restrict");

    }    

}
