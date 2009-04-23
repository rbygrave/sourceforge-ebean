/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.avaje.ebean.server.lib.sql;

/**
 * Used to search for a given table in the dictionary.
 * <p>
 * Parses out the catalog, schema and table names.
 * </p>
 */
class TableSearchName {

	final String fullTableName;
	final String catalog;
	final String schema;
	final String tableName;

	/**
	 * Parses out the catalog, schema and table names.
	 */
	TableSearchName(String fullTableName) {

		this.fullTableName = fullTableName;

		int dotPos0 = fullTableName.indexOf('.');
		int dotPos1 = -1;
		if (dotPos0 > 0) {
			dotPos1 = fullTableName.indexOf('.', dotPos0 + 1);
		}

		if (dotPos1 > -1) {
			// format is: catalog.schema.tableName
			catalog = fullTableName.substring(0, dotPos0);
			schema = fullTableName.substring(dotPos0 + 1, dotPos1);
			tableName = fullTableName.substring(dotPos1 + 1);

		} else if (dotPos0 > -1) {
			// format is: schema.tableName
			catalog = null;
			schema = fullTableName.substring(0, dotPos0);
			tableName = fullTableName.substring(dotPos0 + 1);

		} else {
			// format is: tableName
			catalog = null;
			schema = null;
			tableName = fullTableName;
		}

	}

	public String toString() {
		return fullTableName;
	}

	String getSchema() {
		return schema;
	}

	String getTableName() {
		return tableName;
	}

}
