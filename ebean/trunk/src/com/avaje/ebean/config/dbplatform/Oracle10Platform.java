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
 * Oracle10 and greater specific platform.
 * <p>
 * <ul>
 * <li>supportsGetGeneratedKeys = true</li>
 * <li>Uses ROW_NUMBER to limit results</li>
 * </ul>
 * </p>
 */
public class Oracle10Platform extends DatabasePlatform {

    public Oracle10Platform() {
        super();
        this.name = "oracle";
        //this.dbQueryLimiter = new DbQueryLimiterRowNumber("");
        this.sqlLimiter = new RownumSqlLimiter();

        // use Sequence as default IdType
        dbIdentity.setSupportsGetGeneratedKeys(true);
        dbIdentity.setSupportsSequence(true, IdType.SEQUENCE);
        dbIdentity.setSequenceNextValTemplate("{sequence}.nextval");
        dbIdentity.setSelectSequenceNextValSqlTemplate("select {sequencenextval} from dual");

        this.treatEmptyStringsAsNull = true;
    
        this.openQuote = "\"";
        this.closeQuote = "\"";
        
		dbTypeMap.put(Types.BOOLEAN, new DbType("number(1) default 0"));

		dbTypeMap.put(Types.INTEGER, new DbType("number", 10));
		dbTypeMap.put(Types.BIGINT, new DbType("number", 19));
		dbTypeMap.put(Types.REAL, new DbType("number", 19, 4));
		dbTypeMap.put(Types.DOUBLE, new DbType("number", 19, 4));
		dbTypeMap.put(Types.SMALLINT, new DbType("number", 5));
		dbTypeMap.put(Types.TINYINT, new DbType("number", 3));
		dbTypeMap.put(Types.DECIMAL, new DbType("NUMBER", 38));
		
		dbTypeMap.put(Types.VARCHAR, new DbType("varchar2", 255));

		dbTypeMap.put(Types.LONGVARBINARY, new DbType("blob"));
		dbTypeMap.put(Types.LONGVARCHAR, new DbType("clob"));
		dbTypeMap.put(Types.VARBINARY, new DbType("blob"));

		dbTypeMap.put(Types.TIME, new DbType("timestamp"));

		
		dbDdlSyntax.setDropTableCascade("cascade constraints purge");
		dbDdlSyntax.setIdentity(null);

    }

}
