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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Meta data about a database table.
 */
public class TableInfo implements Serializable {

	static final long serialVersionUID = -5727921165784116926L;

	private static final Logger logger = Logger.getLogger(TableInfo.class.getName());

	private static final Fkey[] EMPTY_FKEY = new Fkey[0];

	/**
	 * set to true if references have been loaded.
	 */
	boolean fkeysLoaded = false;

	boolean primaryAlsoForeignKey;

	Fkey[] exportedKeys;

	Fkey[] importedKeys;

	/**
	 * Map of ColumnInfo.
	 */
	LinkedHashMap<String, ColumnInfo> columnMap = new LinkedHashMap<String, ColumnInfo>();

	/**
	 * The pk columns.
	 */
	ColumnInfo[] keyColumns;

	/**
	 * All the columns.
	 */
	ColumnInfo[] columns;

	/**
	 * The catalog name.
	 */
	String catalog;

	/**
	 * the schema name.
	 */
	String schema;

	/**
	 * The table name.
	 */
	String tableName;

	/**
	 * the full table name.
	 */
	String fullName;

	/**
	 * The type being table, view etc.
	 */
	String tableType;

	/**
	 * table comments.
	 */
	String remarks;

	String typeCat;

	String typeSchem;

	String typeName;

	String selfReferencingColumnName;

	String refGeneration;

	/**
	 * Create the meta data for a table.
	 */
	public TableInfo(TableSearch tableSearch, boolean loadKeys) {

		ResultSet rset = tableSearch.getResultSet();

		try {
			ResultSetMetaData meta = rset.getMetaData();
			int columnCount = meta.getColumnCount();

			this.catalog = rset.getString(1);
			this.schema = rset.getString(2);
			this.tableName = rset.getString(3);
			this.tableType = rset.getString(4);
			if (columnCount >= 5) {
				this.remarks = rset.getString(5);
			}
			if (columnCount >= 8) {
				this.typeCat = rset.getString(6);
				this.typeSchem = rset.getString(7);
				this.typeName = rset.getString(8);
			}
			if (columnCount >= 10) {
				this.selfReferencingColumnName = rset.getString(9);
				this.refGeneration = rset.getString(10);
			}
			initFullName();

		} catch (SQLException e) {
			throw new DataSourceException(e);
		}

		// load columns and primary key info
		initAllColumns(tableSearch.getMetaData());
		initPrimayKeyColumns(tableSearch.getMetaData());

		if (loadKeys) {
			// load exported and imported keys...
			loadForeignKeys(tableSearch);
		}
	}

//	private void readObject(java.io.ObjectInputStream in) throws IOException,
//			ClassNotFoundException {
//
//		in.defaultReadObject();
//		logger = LogFactory.get(TableInfo.class);
//	}

	/**
	 * Return summary information about the table.
	 */
	public String toString() {
		return getName();
	}

	private void initFullName() {
		fullName = tableName;
		if (schema != null) {
			fullName = schema + "." + fullName;
		}
		if (catalog != null) {
			fullName = catalog + "." + fullName;
		}
	}

	/**
	 * Return the full table name.
	 * 
	 */
	public String getFullName() {
		return fullName;
	}

	/**
	 * Return the schema name.
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * Return the table name.
	 */
	public String getName() {
		return tableName;
	}

	/**
	 * specifies how values in SELF_REFERENCING_COL_NAME are created. Values are
	 * "SYSTEM", "USER", "DERIVED". (may be null).
	 */
	public String getRefGeneration() {
		return refGeneration;
	}

	/**
	 * explanatory comment on the table.
	 */
	public String getRemarks() {
		return remarks;
	}

	/**
	 * name of the designated "identifier" column of a typed table. May be null.
	 */
	public String getSelfReferencingColumnName() {
		return selfReferencingColumnName;
	}

	/**
	 * table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL
	 * TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
	 */
	public String getTableType() {
		return tableType;
	}

	/**
	 * the types catalog.
	 */
	public String getTypeCatalog() {
		return typeCat;
	}

	/**
	 * the types name.
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * the types schema.
	 */
	public String getTypeSchema() {
		return typeSchem;
	}

	/**
	 * Return a simple description of the table.
	 */
	public String describe() {
		StringBuffer sb = new StringBuffer();
		sb.append("table: ").append(schema).append(".").append(tableName).append(";");

		for (int j = 0; j < columns.length; j++) {
			sb.append(columns[j].getName() + "; ");
		}
		return sb.toString();
	}

