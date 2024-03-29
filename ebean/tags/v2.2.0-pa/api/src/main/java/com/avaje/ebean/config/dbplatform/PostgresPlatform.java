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

import javax.sql.DataSource;

import com.avaje.ebean.BackgroundExecutor;
import com.avaje.ebean.config.GlobalProperties;


/**
 * Postgres v8.3 specific platform.
 * <p>
 * No support for getGeneratedKeys.
 * </p>
 */
public class PostgresPlatform extends DatabasePlatform {

    public PostgresPlatform() {
        super();
        this.name = "postgres";   
        
        this.dbIdentity.setSupportsGetGeneratedKeys(false);
        this.dbIdentity.setIdType(IdType.SEQUENCE);
        this.dbIdentity.setSupportsSequence(true);

        String colAlias = GlobalProperties.get("ebean.columnAliasPrefix", null);
        if (colAlias == null){
        	// Postgres requires the "as" keyword for column alias
        	GlobalProperties.put("ebean.columnAliasPrefix", "as c");
        }
        
        this.openQuote = "\"";
        this.closeQuote = "\"";
        
        //dbTypeMap.put(Types.BOOLEAN, new DbType("bit default 0"));

        dbTypeMap.put(Types.INTEGER, new DbType("integer",false));
        dbTypeMap.put(Types.DOUBLE, new DbType("float"));
        dbTypeMap.put(Types.TINYINT, new DbType("smallint"));
        dbTypeMap.put(Types.DECIMAL, new DbType("decimal", 38));

        dbTypeMap.put(Types.BLOB, new DbType("bytea"));
        dbTypeMap.put(Types.CLOB, new DbType("text"));
        dbTypeMap.put(Types.LONGVARBINARY, new DbType("bytea"));
        dbTypeMap.put(Types.LONGVARCHAR, new DbType("text"));
        
		dbDdlSyntax.setDropTableCascade("cascade");
		dbDdlSyntax.setDropIfExists("if exists");

    }

    /**
     * Create a Postgres specific sequence IdGenerator.
     */
	@Override
	public IdGenerator createSequenceIdGenerator(BackgroundExecutor be,
			DataSource ds, String seqName, int batchSize) {
		
		return new PostgresSequenceIdGenerator(be, ds, seqName, batchSize);
	}

    
    
}
