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

import com.avaje.ebean.server.deploy.IdentityGeneration;

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
public class MsSqlServer2000 extends PluginDbConfig {

    public MsSqlServer2000(PluginProperties properties){
        super(properties);
        
        properties.setPropertyDefault("namingconvention.selectLastInsertedId", "select @@IDENTITY as X");

        this.resultSetLimit = ResultSetLimit.LimitOffset;
        this.identityGeneration = IdentityGeneration.DB_IDENTITY;
        this.supportsGetGeneratedKeys = false;
        this.supportsSequences = false;
        this.openQuote = "[";
        this.closeQuote = "]";
    }    

}
