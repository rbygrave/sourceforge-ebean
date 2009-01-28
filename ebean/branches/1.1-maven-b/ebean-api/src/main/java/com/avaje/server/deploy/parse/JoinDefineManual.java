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
package com.avaje.ebean.server.deploy.parse;

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssoc;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;
import com.avaje.ebean.server.deploy.meta.DeployTableJoinColumn;
import com.avaje.ebean.server.lib.sql.ColumnInfo;
import com.avaje.ebean.server.lib.sql.DictionaryInfo;
import com.avaje.ebean.server.lib.sql.TableInfo;
import com.avaje.ebean.server.lib.util.StringHelper;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.server.util.Assert;

/**
 * Used to define joins without using Foreign key information.
 * <p>
 * Defines join information using annotations and or xml deployment information
 * and the primary key information.
 * </p>
 */
public class JoinDefineManual {

	private final DictionaryInfo dictionaryInfo;

	public JoinDefineManual(PluginDbConfig dbConfig) {
		this.dictionaryInfo = dbConfig.getDictionaryInfo();
	}

	/**
	 * Define the join information without refering to DB foreign keys.
	 * <p>
	 * This is used when Join information is supplied in the form of annotations
	 * and or xml.
	 * </p>
	 */
	public void define(JoinDefineManualInfo joinInfo) {

		DeployBeanPropertyAssoc prop = joinInfo.getProperty();
		if (prop == null) {
			defineSecondaryTable(joinInfo);

		} else if (prop instanceof DeployBeanPropertyAssocOne) {
			defineAssocOne(joinInfo);

		} else if (prop instanceof DeployBeanPropertyAssocMany) {
			defineAssocMany(joinInfo);

		} else {
			throw new IllegalStateException("Unexpected type? " + prop.getClass().getName());
		}
	}

	private void defineSecondaryTable(JoinDefineManualInfo joinInfo) {

		// TODO: defineSecondaryTable join info...
	}

	private void defineAssocMany(JoinDefineManualInfo joinInfo) {

		DeployTableJoin tableJoin = joinInfo.getTableJoin();
		DeployTableJoinColumn[] columns = tableJoin.columns();
		Assert.isTrue(columns.length > 0, "Expected some columns");

		if (columns.length == 1) {
			DeployTableJoinColumn column = columns[0];

			if (column.hasNullColumn()) {
				String localTable = joinInfo.getDescriptor().getBaseTable();
				TableInfo tableInfo = dictionaryInfo.getTableInfo(localTable);
				Assert.notNull(tableInfo, "TableInfo for "+localTable+" not found?");

				// the column set is always the foreign one so really
				// should be referencedColumnName but just getting the
				// non-null one instead and assuming its the foreign one
				String foreignColumn = column.getNonNullColumn();
				column.setForeignDbColumn(foreignColumn);

				// determine the primary key for the missing column...
				ColumnInfo[] pkCols = tableInfo.getKeyColumns();
				Assert.isTrue(pkCols.length == 1, "Expected single column PK");

				// pk is the localDbColumn for OneToMany type joins
				column.setLocalDbColumn(pkCols[0].getName());
			
			}
		}

		// Must always be an outer join...
		tableJoin.setType(TableJoin.LEFT_OUTER);
	}

	/**
	 * Define for a ManyToOne to OneToOne.
	 */
	private void defineAssocOne(JoinDefineManualInfo joinInfo) {

		// FIXME: Not really handling OneToOne here... as one of
		// the OneToOnes is really a OneToMany (with a max of one)

		DeployTableJoin tableJoin = joinInfo.getTableJoin();
		String tableName = tableJoin.getTable();

		TableInfo tableInfo = dictionaryInfo.getTableInfo(tableName);
		if (tableInfo == null){
			String msg = "Could not find table ["+tableName+"] for "+joinInfo.getDescriptor()+"."+joinInfo.getProperty();
			throw new PersistenceException(msg);
		}

		DeployTableJoinColumn[] columns = tableJoin.columns();
		Assert.isTrue(columns.length > 0, "Expected some columns");

		if (columns.length == 1) {
			DeployTableJoinColumn column = columns[0];

			if (StringHelper.isNull(column.getForeignDbColumn())) {
				// get primary key...
				ColumnInfo[] pkCols = tableInfo.getKeyColumns();
				Assert.isTrue(pkCols.length == 1, "Expected single column PK");

				// defaults to the primary key
				column.setForeignDbColumn(pkCols[0].getName());
			}
		}

		// determine outer join based on nullable DB columns
		DeployBeanDescriptor desc = joinInfo.getDescriptor();
		TableInfo localTableInfo = dictionaryInfo.getTableInfo(desc.getBaseTable());
		for (int i = 0; i < columns.length; i++) {
			String localColumn = columns[i].getLocalDbColumn();
			ColumnInfo colInfo = localTableInfo.getColumnInfo(localColumn);
			if (colInfo == null) {
				String m = "Could not find column[" + localColumn + "] in table["
						+ localTableInfo.getName() + "] when deploying [" + joinInfo.getDebugName() + "] ";
				
				throw new PersistenceException(m);
			}
			if (colInfo.isNullable()) {
				// if any of the columns are nullable then the
				// join must be an outer join
				tableJoin.setType(TableJoin.LEFT_OUTER);
			}
		}

	}

}
