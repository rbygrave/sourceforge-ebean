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
package com.avaje.ebean.server.deploy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.persistence.PersistenceException;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.MapBean;
import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.core.PersistRequest;
import com.avaje.ebean.server.deploy.id.ImportedId;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;
import com.avaje.ebean.util.DeploymentException;

/**
 * Property mapped to a List Set or Map.
 */
public class BeanPropertyAssocMany extends BeanPropertyAssoc {

	/**
	 * Join for manyToMany intersection table.
	 */
	final TableJoin intersectionJoin;
	
	/**
	 * For ManyToMany this is the Inverse join used to build reference queries.
	 */
	final TableJoin inverseJoin;
	
	/**
	 * Derived list of exported property and matching foreignKey
	 */
	ExportedProperty[] exportedProperties;

	/**
	 * Property on the 'child' bean that links back to the 'master'.
	 */
	BeanProperty childMasterProperty;

	boolean embeddedExportedProperties;
	
	/**
	 * Flag to indicate that this is a unidirectional relationship.
	 */
	final boolean unidirectional;
	
	/**
	 * Flag to indicate manyToMany relationship.
	 */
	final boolean manyToMany;

	final String fetchOrderBy;

	BeanProperty mapKeyProperty;

	final String mapKey;

	final String mappedBy;
	
	/**
	 * The type of the many, set, list or map.
	 */
	final ManyType manyType;

	final String serverName;
	
	String targetTablePrefix;
	
	BeanCollectionHelp help;

	ImportedId importedId;

	/**
	 * Create this property.
	 */
	public BeanPropertyAssocMany(BeanDescriptorOwner owner, BeanDescriptor descriptor, DeployBeanPropertyAssocMany deploy) {
		super(owner, descriptor, deploy);
		this.unidirectional = deploy.isUnidirectional();
		this.manyToMany = deploy.isManyToMany();
		this.serverName = descriptor.getServerName();
		this.manyType = deploy.getManyType();
		this.mapKey = deploy.getMapKey();
		this.fetchOrderBy = deploy.getFetchOrderBy();
		this.mappedBy = deploy.getMappedBy();
		
		DeployTableJoin intJoin = deploy.getIntersectionTableJoin();
		if (intJoin != null){
			this.intersectionJoin = new TableJoin(intJoin, null);
		} else {
			this.intersectionJoin = null;
		}
		DeployTableJoin invJoin = deploy.getInverseJoin();
		if (invJoin != null){
			this.inverseJoin = new TableJoin(invJoin, null);
		} else {
			this.inverseJoin = null;
		}
	}
		
	public void initialise() {
		super.initialise();
		if (!isTransient){
			targetTablePrefix = targetTableAlias+".";

			help = BeanCollectionHelpFactory.create(this);
			
			if (manyToMany){
				// only manyToMany's have imported properties
				importedId = createImportedId(this, targetDescriptor, tableJoin);

			} else {
				// find the property in the many that matches
				// back to the master (Order in the OrderDetail bean)
				childMasterProperty = initChildMasterProperty();			
			}
			
			if (mapKey != null){
				mapKeyProperty = initMapKeyProperty();
			}
			
			exportedProperties = createExported();
			if (exportedProperties.length > 0){
				embeddedExportedProperties = exportedProperties[0].isEmbedded();
			}
		}
	}
	
	@Override
	public void appendSelect(DbSqlContext ctx) {
	}
	@Override
	public Object readSet(DbReadContext ctx, Object bean, Class<?> type) throws SQLException {
		return null;
	}
	@Override
	public boolean isValueLoaded(Object value) {
		if (value instanceof BeanCollection<?>){
			return ((BeanCollection<?>)value).isPopulated();
		}
		return true;
	}
	
	public void add(BeanCollection<?> collection, Object bean, String mapKey) {
		help.add(collection, bean, mapKey);
	}
	
	@Override
	public InvalidValue validateCascade(Object manyValue) {
		
		ArrayList<InvalidValue> errs = help.validate(targetDescriptor, manyValue);
		
		if (errs == null){
			return null;
		} else {
			return new InvalidValue("recurse.many", targetDescriptor.getFullName(), manyValue, InvalidValue.toArray(errs));
		}
	}
	