	/**
	 * Return true if the primary key is also part of a foreign key.
	 * <p>
	 * This would typically identify a intersection table of a many to many
	 * relationship.
	 * </p>
	 */
	public boolean isPrimaryAlsoForeignKey() {
		loadForeignKeys(null);
		return primaryAlsoForeignKey;
	}

	/**
	 * Returns true if the only columns on this table are the primary key
	 * columns. This suggests this table is a pure intersection table between
	 * two other tables.
	 */
	public boolean hasOnlyPrimaryKey() {
		return keyColumns.length == columns.length;
	}

	/**
	 * Return the primary key columns.
	 */
	public ColumnInfo[] getKeyColumns() {
		return keyColumns;
	}

	/**
	 * Return all of the columns.
	 */
	public ColumnInfo[] getColumns() {
		return columns;
	}

	/**
	 * Return a ColumnInfo for a given column name.
	 */
	public ColumnInfo getColumnInfo(String columnName) {

		String key = DictionaryInfo.stripQuotesLowerCase(columnName);
		return (ColumnInfo) columnMap.get(key);
	}

	/**
	 * Return the import reference tables.
	 */
	public Fkey[] getImportedFkeys() {
		loadForeignKeys(null);
		return importedKeys;
	}

	/**
	 * Return the exported reference tables.
	 */
	public Fkey[] getExportedFkeys() {
		loadForeignKeys(null);
		return exportedKeys;
	}

	/**
	 * The list of imported foreign key relationships to a particular table.
	 */
	public List<Fkey> getImportedFkeys(String tableName) {

		tableName = DictionaryInfo.stripQuotesLowerCase(tableName);

		ArrayList<Fkey> list = new ArrayList<Fkey>();
		Fkey[] keys = getImportedFkeys();
		for (int i = 0; i < keys.length; i++) {
			Fkey key = keys[i];
			if (key.getTableName().equalsIgnoreCase(tableName)) {
				list.add(key);
			}
		}
		return list;
	}

	/**
	 * The list of exported foreign key relationships to a particular table.
	 * <p>
	 * This returns 'detail' or 'child' relationships.
	 * </p>
	 */
	public List<Fkey> getExportedFkeys(String tableName) {

		tableName = DictionaryInfo.stripQuotesLowerCase(tableName);

		ArrayList<Fkey> list = new ArrayList<Fkey>();
		Fkey[] keys = getExportedFkeys();
		for (int i = 0; i < keys.length; i++) {
			Fkey key = keys[i];
			if (key.getTableName().equalsIgnoreCase(tableName)) {
				list.add(key);
			}
		}
		return list;
	}

	/**
	 * Initialise the column information. Includes primary key fetch.
	 */
	private void initAllColumns(DatabaseMetaData metaData) {

		ResultSet rset = null;
		try {

			rset = metaData.getColumns(catalog, schema, tableName, null);
			while (rset.next()) {
				ColumnInfo col = new ColumnInfo(rset);
				String key = DictionaryInfo.stripQuotesLowerCase(col.getName());

				columnMap.put(key, col);
			}

			Collection<ColumnInfo> c = columnMap.values();
			columns = (ColumnInfo[]) c.toArray(new ColumnInfo[c.size()]);

		} catch (SQLException e) {
			throw new DataSourceException(e);
		} finally {
			if (rset != null) {
				try {
					rset.close();
				} catch (SQLException e) {
					logger.log(Level.SEVERE, null, e);
				}
			}
		}
	}

	private void initPrimayKeyColumns(DatabaseMetaData metaData) {

		ResultSet keysRset = null;
		try {
			ArrayList<ColumnInfo> pkList = new ArrayList<ColumnInfo>();

			keysRset = metaData.getPrimaryKeys(catalog, schema, tableName);
			while (keysRset.next()) {
				String colName = keysRset.getString(4);
				int colPos = keysRset.getInt(5);

				ColumnInfo ci = getColumnInfo(colName);
				ci.setPrimaryKeyPosition(colPos);

				pkList.add(ci);
			}

			keyColumns = (ColumnInfo[]) pkList.toArray(new ColumnInfo[pkList.size()]);

		} catch (SQLException e) {
			throw new DataSourceException(e);

		} finally {
			if (keysRset != null) {
				try {
					keysRset.close();
				} catch (SQLException e) {
					logger.log(Level.SEVERE, null, e);
				}
			}
		}
	}

