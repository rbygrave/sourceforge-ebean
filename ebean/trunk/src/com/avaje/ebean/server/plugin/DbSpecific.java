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

import com.avaje.ebean.server.ddl.DbTypeMap;
import com.avaje.ebean.server.ddl.DdlGenContext;
import com.avaje.ebean.server.ddl.DdlSyntax;
import com.avaje.ebean.server.deploy.IdentityGeneration;

/**
 * Database specific settings.
 */
public class DbSpecific {

	/**
	 * Alias used in the ROW_NUMBER function.
	 */
	protected String rowNumberWindowAlias;
			
	/**
	 * The open quote used by quoted identifiers.
	 */
	protected String openQuote;

	/**
	 * The close quote used by quoted identifiers.
	 */
	protected String closeQuote;

	/**
	 * The technique used for limiting the result set. If null this will use use
	 * the rset.absolute() method to skip fetched rows.
	 */
	protected ResultSetLimit resultSetLimit;

	/**
	 * Whether this jdbc driver supports returning generated keys for inserts.
	 */
	protected boolean supportsGetGeneratedKeys;

	/**
	 * Should default to true for databases that don't support IDENTITY
	 * auto increment or sequences. IdGenerator MUST be used.
	 */
	protected IdentityGeneration identityGeneration;

	/**
	 * Should default to true for all databases that don't support IDENTITY or
	 * auto increment. e.g. Oracle.
	 */
	protected boolean supportsSequences;

	protected DbTypeMap dbTypeMap = new DbTypeMap();
	
	protected DdlSyntax ddlSyntax = new DdlSyntax();
	
	public DbSpecific(PluginProperties properties) {

		String rl = properties.getProperty("resultSetLimit", null);
		if (rl != null) {
			resultSetLimit = ResultSetLimit.parse(rl);
		} else {
			resultSetLimit = ResultSetLimit.JdbcRowNavigation;
		}

		rowNumberWindowAlias = properties.getProperty("rowNumberWindowAlias", "as limitresult");	
		closeQuote = properties.getProperty("closequote", "\"");
		openQuote = properties.getProperty("openquote", "\"");
		
		supportsGetGeneratedKeys = properties.getPropertyBoolean("supportsGetGeneratedKeys", false);
		supportsSequences = properties.getPropertyBoolean("supportsSequences", false);
		
		String ia = properties.getProperty("identityGeneration", "auto");
		identityGeneration = IdentityGeneration.parse(ia);
	}

	public DdlGenContext createDdlGenContext() {
		return new DdlGenContext(dbTypeMap, ddlSyntax);
	}

	public String getCloseQuote() {
		return closeQuote;
	}

	public String getRowNumberWindowAlias() {
		return rowNumberWindowAlias;
	}
	
	public IdentityGeneration getIdentityGeneration() {
		return identityGeneration;
	}

	public String getOpenQuote() {
		return openQuote;
	}

	public boolean useJdbcResultSetLimit() {
		return resultSetLimit.useJdbcResultSetLimit();
	}
	
	public ResultSetLimit getResultSetLimit() {
		return resultSetLimit;
	}

	public boolean isSupportsGetGeneratedKeys() {
		return supportsGetGeneratedKeys;
	}

	public boolean isSupportsSequences() {
		return supportsSequences;
	}

	/**
	 * Determines the IdentityGeneration used based on the support for
	 * getGeneratedKeys and sequences. Refer to IdentityGeneration.
	 */
	public IdentityGeneration getDefaultIdentityGeneration() {
		if (identityGeneration != IdentityGeneration.AUTO) {
			return identityGeneration;
		}

		if (!supportsGetGeneratedKeys) {
			return IdentityGeneration.ID_GENERATOR;
		}
		if (supportsSequences) {
			return IdentityGeneration.DB_SEQUENCE;
		} else {
			return IdentityGeneration.DB_IDENTITY;
		}
	}

}