	/**
	 * Refresh the appropriate list set or map.
	 */
	public void refresh(EbeanServer server, Query<?> query, Transaction t, BeanPropertyAssocMany many, Object parentBean) {
		help.refresh(server, query, t, many, parentBean);
	}
	
	/**
	 * Return the many type.
	 */
	public ManyType getManyType() {
		return manyType;
	}

	/**
	 * Return true if this is many to many.
	 */
	public boolean isManyToMany() {
		return manyToMany;
	}
	
	/**
	 * ManyToMany only, join from local table to intersection table.
	 */
	public TableJoin getIntersectionTableJoin() {
		return intersectionJoin;
	}
	
	/**
	 * Set the join properties from the parent bean to the child bean.
	 * This is only valid for OneToMany and NOT valid for ManyToMany.
	 */
	public void setJoinValuesToChild(PersistRequest request, Object parent, Object child, Object mapKeyValue) {
		
		if (mapKeyProperty != null){
			mapKeyProperty.setValue(child, mapKeyValue);
		}
		
		if (!manyToMany){
			if (childMasterProperty != null){
				// bidirectional in the sense that the 'master' property
				// exists on the 'detail' bean
				childMasterProperty.setValue(child, parent);
			} else {
				// unidirectional in the sense that the 'master' property
				// does NOT exist on the 'detail' bean
			}
		}
	}

	/**
	 * Return the order by clause used to order the fetching of the data for
	 * this list, set or map.
	 */
	public String getFetchOrderBy() {
		return fetchOrderBy;
	}

	/**
	 * Return the default mapKey when returning a Map.
	 */
	public String getMapKey() {
		return mapKey;
	}

	public void createReference(Object parentBean, ObjectGraphNode profilePoint) {
		
		BeanCollection<?> ref = help.createReference(parentBean, serverName, name, profilePoint);

		setValue(parentBean, ref);
	}
	
	public BeanCollection<?> createEmpty() {
		
		return help.createEmpty();
	}

	public void setPredicates(Query<?> query, Object parentBean) {
		
		if (manyToMany){
			// for ManyToMany lazy loading we need to include a
			// join to the intersection table. The predicate column
			// is not on the 'destination many table'.
			OrmQuery<?> iq = (OrmQuery<?>)query;
			iq.setIncludeTableJoin(inverseJoin);
		}
		
		BeanProperty[] uids = descriptor.propertiesId();

		ExportedProperty[] expProps = getExported();
		if (embeddedExportedProperties) {
			// use the EmbeddedId object instead of the parentBean
			parentBean = uids[0].getValue(parentBean);
		}
		
		for (int i = 0; i < expProps.length; i++) {
			Object val = expProps[i].getValue(parentBean);
			String fkColumn = expProps[i].getForeignDbColumn();

			query.where().eq(targetTablePrefix+fkColumn, val);
		}
		
		if (extraWhere != null){
			query.where().raw(extraWhere);
		}
	}
		
	private ExportedProperty[] getExported() {
		if (exportedProperties == null) {
			exportedProperties = createExported();
			embeddedExportedProperties = exportedProperties[0].isEmbedded();
		}
		return exportedProperties;
	}

	/**
	 * Create the array of ExportedProperty used to build reference objects.
	 */
	private ExportedProperty[] createExported() {

		BeanProperty[] uids = descriptor.propertiesId();

		ArrayList<ExportedProperty> list = new ArrayList<ExportedProperty>();

		if (uids.length == 1 && uids[0].isEmbedded()) {

			BeanPropertyAssocOne one = (BeanPropertyAssocOne) uids[0];
			BeanDescriptor targetDesc = one.getTargetDescriptor();
			BeanProperty[] emIds = targetDesc.propertiesBaseScalar();
			for (int i = 0; i < emIds.length; i++) {
				ExportedProperty expProp = findMatch(true, emIds[i]);
				list.add(expProp);
			}

		} else {
			for (int i = 0; i < uids.length; i++) {
				ExportedProperty expProp = findMatch(false, uids[i]);
				list.add(expProp);	
			}
		}

		return (ExportedProperty[]) list.toArray(new ExportedProperty[list.size()]);
	}