	protected void loadForeignKeys(TableSearch tableSearch) {

		if (fkeysLoaded) {
			return;
		}

		try {
			Connection conn = tableSearch.getConnection();

			DatabaseMetaData metaData = conn.getMetaData();
			loadFKeys(metaData);

		} catch (SQLException e) {
			throw new DataSourceException(e);
		}
	}

	/**
	 * Find the inverse Fkey.
	 */
	public Fkey findInverse(Fkey otherFkey) {

		Fkey[] fkeys;
		if (otherFkey.isExported()) {
			fkeys = getImportedFkeys();
		} else {
			fkeys = getExportedFkeys();
		}

		for (int i = 0; i < fkeys.length; i++) {
			if (fkeys[i].matchInverse(otherFkey)) {
				return fkeys[i];
			}
		}
		return null;
	}

	private Fkey addToFkey(FkeyColumn col, boolean isExported, HashMap<String, Fkey> map) {
		String fkName = col.getFkName();
		if (fkName == null) {
			// fkName = col.getTableName();
			throw new RuntimeException("Can not currently handle without a ForeignKey name?");
		}

		Fkey fkey = (Fkey) map.get(fkName);
		if (fkey == null) {
			fkey = new Fkey(fkName, col.getTableName(), isExported);
			map.put(fkName, fkey);
		}
		fkey.addColumn(col);
		return fkey;
	}

	private void loadFKeys(DatabaseMetaData metaData) {

		if (fkeysLoaded) {
			return;
		}
		
		if (!tableType.equalsIgnoreCase("TABLE")){
			// bypass trying to load foreign keys and unique indexes
			// for VIEWS etc (anything that isn't a TABLE)
			exportedKeys = EMPTY_FKEY;
			importedKeys = EMPTY_FKEY;
			fkeysLoaded = true;
			return;
		}

		ResultSet rsetExported = null;
		ResultSet rsetImported = null;
		try {
			rsetExported = metaData.getExportedKeys(catalog, schema, tableName);
			rsetImported = metaData.getImportedKeys(catalog, schema, tableName);

			HashMap<String, Fkey> exportedMap = new HashMap<String, Fkey>();
			while (rsetExported.next()) {
				FkeyColumn fkCol = new FkeyColumn(true, rsetExported);
				addToFkey(fkCol, true, exportedMap);
			}

			HashMap<String, Fkey> importedMap = new HashMap<String, Fkey>();
			while (rsetImported.next()) {
				FkeyColumn fkCol = new FkeyColumn(false, rsetImported);
				Fkey fkey = addToFkey(fkCol, false, importedMap);

				ColumnInfo columnInfo = getColumnInfo(fkCol.getFkColumnName());

				if (columnInfo == null){
					// Can happen when e.g. rename a column but don't drop the FK 
					// This inconsistency can occur in MySQL 
					logger.log(Level.SEVERE, 
						"There is a Foreign key " + fkey.getFkName() + 
						" that references a column " +
						fkCol.getFkColumnName()  +
						" that no longer exists on table " + fkey.getTableName());
				}else{
					columnInfo.setForeignKey(true);

					if (columnInfo.isNullable()) {
						fkey.setImportNullable(true);
					}
	
					// primary key columns already determined
					if (columnInfo.isPrimaryKey()) {
						fkey.setPrimaryKey(true);
					}
				}
			}

			// convert into arrays...
			Collection<Fkey> expColl = exportedMap.values();
			if (expColl.size() == 0){
				exportedKeys = EMPTY_FKEY;
			} else {
				exportedKeys = (Fkey[]) expColl.toArray(new Fkey[expColl.size()]);
			}
			
			Collection<Fkey> impColl = importedMap.values();
			if (impColl.size() == 0){
				importedKeys = EMPTY_FKEY;
			} else {
				importedKeys = (Fkey[]) impColl.toArray(new Fkey[impColl.size()]);
			}
			
			if (exportedKeys.length == 0 && importedKeys.length ==0){
				logger.info("Dictionary: Table ["+fullName+"] has no imported or exported foreign keys.");
			}
			
			// check primary key columns to see if they are also
			// part of a foreign key (implying an intersection table)
			for (int i = 0; i < keyColumns.length; i++) {
				if (keyColumns[i].isForeignKey()) {
					primaryAlsoForeignKey = true;
				}
			}

			IndexMeta[] uniqueIndices = loadUniqueIndexes(metaData);
			setUniqueImported(uniqueIndices, importedKeys);

			fkeysLoaded = true;

		} catch (SQLException e) {
			throw new DataSourceException(e);

		} finally {
			if (rsetExported != null) {
				try {
					rsetExported.close();
				} catch (SQLException e) {
					logger.log(Level.SEVERE, null, e);
				}
			}
			if (rsetImported != null) {
				try {
					rsetImported.close();
				} catch (SQLException e) {
					logger.log(Level.SEVERE, null, e);
				}
			}
		}
	}

