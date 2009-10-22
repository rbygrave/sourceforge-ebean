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



/**
 * H2 specific platform.
 * <p>
 * <ul>
 * <li>supportsGetGeneratedKeys = true</li>
 * <li>Uses LIMIT OFFSET clause</li>
 * <li>Uses double quotes for quoted identifiers</li>
 * </ul>
 * </p>
 */
public class H2Platform extends DatabasePlatform {

    public H2Platform(){
        super();
        this.name = "h2";
        
        this.dbIdentity.setSupportsGetGeneratedKeys(true);
        this.dbIdentity.setIdType(IdType.IDENTITY);
        
        this.dbIdentity.setSupportsSequence(true, IdType.IDENTITY);
        this.dbIdentity.setSequenceNextValTemplate("{sequence}.nextval");
        this.dbIdentity.setSelectSequenceNextValSqlTemplate("select {sequencenextval}");
        
        this.openQuote = "\"";
        this.closeQuote = "\"";
        
        // H2 data types match default JDBC types
        // so no changes to dbTypeMap required
        
        this.dbDdlSyntax.setDropIfExists("if exists");
        this.dbDdlSyntax.setDisableReferentialIntegrity("SET REFERENTIAL_INTEGRITY FALSE");
        this.dbDdlSyntax.setEnableReferentialIntegrity("SET REFERENTIAL_INTEGRITY TRUE");
        this.dbDdlSyntax.setForeignKeySuffix("on delete restrict on update restrict");
    }    

}
