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
package com.avaje.ebean.config.naming;

/**
 * The Class TableName is a simple container for catalog, schema and table name
 *
 * @author emcgreal
 */
public final class TableName{

	/** The catalog. */
	private final String catalog;

	/** The schema. */
	private final String schema;

	/** The name. */
	private final String name;

	/**
	 * @param catalog
	 * @param schema
	 * @param name
	 */
	public TableName(String catalog, String schema, String name) {
		super();
		this.catalog = catalog != null ? catalog.trim() : null;
		this.schema = schema != null ? schema.trim() : null;
		this.name = name != null ? name.trim() : null;
	}

	/**
	 * Gets the catalog.
	 *
	 * @return the catalog
	 */
	public String getCatalog() {
		return catalog;
	}

	/**
	 * Gets the schema.
	 *
	 * @return the schema
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}


	/**
	 * Gets the qualified name as catalog.schema.name
	 *
	 * @return the qualified name
	 */
	public String getQualifiedName(){
		StringBuffer buffer = new StringBuffer();

		// Add catalog
		if (catalog != null){
			buffer.append(catalog);
		}

		// Add schema
		if (schema != null){
			if (buffer.length() > 0){
				buffer.append(".");
			}
			buffer.append(schema);
		}

		if (buffer.length() > 0){
			buffer.append(".");
		}
		buffer.append(name);

		return buffer.toString();
	}

	/**
	 * Checks if is table name is valid i.e. it has at least a name.
	 *
	 * @return true, if is valid
	 */
	public boolean isValid(){
		return name != null && name.length() > 0;
	}
}