	/**
	 * Determine if any of the imported Fkeys are mapped to unique constraints
	 * via the indicies.
	 * <p>
	 * Used to determine one to one relationships.
	 * </p>
	 */
	private void setUniqueImported(IndexMeta[] indices, Fkey[] importedFkeys) {

		for (int i = 0; i < importedFkeys.length; i++) {
			for (int j = 0; j < indices.length; j++) {
				if (indices[j].matches(importedFkeys[i])) {
					importedFkeys[i].setImportUnique(true);
					break;
				}
			}
		}
	}

	/**
	 * Create array of unique indexes.
	 * <p>
	 * The goal is to set imported unique to true on appropriate imported fkeys.
	 * This is used to identify one to one type relationships.
	 * </p>
	 */
	private IndexMeta[] loadUniqueIndexes(DatabaseMetaData metaData) {

		ResultSet rset = null;
		try {
			boolean searchUnique = true;
			boolean approximate = true;

			rset = metaData.getIndexInfo(catalog, schema, tableName, searchUnique, approximate);

			HashMap<String, IndexMeta> indexMap = new HashMap<String, IndexMeta>();
			while (rset.next()) {
				addIndexMeta(rset, indexMap);
			}

			Collection<IndexMeta> c = indexMap.values();
			return (IndexMeta[]) c.toArray(new IndexMeta[c.size()]);

		} catch (SQLException e) {
			throw new DataSourceException(e);

		} finally {
			if (rset != null) {
				try {
					rset.close();
				} catch (SQLException e) {
					logger.log(Level.SEVERE, null, e);
				}
			}
		}
	}

	private void addIndexMeta(ResultSet rset, HashMap<String, IndexMeta> indexMap)
			throws SQLException {

		// String catalog = rset.getString(1);
		// String schema = rset.getString(2);
		// String tableName = rset.getString(3);
		// boolean nonUnique = rset.getBoolean(4);

		// String idxQualifier = rset.getString(5);
		String idxName = rset.getString(6);
		// short idxType = rset.getShort(7);
		// short ordPos = rset.getShort(8);
		String colName = rset.getString(9);
		// String ascd = rset.getString(10);
		// int cardinality = rset.getInt(11);
		// int pages = rset.getInt(12);
		// String filterCond = rset.getString(13);

		if (idxName == null || colName == null) {
			// String m = "idxName["+idxName+"] colName["+colName+"]
			// tableName["+tableName+"] idxType["+idxType+"]";
			// Log.warning(m);
			return;
		}
		IndexMeta index = (IndexMeta) indexMap.get(idxName);
		if (index == null) {
			index = new IndexMeta(idxName);
			indexMap.put(idxName, index);
		}
		index.addColumn(colName);
	}

	/**
	 * Helper class used to setImportedUnique(true) on imported foreign keys.
	 */
	static class IndexMeta {

		String name;

		HashSet<String> columnSet = new HashSet<String>();

		IndexMeta(String name) {
			this.name = name;
		}

		String getName() {
			return name;
		}

		boolean matches(Fkey fkey) {
			FkeyColumn[] keyColumns = fkey.columns();
			if (keyColumns.length != columnSet.size()) {
				return false;
			}
			for (int i = 0; i < keyColumns.length; i++) {
				String keyCol = keyColumns[i].getFkColumnName().toLowerCase();
				if (!columnSet.contains(keyCol)) {
					return false;
				}
			}
			return true;
		}

		void addColumn(String column) {
			columnSet.add(column.toLowerCase());
		}
	}
}
