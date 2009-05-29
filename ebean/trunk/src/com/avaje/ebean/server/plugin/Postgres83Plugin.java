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
package com.avaje.ebean.server.plugin;

import java.sql.Types;

import com.avaje.ebean.server.ddl.DbType;

/**
 * Postgres v8.3 plugin.
 * <p>
 * No support for getGeneratedKeys.
 * </p>
 */
public class Postgres83Plugin extends DbSpecific {

    public Postgres83Plugin(PluginProperties properties) {
        super(properties);
        	
        properties.setPropertyDefault("namingconvention.sequence.name", "{table}_seq");
        properties.setPropertyDefault("namingconvention.sequence.nextval", "nextval('{sequence}')");
        properties.setPropertyDefault("namingconvention.sequence.from", "");
        
        this.supportsSequences = true;
        this.supportsGetGeneratedKeys = false;
        
        this.resultSetLimit = ResultSetLimit.LimitOffset;
        this.openQuote = "\"";
        this.closeQuote = "\"";
        
        dbTypeMap.put(Types.BOOLEAN, new DbType("bit default 0"));

        dbTypeMap.put(Types.DOUBLE, new DbType("float"));
        dbTypeMap.put(Types.TINYINT, new DbType("smallint"));
        dbTypeMap.put(Types.DECIMAL, new DbType("decimal", 38));

        dbTypeMap.put(Types.BLOB, new DbType("bytea"));
        dbTypeMap.put(Types.CLOB, new DbType("text"));
        dbTypeMap.put(Types.LONGVARBINARY, new DbType("bytea"));
        dbTypeMap.put(Types.LONGVARCHAR, new DbType("text"));
    }

}