	/**
	 * Find the matching foreignDbColumn for a given local property.
	 */
	private ExportedProperty findMatch(boolean embedded,BeanProperty prop) {

		String matchColumn = prop.getDbColumn();

		String searchTable;
		TableJoinColumn[] columns;
		if (manyToMany){
			// look for column going to intersection
			columns = intersectionJoin.columns();
			searchTable = intersectionJoin.getTable();
			
		} else {
			columns = tableJoin.columns();
			searchTable = tableJoin.getTable();
		}
		for (int i = 0; i < columns.length; i++) {
			String matchTo = columns[i].getLocalDbColumn();
			
			if (matchColumn.equalsIgnoreCase(matchTo)) {
				String foreignCol = columns[i].getForeignDbColumn();
				if (manyToMany){
					// we use the inverseJoin alias as thats the join
					// we will use for the reference find
					String refQuery = inverseJoin.getForeignTableAlias()+"." + foreignCol;
					return new ExportedProperty(embedded, refQuery, prop, foreignCol);
					
				} else {
					
					return new ExportedProperty(embedded, foreignCol, prop);
				}
			}
		}

		String msg = "Matching property for ["+prop.getFullBeanName()+"] not found in table["+searchTable+"]?";
		throw new DeploymentException(msg);
	}

	/**
	 * Return the child property that links back to the master bean.
	 * <p>
	 * Note that childMasterProperty will be null if a field is used instead of
	 * a ManyToOne bean association.
	 * </p>
	 */
	private BeanProperty initChildMasterProperty() {

		if (unidirectional){
			return null;
		}
		
		// search for the property, to see if it exists
		Class<?> beanType = descriptor.getBeanType();
		BeanDescriptor targetDesc = getTargetDescriptor();

		BeanPropertyAssocOne[] ones = targetDesc.propertiesOne();
		for (int i = 0; i < ones.length; i++) {

			BeanPropertyAssocOne prop = (BeanPropertyAssocOne) ones[i];
			if (mappedBy != null){
				// match using mappedBy as property name
				if (mappedBy.equalsIgnoreCase(prop.getName())) {
					return prop;
				}
			} else {
				// assume only one property that matches parent object type
				if (prop.getTargetType().equals(beanType)) {
					// found it, stop search
					return prop;
				}
			}
		}

		String msg = "Can not find Master [" + beanType + "] in Child[" + targetDesc + "]";
		throw new RuntimeException(msg);
	}

	/**
	 * Search for and return the mapKey property.
	 */
	private BeanProperty initMapKeyProperty() {

		// search for the property
		
		BeanDescriptor targetDesc = getTargetDescriptor();

		Iterator<BeanProperty> it = targetDesc.propertiesAll();
		while (it.hasNext()){
			BeanProperty  prop = it.next();
			if (mapKey.equalsIgnoreCase(prop.getName())) {
				return prop;
			}	
		}

		String from = descriptor.getFullName();
		String to = targetDesc.getFullName();
		String msg = from+": Could not find mapKey property ["+mapKey+"] on ["+to+"]";
		throw new PersistenceException(msg);
	}
	
	public Object buildManyToManyMapBean(Object parent, Object other) {
		
		MapBean mapBean = new MapBean();
		mapBean.setTableName(intersectionJoin.getTable());
		buildExport(mapBean, parent);
		buildImport(mapBean, other);
		return mapBean;
	}
	
	/**
	 * Set the predicates for lazy loading of the association.
	 * Handles predicates for both OneToMany and ManyToMany.
	 */
	private void buildExport(MapBean mapBean, Object parentBean) {

		BeanProperty[] uids = descriptor.propertiesId();

		ExportedProperty[] expProps = getExported();
		if (embeddedExportedProperties) {
			parentBean = uids[0].getValue(parentBean);
		}
		for (int i = 0; i < expProps.length; i++) {
			Object val = expProps[i].getValue(parentBean);
			String fkColumn = expProps[i].getIntersectionDbColumn();

			mapBean.set(fkColumn, val);
		}
	}
	
	/**
	 * Set the predicates for lazy loading of the association.
	 * Handles predicates for both OneToMany and ManyToMany.
	 */
	private void buildImport(MapBean mapBean, Object otherBean) {

		importedId.buildImport(mapBean, otherBean);
	}

		
}
