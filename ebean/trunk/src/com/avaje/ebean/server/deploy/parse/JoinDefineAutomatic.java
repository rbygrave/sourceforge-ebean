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

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.NamingConvention;
import com.avaje.ebean.server.deploy.BeanTable;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;
import com.avaje.ebean.server.lib.sql.DictionaryInfo;
import com.avaje.ebean.server.lib.sql.Fkey;
import com.avaje.ebean.server.lib.sql.IntersectionInfo;
import com.avaje.ebean.server.lib.sql.TableInfo;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.util.DeploymentException;
import com.avaje.ebean.util.Message;

/**
 * Defines any table joins that have not been explicitly set.
 * <p>
 * This uses DB foreign keys and such to determine the join information
 * between entity beans. 
 * </p>
 * <p>
 * This includes joins for Assoc Ones, Assoc Manys and Secondary tables,
 * </p>
 */
public class JoinDefineAutomatic {

	private static final Logger logger = Logger.getLogger(JoinDefineAutomatic.class.getName());
	
	NamingConvention namingConvention;

	String manyToManyAlias;

	DictionaryInfo dictionaryInfo;

	/**
	 * Configure for the plugin.
	 */
	public JoinDefineAutomatic(PluginDbConfig dbConfig) {
	
		this.namingConvention = dbConfig.getNamingConvention();
		this.dictionaryInfo = dbConfig.getDictionaryInfo();
		
		// this alias is used for ManyToMany lazy loading queries
		String key = "manytomany.intersection.alias";
		manyToManyAlias = dbConfig.getProperties().getProperty(key, "zzzzzz");
	}

	/**
	 * Defines table joins that have not been explicitly set.
	 */
	public void process(DeployBeanInfo<?> info) {
		defineJoins(info);
	}

	/**
	 * Define a join given an explicit localDbColumn to join.
	 */
	public void define(JoinDefineAutomaticInfo params) throws MissingTableException {
		
		defineJoinDynamically(params);
	}

	/**
	 * Define any joins dynamically that have not been set.
	 */
	private void defineJoins(DeployBeanInfo<?> info) {

		DeployBeanDescriptor<?> desc = info.getDescriptor();

		String baseTable = desc.getBaseTable();
		if (baseTable == null || baseTable.trim().length() == 0) {
			// assume this is an Embedded Class
			return;
		}

		// any secondary tables
		List<DeployTableJoin> tableJoins = desc.getTableJoins();
		
		for (int i = 0; i < tableJoins.size(); i++) {

			DeployTableJoin tableJoin = tableJoins.get(i);
			if (!tableJoin.hasJoinColumns()) {
				JoinDefineAutomaticInfo params = new JoinDefineAutomaticInfo(desc, tableJoin);
				try {
					defineJoinDynamically(params);
				} catch (MissingTableException e){
					// can't really handle this
					String msg = "Secondary Table not found to join from "+desc.getFullName();
					throw new RuntimeException(msg, e);
				}
			}
		}

		// associated one properties (not embedded)
		List<DeployBeanPropertyAssocOne<?>> assocOnes = desc.propertiesAssocOne();
		for (int i = 0; i < assocOnes.size(); i++) {
			DeployBeanPropertyAssocOne<?> propBean = assocOnes.get(i);
			if (!propBean.isEmbedded() && !propBean.isTransient()){
				try {
					defineJoinDynamically(desc, propBean);
					
				} catch (MissingTableException e){
					makePropertyTransient(propBean, e.getTableName());
				}
			} 		
		}

		List<DeployBeanPropertyAssocMany<?>> assocManys = desc.propertiesAssocMany();
		for (int i = 0; i < assocManys.size(); i++) {
			DeployBeanPropertyAssocMany<?> propList = assocManys.get(i);
			if (!propList.isTransient()){
				
				DeployTableJoin tableJoin = propList.getTableJoin();
				if (tableJoin.hasJoinColumns()) {
					// join columns already defined in by user 
					// just check that the table exists
					TableInfo joinTable = dictionaryInfo.getTableInfo(tableJoin.getTable());
					if (joinTable == null){
						// the table to join to can not be found
						makePropertyTransient(propList, tableJoin.getTable());
					}
					
				} else {
					if (propList.isManyToMany()) {
						defineManyToMany(propList, desc, info);
					
					} else {
						// try to find the join columns automatically using
						// the database dictionary
						JoinDefineAutomaticInfo params = new JoinDefineAutomaticInfo(desc, propList);
						try {
							defineJoinDynamically(params);
						} catch (MissingTableException e){
							makePropertyTransient(propList, e.getTableName());
						}
					}
				}
			}
		}
	}

	private void makePropertyTransient(DeployBeanProperty prop, String tableName) {
		
		String msg = "Making property ["+prop.getFullBeanName()
					+"] transient due to join table ["+tableName+"] not found";

		logger.log(Level.WARNING, msg);
		prop.setTransient(true);
	}
	
	public void defineJoinDynamically(DeployBeanDescriptor<?> desc, DeployBeanPropertyAssocOne<?> propBean)
		throws MissingTableException {
		
		DeployTableJoin tableJoin = propBean.getTableJoin();
		if (!tableJoin.hasJoinColumns()) {
			JoinDefineAutomaticInfo params = new JoinDefineAutomaticInfo(desc, propBean);
			defineJoinDynamically(params);
		}
		if (tableJoin.isOuterJoin()) {
			propBean.setOuterJoin(true);
		}

		if (tableJoin.isImportedPrimaryKey()) {
			propBean.setImportedPrimaryKey(true);
		}
	}
	
