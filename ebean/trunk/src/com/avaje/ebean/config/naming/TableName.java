/**
 * Copyright (C) 2006  Authors
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
 * TableName holds catalog, schema and table name.
 * 
 * @author emcgreal
 */
public final class TableName {

	/** The catalog. */
	private String catalog;

	/** The schema. */
	private String schema;

	/** The name. */
	private String name;

	/**
	 * Construct with the given catalog schema and table name.
	 * <p>
	 * Note the catalog and schema can be null.
	 * </p>
	 */
	public TableName(String catalog, String schema, String name) {
		super();
		this.catalog = catalog != null ? catalog.trim() : null;
		this.schema = schema != null ? schema.trim() : null;
		this.name = name != null ? name.trim() : null;
	}

	/**
	 * Construct splitting the qualifiedTableName potentially into catalog,
	 * schema and name.
	 * <p>
	 * The qualifiedTableName can take the form of catalog.schema.tableName
	 * and is split on the '.' period character. The catalog and schema are optional. 
	 * </p>
	 * 
	 * @param qualifiedTableName
	 *            the fully qualified table name using '.' between schema and
	 *            table name etc (with catalog and schema optional).
	 */
	public TableName(String qualifiedTableName) {
		String[] split = qualifiedTableName.split("\\.");
		int len = split.length;
		if (split.length > 3) {
			String m = "Error splitting " + qualifiedTableName + ". Expecting at most 2 '.' characters";
			throw new RuntimeException(m);
		}
		if (len == 3) {
			this.catalog = split[0];
		}
		if (len >= 2) {
			this.schema = split[len-2];
		}
		this.name = split[len-1];
	}

	public String toString() {
		return getQualifiedName();
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
	 * Returns the qualified name in the form catalog.schema.name.
	 * <p>
	 * Catalog and schema are optional.
	 * </p>
	 * 
	 * @return the qualified name
	 */
	public String getQualifiedName() {
		
		StringBuilder buffer = new StringBuilder();

		// Add catalog
		if (catalog != null) {
			buffer.append(catalog);
		}

		// Add schema
		if (schema != null) {
			if (buffer.length() > 0) {
				buffer.append(".");
			}
			buffer.append(schema);
		}

		if (buffer.length() > 0) {
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
	public boolean isValid() {
		return name != null && name.length() > 0;
	}
}