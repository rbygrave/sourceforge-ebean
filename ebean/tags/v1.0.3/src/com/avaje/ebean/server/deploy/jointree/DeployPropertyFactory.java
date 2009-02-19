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
package com.avaje.ebean.server.deploy.jointree;

import java.util.ArrayList;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.id.ImportedId;
import com.avaje.ebean.server.plugin.PluginDbConfig;

/**
 * Creates the List of DeployProperty for a given node.
 */
public class DeployPropertyFactory {

	private final String tableAliasPlaceHolder;
	
	public DeployPropertyFactory(PluginDbConfig dbConfig){
		tableAliasPlaceHolder = dbConfig.getTableAliasPlaceHolder();
	}
	
	/**
	 * Create DeployProperties for an assoc one or many bean.
	 */
	public DeployPropertyRequest createDeployProperties(BeanDescriptor desc) {
		return createDeployProperties(desc, null, desc.getBaseTableAlias(), true);
	}

	/**
	 * Create DeployProperties with a given propertyPrefix.
	 */
	public DeployPropertyRequest createDeployProperties(BeanDescriptor desc, String propertyPrefix, String tablePrefix) {
		return createDeployProperties(desc, propertyPrefix, tablePrefix, false);
	}
	
	/**
	 * Create the list of properties for this node.
	 */
	private DeployPropertyRequest createDeployProperties(BeanDescriptor desc, String propertyPrefix, String tablePrefix, boolean root) {

		DeployPropertyRequest request = new DeployPropertyRequest(desc, propertyPrefix, tablePrefix);
		
		addId(request);
		addBase(request);
		addEmbedded(request);
		addForeignKeys(request);
		
		return request;
	}

	private void addForeignKeys(DeployPropertyRequest request) {

		BeanPropertyAssocOne[] imported = request.desc.propertiesOneImported();
		for (int i = 0; i < imported.length; i++) {
			BeanPropertyAssocOne assocOneImported = imported[i];
			ImportedId importedId = assocOneImported.getImportedId();
			if (importedId == null){
				System.out.println("erorr here rob.");
			} 
			if (importedId.isScalar()) {
				if (!request.desc.isSqlSelectBased()){
					addForeignKey(request, assocOneImported.getName(), importedId.getDbColumn(), request.propertyPrefix);				
				} 
			}
		}
	}
	
	/**
	 * Add the Id property(s) to the List handling embeddedId's.
	 */
	private void addId(DeployPropertyRequest request) {
		
		
		BeanProperty[] uids = request.desc.propertiesId();
		
		if (uids.length == 1){
    		if (uids[0].isEmbedded()){
    			// embedded concatenated Id
				BeanPropertyAssocOne embId = (BeanPropertyAssocOne)uids[0];
				
				String prefix = embId.getName();
				if (request.propertyPrefix != null){
					prefix = request.propertyPrefix +"."+prefix;
				}
				
				BeanDescriptor emIdDesc = embId.getTargetDescriptor();
				
	        	BeanProperty[] emProps = emIdDesc.propertiesBaseScalar();
	        	for (int i = 0; i < emProps.length; i++) {        		
					addProp(request, emProps[i], prefix);
				}
    		} else {
    			addProp(request, uids[0], request.propertyPrefix);
    		}    		
    		
		} else {    	
			// multiple scalar Id properties (not embedded)
			for (int i = 0; i < uids.length; i++) {
				addProp(request, uids[i], request.propertyPrefix);
			}
		}
	}
	
	/**
	 * Add the base scalar properties.
	 */
	private void addBase(DeployPropertyRequest request) {

    	BeanProperty[] props = request.desc.propertiesBaseScalar();
    	for (int i = 0; i < props.length; i++) {        		
				
			// get the logical property name
			addProp(request, props[i], request.propertyPrefix);			
		}
	}
	
	/**
	 * Add embedded properties.
	 */
	private void addEmbedded(DeployPropertyRequest request) {
		
	
		BeanPropertyAssocOne[] embedded = request.desc.propertiesEmbedded();
        for (int j = 0; j < embedded.length; j++) {

        	BeanPropertyAssocOne embedProp = embedded[j];

			String prefix = embedProp.getName();
			if (request.propertyPrefix != null){
				prefix = request.propertyPrefix +"."+prefix;
			}
			
			BeanProperty[] props = embedProp.getProperties();
	    	for (int i = 0; i < props.length; i++) {        		
	        	addProp(request, props[i], prefix);
			}
		}
	}
	
	/**
	 * Add a individual property with the appropriate prefixes.
	 */
	private void addProp(DeployPropertyRequest request, BeanProperty prop, String propertyPrefix) {
		
		String propName = prop.getName();
		if (propertyPrefix != null) {
			propName = propertyPrefix + "." + propName;
		}

		String deployFullName;
		if (request.tablePrefix != null){
			deployFullName = prop.getDeploymentName(request.tablePrefix, tableAliasPlaceHolder);
			
		} else {
			// SqlSelect based and Map beans 
			deployFullName = prop.getDbColumn();
			if (prop.getDbTableAlias() != null) {
				// for beanProperty based on sql-select query
				deployFullName = prop.getDbTableAlias() +"."+ deployFullName;
			} 
		}

		boolean foreignKey = false;
		if (prop.isId()){
			foreignKey =  propertyPrefix != null;
		}
		
		request.add(new PropertyDeploy(foreignKey, propName, deployFullName));
	}

	private static void addForeignKey(DeployPropertyRequest request, String propName, String dbColumn, String propertyPrefix) {
		
		if (propertyPrefix != null) {
			propName = propertyPrefix + "." + propName;
		}

		String deployFullName = dbColumn;//prop.getDbColumn();
		if (request.tablePrefix != null){
			deployFullName = request.tablePrefix +"."+ deployFullName;
		} 
		
		// unconfirmed as could be OneToMany etc...
		request.addForeignKey(new PropertyDeploy(false, propName, deployFullName));
	}

	
	public static class DeployPropertyRequest {
		
		private final ArrayList<PropertyDeploy> fkeyList;
		private final ArrayList<PropertyDeploy> list;
		private final String propertyPrefix;
		private final String tablePrefix;
		private final BeanDescriptor desc;
		
		DeployPropertyRequest(BeanDescriptor desc, String propertyPrefix, String tablePrefix){
			this.desc = desc;
			this.propertyPrefix = propertyPrefix;
			this.tablePrefix = tablePrefix;
			list = new ArrayList<PropertyDeploy>();
			fkeyList = new ArrayList<PropertyDeploy>();
		}
		void add(PropertyDeploy prop){
			list.add(prop);
		}
		void addForeignKey(PropertyDeploy prop){
			fkeyList.add(prop);
		}
		
		public PropertyDeploy[] getProperties() {
			return (PropertyDeploy[])list.toArray(new PropertyDeploy[list.size()]);
		}
		public PropertyDeploy[] getForeignKeys() {
			return (PropertyDeploy[])fkeyList.toArray(new PropertyDeploy[fkeyList.size()]);
		}
		
	}
}