	/**
	 * Define ManyToMany TableJoins. This finds the intersection table and sets
	 * up the joins appropriately. There are two TableJoins for a ManyToMany
	 * plus a third set up as the inverse. BaseTable to Intersection,
	 * Intersection to OtherTable, and the inverse one of OtherTable to
	 * Intersection.
	 */
	private void defineManyToMany(DeployBeanPropertyAssocMany<?> prop, DeployBeanDescriptor<?> desc, DeployBeanInfo<?> info) {

		if (prop.getIntersectionTableJoin() != null){
			// skip as already defined manually using a JoinTable annotation
			return;
		}
		String localTable = desc.getBaseTable();
		BeanTable manyBeanTable = prop.getBeanTable();
		String manyTable = manyBeanTable.getBaseTable();

		try {
			IntersectionInfo xInfo = dictionaryInfo.findIntersection(localTable, manyTable);
			if (xInfo == null) {
				String m = "No intersection table for [" + localTable;
				m += "] and [" + manyTable + "]";
				throw new PersistenceException(m);
			}

			// set the intersection table
			DeployTableJoin intJoin = new DeployTableJoin();
			String intTableName = xInfo.getIntersection().getName();
			intJoin.setTable(intTableName);

			// add the source to intersection join columns
			Fkey sourceExportKey = xInfo.getSourceExportedKey();
			intJoin.addColumns(sourceExportKey);

			// set the intersection to dest table join columns
			DeployTableJoin destJoin = prop.getTableJoin();
			Fkey destImpKey = xInfo.getIntersectionImportedKey();
			destJoin.addColumns(destImpKey);

			// set table alias etc for the join to intersection
			info.setManyIntersectionAlias(prop, intJoin);

			// set the intersection alias to the destJoin
			String intAlias = intJoin.getForeignTableAlias();
			destJoin.setLocalTableAlias(intAlias);

			// reverse join from dest back to intersection
			DeployTableJoin inverseDest = destJoin.createInverse();
			inverseDest.setTable(intTableName);
			// try to make sure we don't get a tableAlias clash
			inverseDest.setLocalTableAlias(prop.getBeanTable().getBaseTableAlias());

			// zzzzzz is very likely to be a unique table alias.
			// you'd have to have a nesting of 6 beans that had names
			// starting with z. Pretty unlikely but of course possible.
			inverseDest.setForeignTableAlias(manyToManyAlias);

			prop.setIntersectionTableJoin(intJoin);
			prop.setInverseJoin(inverseDest);

		} catch (SQLException ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * determine the join dynamically using DictionaryInfo.
	 */
	private void defineJoinDynamically(JoinDefineAutomaticInfo params) throws MissingTableException {

		String baseTable = params.desc.getBaseTable();

		TableInfo tableInfo = dictionaryInfo.getTableInfo(baseTable);
		if (tableInfo == null) {
			throw new RuntimeException("No tableInfo found for [" + baseTable + "]");
		}

		String foreignTableName = params.tableJoin.getTable();
		TableInfo foreignTableInfo = dictionaryInfo.getTableInfo(foreignTableName);
		if (foreignTableInfo == null) {
			throw new MissingTableException("Join table[" + foreignTableName + "] not found", foreignTableName);
		}
		
		if (params.isOneToOne()){
			findMatchOneToOne(params, tableInfo, foreignTableName);
		} else {
			findMatch(params, tableInfo, foreignTableName);
		}
	}

	private void findMatch(JoinDefineAutomaticInfo params, TableInfo tableInfo, String foreignTableName) {
		
		List<Fkey> fkeyList;
		if (!params.isImported) {
			fkeyList = tableInfo.getExportedFkeys(foreignTableName);
		} else {
			fkeyList = tableInfo.getImportedFkeys(foreignTableName);
		}

		if (fkeyList.size() == 0) {
			throw new DeploymentException(Message.msg("deploy.fk.none",  params.getErrorArray()));
		}

		if (fkeyList.size() == 1) {
			Fkey fkey = (Fkey) fkeyList.get(0);
			params.setMatch(fkey);
			return;
		}

		// there are many relationships between these tables
		// I need to try and select the 'correct' one from the list...
		if (!params.findMatch(fkeyList, namingConvention)) {
			throw new DeploymentException(Message.msg("deploy.fk.notfound", params.getErrorArray()));
		}
	}
	

	/**
	 * Need to search both imported and exported foreign keys.
	 */
	private void findMatchOneToOne(JoinDefineAutomaticInfo params, TableInfo tableInfo, String foreignTableName) {

		List<Fkey> expList = tableInfo.getExportedFkeys(foreignTableName);
		List<Fkey> impList = tableInfo.getImportedFkeys(foreignTableName);
		
		boolean exp = expList.size() == 1;
		boolean imp = impList.size() == 1;
		
		if (exp && !imp){
			Fkey fkey = (Fkey) expList.get(0);
			params.setMatch(fkey);

			// check to see if this should be a LEFT OUTER using
			// the inverse Fkey as we are on 'non-owning' side of OneToOne
			Fkey inverse = dictionaryInfo.findInverse(fkey);
			if (inverse.isImportNullable()) {
				params.getTableJoin().setType(TableJoin.LEFT_OUTER);
				params.getProp().setOuterJoin(true);
			}

			// identify this side as the 'non-owning' side.
			// it does not have the foreign key
			DeployBeanPropertyAssocOne<?> prop = (DeployBeanPropertyAssocOne<?>)params.getProp();
			prop.setOneToOneExported(true);			
			return;
		}
		if (imp && !exp){
			Fkey fkey = (Fkey) impList.get(0);
			params.setMatch(fkey);
			return;
		}

		String m = "Unable to find join for OneToOne.";
		m += "["+expList.size()+"] matching expFkeys";
		m += "["+impList.size()+"] matching impFkeys";
		
		throw new PersistenceException(m);
	}
}
