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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.bean.BeanController;
import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.bean.BeanListener;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.query.OrmQueryDetail;
import com.avaje.ebean.server.core.ReferenceOptions;
import com.avaje.ebean.server.deploy.id.IdBinder;
import com.avaje.ebean.server.deploy.id.IdBinderFactory;
import com.avaje.ebean.server.deploy.jointree.JoinTree;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyLists;
import com.avaje.ebean.server.query.CQueryPlan;
import com.avaje.ebean.server.reflect.BeanReflect;
import com.avaje.ebean.server.type.TypeManager;
import com.avaje.ebean.server.validate.Validator;
import com.avaje.lib.log.LogFactory;

/**
 * Describes Beans including their deployment information.
 */
public class BeanDescriptor {

	private static final Logger logger = LogFactory.get(BeanDescriptor.class);

	ConcurrentHashMap<Integer, CQueryPlan> queryPlanCache = new ConcurrentHashMap<Integer, CQueryPlan>();

	/**
	 * The EbeanServer name. Same as the plugin name.
	 */
	final String serverName;

	/**
	 * Type of Identity generation strategy used.
	 */
	final char identityGeneration;

	/**
	 * The name of an IdGenerator (optional).
	 */
	final String idGeneratorName;

	/**
	 * The database sequence name (optional).
	 */
	final String sequenceNextVal;

	/**
	 * True if this is Table based for TableBeans.
	 */
	final boolean tableGenerated;

	/**
	 * True if this is an Embedded bean.
	 */
	final boolean embedded;

	final boolean autoFetchTunable;

	/**
	 * Set for beans that don't have a default constructor and are typically
	 * built using a BeanFinder instead. The Ebean "Meta" beans are examples of
	 * this.
	 */
	final boolean defaultConstructor;

	final String lazyFetchIncludes;

	/**
	 * The concurrency mode for beans of this type.
	 */
	final int concurrencyMode;

	/**
	 * The tables this bean is dependent on.
	 */
	final String[] dependantTables;

	/**
	 * Extra deployment attributes.
	 */
	final Map<String, String> extraAttrMap;

	/**
	 * The base database table.
	 */
	final String baseTable;

	/**
	 * True if based on a table (or view) false if based on a raw sql select
	 * statement.
	 */
	final boolean sqlSelectBased;

	/**
	 * Sql table alias for the base table. Also identifies which properties are
	 * 'base table' properties.
	 */
	final String baseTableAlias;

	/**
	 * Used to provide mechanism to new EntityBean instances. Generated code
	 * faster than reflection at this stage.
	 */
	final BeanReflect beanReflect;

	/**
	 * Map of BeanProperty Linked so as to preserve order.
	 */
	final LinkedHashMap<String, BeanProperty> propMap;

	/**
	 * The type of bean this describes.
	 */
	final Class<?> beanType;

	/**
	 * This is not sent to a remote client.
	 */
	final BeanDescriptorOwner owner;

	/**
	 * The EntityBean type used to create new EntityBeans.
	 */
	final Class<?> factoryType;

	/**
	 * Intercept pre post on insert,update,delete and postLoad(). Server side
	 * only.
	 */
	final BeanController beanController;

	/**
	 * If set overrides the find implementation. Server side only.
	 */
	final BeanFinder beanFinder;

	/**
	 * Listens for post commit insert update and delete events.
	 */
	final BeanListener beanListener;

	/**
	 * The table joins for this bean.
	 */
	final TableJoin[] derivedTableJoins;

	/**
	 * Inheritance information. Server side only.
	 */
	final InheritInfo inheritInfo;

	/**
	 * Derived list of properties that make up the unique id.
	 */
	final BeanProperty[] propertiesId;

	/**
	 * Derived list of properties that are used for version concurrency
	 * checking.
	 */
	final BeanProperty[] propertiesVersion;

	/**
	 * Properties local to this type (not from a super type).
	 */
	final BeanProperty[] propertiesLocal;

	final BeanPropertyAssocOne unidirectional;

	/**
	 * A hashcode of all the many property names.
	 * This is used to efficiently create sets of 
	 * loaded property names (for partial objects).
	 */
	final int namesOfManyPropsHash;
	
	/**
	 * The set of names of the many properties.
	 */
	final Set<String> namesOfManyProps;
	
	/**
	 * list of properties that are Lists/Sets/Maps (Derived).
	 */
	final BeanPropertyAssocMany[] propertiesMany;
	final BeanPropertyAssocMany[] propertiesManySave;
	final BeanPropertyAssocMany[] propertiesManyDelete;

	/**
	 * list of properties that are associated beans and not embedded (Derived).
	 */
	final BeanPropertyAssocOne[] propertiesOne;

	final BeanPropertyAssocOne[] propertiesOneImported;
	final BeanPropertyAssocOne[] propertiesOneImportedSave;
	final BeanPropertyAssocOne[] propertiesOneImportedDelete;

	final BeanPropertyAssocOne[] propertiesOneExported;
	final BeanPropertyAssocOne[] propertiesOneExportedSave;
	final BeanPropertyAssocOne[] propertiesOneExportedDelete;

	/**
	 * list of properties that are embedded beans.
	 */
	final BeanPropertyAssocOne[] propertiesEmbedded;

	/**
	 * List of the scalar properties excluding id and secondary table properties.
	 */
	final BeanProperty[] propertiesBaseScalar;

	final BeanProperty[] propertiesTransient;

	/**
	 * Set to true if the bean has version properties or an embedded bean has
	 * version properties.
	 */
	final BeanProperty propertyFirstVersion;

	/**
	 * Set when the Id property is a single non-embedded property. Can make life
	 * simpler for this case.
	 */
	final BeanProperty propertySingleId;

	/**
	 * The bean class name or the table name for MapBeans.
	 */
	final String fullName;

	final Map<String, DeployNamedQuery> namedQueries;

	final Map<String, DeployNamedUpdate> namedUpdates;

	/**
	 * Logical to physical deployment mapping for use with updates.
	 * <p>
	 * Maps bean properties to db columns and the bean name to base table.
	 * </p>
	 */
	Map<String,String> updateDeployMap;

	/**
	 * Has local validation rules.
	 */
	final boolean hasLocalValidation;

	/**
	 * Has local or recursive validation rules.
	 */
	final boolean hasCascadeValidation;

	/**
	 * Properties with local validation rules.
	 */
	final BeanProperty[] propertiesValidationLocal;

	/**
	 * Properties with local or cascade validation rules.
	 */
	final BeanProperty[] propertiesValidationCascade;

	final Validator[] beanValidators;

	/**
	 * Flag used to determine if saves can be skipped.
	 */
	final boolean saveRecurseSkippable;

	/**
	 * Flag used to determine if deletes can be skipped.
	 */
	final boolean deleteRecurseSkippable;

	/**
	 * Make the TypeManager available for helping SqlSelect.
	 */
	final TypeManager typeManager;

	final IdBinder idBinder;

	final String name;
	
	final boolean baseTableNotFound;
	
	/**
	 * Construct the BeanDescriptor.
	 */
	public BeanDescriptor(TypeManager typeManager, DeployBeanDescriptor deploy) {

		this.baseTableNotFound = deploy.isBaseTableNotFound();
		this.name = deploy.getName();
		this.fullName = deploy.getFullName();
		this.typeManager = typeManager;
		this.owner = deploy.getOwner();
		this.beanType = deploy.getBeanType();
		this.factoryType = deploy.getFactoryType();
		this.serverName = deploy.getServerName();
		this.namedQueries = deploy.getNamedQueries();
		this.namedUpdates = deploy.getNamedUpdates();
		
		this.inheritInfo = deploy.getInheritInfo();

		this.beanFinder = deploy.getBeanFinder();
		this.beanController = deploy.getBeanController();
		this.beanListener = deploy.getBeanListener();

		this.identityGeneration = deploy.getIdentityGeneration();
		this.idGeneratorName = deploy.getIdGeneratorName();
		this.sequenceNextVal = deploy.getSequenceNextVal();
		this.tableGenerated = deploy.isTableGenerated();
		this.embedded = deploy.isEmbedded();
		this.defaultConstructor = deploy.hasDefaultConstructor();
		this.lazyFetchIncludes = deploy.getLazyFetchIncludes();
		this.concurrencyMode = deploy.getConcurrencyMode();

		this.dependantTables = deploy.getDependantTables();

		this.extraAttrMap = deploy.getExtraAttributeMap();

		this.baseTable = deploy.getBaseTable();
		this.baseTableAlias = deploy.getBaseTableAlias();
		this.sqlSelectBased = deploy.isSqlSelectBased();

		this.beanReflect = deploy.getBeanReflect();
		
		this.autoFetchTunable = !tableGenerated && !embedded && !sqlSelectBased && (beanFinder == null);

		// helper object used to derive lists of properties
		DeployBeanPropertyLists listHelper = new DeployBeanPropertyLists(owner, this, deploy);

		this.propMap = listHelper.getPropertyMap();
		this.propertiesTransient = listHelper.getTransients();
		this.propertiesBaseScalar = listHelper.getBaseScalar();
		this.propertiesId = listHelper.getId();
		this.propertiesVersion = listHelper.getVersion();
		this.propertiesEmbedded = listHelper.getEmbedded();
		this.propertiesLocal = listHelper.getLocal();
		this.unidirectional = listHelper.getUnidirectional();
		this.propertiesOne = listHelper.getOnes();
		this.propertiesOneExported = listHelper.getOneExported();
		this.propertiesOneExportedSave = listHelper.getOneExportedSave();
		this.propertiesOneExportedDelete = listHelper.getOneExportedDelete();
		this.propertiesOneImported = listHelper.getOneImported();
		this.propertiesOneImportedSave = listHelper.getOneImportedSave();
		this.propertiesOneImportedDelete = listHelper.getOneImportedDelete();

		this.propertiesMany = listHelper.getMany();
		this.propertiesManySave = listHelper.getManySave();
		this.propertiesManyDelete = listHelper.getManyDelete();

		this.namesOfManyProps = deriveManyPropNames();
		this.namesOfManyPropsHash = namesOfManyProps.hashCode();
		
		this.derivedTableJoins = listHelper.getTableJoin();
		this.propertyFirstVersion = listHelper.getFirstVersion();

		if (propertiesId.length == 1) {
			this.propertySingleId = propertiesId[0];
		} else {
			this.propertySingleId = null;
		}
		// there are no cascade save associated beans
		saveRecurseSkippable = (0 == (propertiesOneExportedSave.length
				+ propertiesOneImportedSave.length + propertiesManySave.length));

		deleteRecurseSkippable = (0 == (propertiesOneExportedDelete.length
				+ propertiesOneImportedDelete.length + propertiesManyDelete.length));

		this.propertiesValidationLocal = listHelper.getPropertiesWithValidators(false);
		this.propertiesValidationCascade = listHelper.getPropertiesWithValidators(true);
		this.beanValidators = listHelper.getBeanValidators();
		this.hasLocalValidation = (propertiesValidationLocal.length > 0 || beanValidators.length > 0);
		this.hasCascadeValidation = (propertiesValidationCascade.length > 0 || beanValidators.length > 0);

		// object used to handle Id values
		this.idBinder = IdBinderFactory.createIdBinder(propertiesId);
	}

	/**
	 * Return true if the base table for this entity bean is not found.
	 * <p>
	 * Handling this case so that Ebean will start even if some entity beans
	 * will not work.
	 * </p>
	 */
	public boolean isBaseTableNotFound() {
		return baseTableNotFound;
	}
	
	/**
	 * Initialisation after the JoinTree has been built.
	 */
	public void initialiseWithJoinTree(JoinTree joinTree) {
		initialiseNamedQueries(joinTree);
	}

	/**
	 * Initialise the Id properties first.
	 * <p>
	 * These properties need to be initialised prior to the association properties 
	 * as they are used to get the imported and exported properties.
	 * </p>
	 */
	public void initialiseId() {
		if (baseTableNotFound){
			String msg = "BeanDescriptor " + fullName+" skipping initialisation as base table ["+baseTable+"] not found";
			logger.log(Level.FINER, msg);
			return;
		}
		
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("BeanDescriptor initialise " + fullName);
		}

		if (inheritInfo != null) {
			inheritInfo.setDescriptor(this);
		}
		
		if (embedded){
			// initialise all the properties
			Iterator<BeanProperty> it = propertiesAll();
			while (it.hasNext()) {
				BeanProperty prop = it.next();
				prop.initialise();
			}
		} else {
			// initialise just the Id properties
			BeanProperty[] idProps = propertiesId();
			for (int i = 0; i < idProps.length; i++) {
				idProps[i].initialise();
			}
		}
	}
	
	/**
	 * Initialise the exported and imported parts for associated properties.
	 */
	public void initialiseOther() {
		if (baseTableNotFound){
			return;
		}

		if (!embedded){
			// initialise all the non-id properties
			Iterator<BeanProperty> it = propertiesAll();
			while (it.hasNext()) {
				BeanProperty prop = it.next();
				if (!prop.isId()){
					prop.initialise();
				}
			}
		}

		
		if (unidirectional != null) {
			unidirectional.initialise();
		}

		idBinder.initialise();

		if (tableGenerated || embedded){
			
		} else {
			// map of bean and property name to base table and db column 
			updateDeployMap = DeployUpdateMapFactory.build(this);
				
			// parse every named update up front into sql dml
			for (DeployNamedUpdate namedUpdate : namedUpdates.values()) {
				DeployUpdateParser parser = new DeployUpdateParser(updateDeployMap);
				namedUpdate.initialise(parser);
			}	
		}
	}
	
	/**
	 * Convert the logical orm update statement into sql by converting the bean properties and bean name to
	 * database columns and table.
	 */
	public String convertOrmUpdateToSql(String ormUpdateStatement) {
		DeployUpdateParser parser = new DeployUpdateParser(updateDeployMap);
		return parser.parse(ormUpdateStatement);
	}
	
	/**
	 * Currently need to initialise SqlSelect named queries if they use imported foreign keys.
	 * <p>
	 * Specifically finding the logical foreign key property.
	 * </p>
	 */
	private void initialiseNamedQueries(JoinTree joinTree) {
		Iterator<DeployNamedQuery> it = namedQueries.values().iterator();
		while (it.hasNext()) {
			DeployNamedQuery namedQuery = it.next();
			if (namedQuery.isSqlSelect()){
				namedQuery.initialise(this, joinTree);
			}
		}
	}

	/**
	 * Reset the statistics on all the query plans.
	 */
	public void resetStatistics() {
		Iterator<CQueryPlan> it = queryPlanCache.values().iterator();
		while (it.hasNext()) {
			CQueryPlan queryPlan = (CQueryPlan) it.next();
			queryPlan.resetStatistics();
		}
	}
		
	/**
	 * Return the query plans for this BeanDescriptor.
	 */
	public Iterator<CQueryPlan> queryPlans() {
		return queryPlanCache.values().iterator();
	}

	public CQueryPlan getQueryPlan(Integer key) {
		return queryPlanCache.get(key);
	}

	public void putQueryPlan(Integer key, CQueryPlan plan) {
		queryPlanCache.put(key, plan);
	}

	/**
	 * Return the TypeManager.
	 */
	public TypeManager getTypeManager() {
		return typeManager;
	}

	/**
	 * Return true if save does not recurse to other beans. That is return true
	 * if there are no assoc one or assoc many beans that cascade save.
	 */
	public boolean isSaveRecurseSkippable() {
		return saveRecurseSkippable;
	}

	/**
	 * Return true if delete does not recurse to other beans. That is return
	 * true if there are no assoc one or assoc many beans that cascade delete.
	 */
	public boolean isDeleteRecurseSkippable() {
		return deleteRecurseSkippable;
	}

	/**
	 * Return true if this type has local validation rules.
	 */
	public boolean hasLocalValidation() {
		return hasLocalValidation;
	}

	/**
	 * Return true if this type has local or cascading validation rules.
	 */
	public boolean hasCascadeValidation() {
		return hasCascadeValidation;
	}

	public InvalidValue validate(boolean cascade, Object bean) {

		if (!hasCascadeValidation) {
			// no validation rules at all on this bean
			return null;
		}

		List<InvalidValue> errList = null;

		Set<String> loadedProps = null;
		if (bean instanceof EntityBean) {
			EntityBeanIntercept ebi = ((EntityBean) bean)._ebean_getIntercept();
			loadedProps = ebi.getLoadedProps();
		}
		if (loadedProps != null) {
			// validate just the loaded properties
			Iterator<String> propIt = loadedProps.iterator();
			while (propIt.hasNext()) {
				String propName = (String) propIt.next();
				BeanProperty property = getBeanProperty(propName);

				// check if we should fire validation on this property
				if (property.hasValidationRules(cascade)) {
					Object value = property.getValue(bean);
					List<InvalidValue> errs = property.validate(cascade, value);
					if (errs != null) {
						if (errList == null) {
							errList = new ArrayList<InvalidValue>();
						}
						errList.addAll(errs);
					}
				}
			}
		} else {
			// get appropriate list of properties with validation rules
			BeanProperty[] props = cascade ? propertiesValidationCascade
					: propertiesValidationLocal;

			// validate all the properties
			for (int i = 0; i < props.length; i++) {
				BeanProperty prop = props[i];
				Object value = prop.getValue(bean);
				List<InvalidValue> errs = prop.validate(cascade, value);
				if (errs != null) {
					if (errList == null) {
						errList = new ArrayList<InvalidValue>();
					}
					errList.addAll(errs);
				}
			}
		}

		for (int i = 0; i < beanValidators.length; i++) {
			if (!beanValidators[i].isValid(bean)) {
				if (errList == null) {
					errList = new ArrayList<InvalidValue>();
				}
				errList.add(new InvalidValue(beanValidators[i], getFullName(), null, bean));
			}
		}

		if (errList == null) {
			return null;
		}

		return new InvalidValue(null, getFullName(), bean, InvalidValue.toArray(errList));
	}

	/**
	 * Return the many property included in the query or null if one is not.
	 */
	public BeanPropertyAssocMany getManyProperty(OrmQuery<?> query) {
				
		OrmQueryDetail detail = query.getDetail();
		for (int i = 0; i < propertiesMany.length; i++) {
			if (detail.includes(propertiesMany[i].getName())){
				return propertiesMany[i];
			}
		}
		
		return null;
	}
	
	/**
	 * Return the IdBinder which is helpful for handling the various types of
	 * Id.
	 */
	public IdBinder getIdBinder() {
		return idBinder;
	}

	/**
	 * Return the sql for binding an id. This is the columns with table alias
	 * that make up the id.
	 */
	public String getBindIdSql() {
		return idBinder.getBindIdSql();
	}

	public Object getOldValues(EntityBeanIntercept intercept) {
		if (intercept.isDirty()){
			return intercept.getOldValues();
		} else {
			return intercept.getOwner();
		}
	}
	
	public boolean isDirty(EntityBeanIntercept intercept) {
		
		if (intercept.isDirty()){
			return true;
		}
		
		Object bean = intercept.getOwner();
		
		// check the embedded beans if there are any
		for (int i = 0; i < propertiesEmbedded.length; i++) {
			Object v = propertiesEmbedded[i].getValue(bean);
			if (v instanceof EntityBean){
				if (((EntityBean)v)._ebean_getIntercept().isDirty()){
					return true;
				}
			} else if (v != null){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Bind the idValue to the preparedStatement.
	 * <p>
	 * This takes care of the various id types such as embedded beans etc.
	 * </p>
	 */
	public int bindId(PreparedStatement pstmt, int index, Object idValue) throws SQLException {
		return idBinder.bindId(pstmt, index, idValue);
	}

	/**
	 * Return the id as an array of scalar bindable values.
	 * <p>
	 * This 'flattens' any EmbeddedId or multiple Id property cases.
	 * </p>
	 */
	public Object[] getBindIdValues(Object idValue) {
		return idBinder.getBindValues(idValue);
	}

	/**
	 * Return a named query.
	 */
	public DeployNamedQuery getNamedQuery(String name) {
		return namedQueries.get(name);
	}

	/**
	 * Return a named update.
	 */
	public DeployNamedUpdate getNamedUpdate(String name) {
		return namedUpdates.get(name);
	}
	
	/**
	 * Create a plain vanilla object.
	 * <p>
	 * Used for EmbeddedId Bean construction.
	 * </p>
	 */
	public Object createVanillaBean() {
		return beanReflect.createVanillaBean();
	}

	/**
	 * Creates a new EntityBean without using the creation queue.
	 */
	public EntityBean createEntityBean() {
		try {
			// Note factoryType is used indirectly via beanReflect
			EntityBean eb = (EntityBean) beanReflect.createEntityBean();
			EntityBeanIntercept in = eb._ebean_getIntercept();
			in.setServerName(serverName);

			return eb;

		} catch (Exception ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Create a reference bean based on the id.
	 */
	public EntityBean createReference(Object id, Object parent, ReferenceOptions options) {

		try {

			EntityBean eb = (EntityBean) beanReflect.createEntityBean();

			EntityBeanIntercept ebi = eb._ebean_getIntercept();
			ebi.setServerName(serverName);
			if (parent != null) {
				// Special case for a OneToOne ... parent
				// needs to be added to context prior to query
				ebi.setParentBean(parent);
			}

			convertSetId(id, eb);

			// Note: not creating proxies for many's...

			ebi.setReference();

			return eb;

		} catch (Exception ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Return the BeanDescriptor of another bean type.
	 */
	public BeanDescriptor getBeanDescriptor(Class<?> otherType) {
		return owner.getBeanDescriptor(otherType);
	}

	/**
	 * Return the "shadow" property to support unidirectional relationships.
	 * <p>
	 * For bidirectional this is a real property on the bean. For unidirectional
	 * relationships we have this 'shadow' property which is not externally
	 * visible.
	 * </p>
	 */
	public BeanPropertyAssocOne getUnidirectional() {
		return unidirectional;
	}

	/**
	 * Get a property value from a bean of this type.
	 */
	public Object getValue(Object bean, String property) {
		return getBeanProperty(property).getValue(bean);
	}

	/**
	 * Set a property value to a bean of this type.
	 */
	public void setValue(Object bean, String property, Object value) {
		getBeanProperty(property).setValue(bean, value);
	}

	/**
	 * Return true if this bean type should use IdGeneration.
	 * <p>
	 * If this is false and the Id is null it is assumed that a database auto
	 * increment feature is being used to populate the id.
	 * </p>
	 */
	public boolean isUseIdGenerator() {
		return identityGeneration == IdentityGeneration.ID_GENERATOR;
	}

	/**
	 * Return the class type this BeanDescriptor describes.
	 */
	public Class<?> getBeanType() {
		return beanType;
	}

	/**
	 * Return the class type this BeanDescriptor describes.
	 */
	public Class<?> getFactoryType() {
		return factoryType;
	}

	/**
	 * Return the bean class name this descriptor is used for.
	 * <p>
	 * If this BeanDescriptor is for a table then this returns the table name
	 * instead.
	 * </p>
	 */
	public String getFullName() {
		return fullName;
	}

	/**
	 * Return the short name of the entity bean.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Summary description.
	 */
	public String toString() {
		return fullName;
	}

	/**
	 * Helper method to return the unique property. If only one property makes
	 * up the unique id then it's value is returned. If there is a concatenated
	 * unique id then a Map is built with the keys being the names of the
	 * properties that make up the unique id.
	 */
	public Object getId(Object bean) {

		if (propertySingleId != null) {
			return propertySingleId.getValue(bean);
		}

		// it is a concatenated id Not embedded
		// so return a Map
		LinkedHashMap<String, Object> idMap = new LinkedHashMap<String, Object>();
		for (int i = 0; i < propertiesId.length; i++) {

			Object value = propertiesId[i].getValue(bean);
			idMap.put(propertiesId[i].getName(), value);
		}
		return idMap;
	}

	/**
	 * Return false if the id is a simple scalar and false if it is embedded or concatenated.
	 */
	public boolean isComplexId() {
		return idBinder.isComplexId();
	}
	
	/**
	 * Return the default order by that may need to be added if a many property
	 * is included in the query.
	 */
	public String getDefaultOrderBy() {
		return idBinder.getDefaultOrderBy();
	}

	/**
	 * Convert the type of the idValue if required.
	 */
	public Object convertId(Object idValue) {
		return idBinder.convertSetId(idValue, null);
	}

	/**
	 * Convert and set the id value.
	 * <p>
	 * If the bean is not null, the id value is set to the id property of the
	 * bean after it has been converted to the correct type.
	 * </p>
	 */
	public Object convertSetId(Object idValue, Object bean) {
		return idBinder.convertSetId(idValue, bean);
	}

	/**
	 * Get a BeanProperty by its name.
	 */
	public BeanProperty getBeanProperty(String propName) {
		BeanProperty prop = (BeanProperty) propMap.get(propName);
//		if (prop == null && inheritInfo != null) {
//			return inheritInfo.getSubTypeProperty(propName);
//		}
		return prop;
	}

	/**
	 * Return the name of the server this BeanDescriptor belongs to.
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * Return true if queries for beans of this type are autoFetch tunable.
	 */
	public boolean isAutoFetchTunable() {
		return autoFetchTunable;
	}

	/**
	 * Returns the Inheritance mapping information. This will be null if this
	 * type of bean is not involved in any ORM inheritance mapping.
	 */
	public InheritInfo getInheritInfo() {
		return inheritInfo;
	}

	/**
	 * Return true if this was generated from jdbc meta data of a table. Returns
	 * false for normal beans.
	 */
	public boolean isTableGenerated() {
		return tableGenerated;
	}

	/**
	 * Return true if this is an embedded bean.
	 */
	public boolean isEmbedded() {
		return embedded;
	}

	/**
	 * Return the concurrency mode used for beans of this type.
	 */
	public int getConcurrencyMode() {
		return concurrencyMode;
	}

	/**
	 * Return the tables this bean is dependent on. This implies that if any of
	 * these tables are modified then cached beans may be invalidated.
	 */
	public String[] getDependantTables() {
		return dependantTables;
	}

	/**
	 * Return the beanListener.
	 */
	public BeanListener getBeanListener() {
		return beanListener;
	}

	/**
	 * Return the beanFinder. Usually null unless overriding the finder.
	 */
	public BeanFinder getBeanFinder() {
		return beanFinder;
	}

	/**
	 * Return the Controller.
	 */
	public BeanController getBeanController() {
		return beanController;
	}

	/**
	 * Returns true if this bean is based on a table (or possibly view) and
	 * returns false if this bean is based on a raw sql select statement.
	 * <p>
	 * When false querying this bean is based on a supplied sql select statement
	 * placed in the orm xml file (as opposed to Ebean generated sql).
	 * </p>
	 */
	public boolean isSqlSelectBased() {
		return sqlSelectBased;
	}

	/**
	 * Return the base table. Only properties mapped to the base table are by
	 * default persisted.
	 */
	public String getBaseTable() {
		return baseTable;
	}

	/**
	 * Return the base table alias.
	 */
	public String getBaseTableAlias() {
		return baseTableAlias;
	}

	
	/**
	 * Return the map of logical property names to database column names as well as the
	 * bean name to base table.
	 * <p>
	 * This is used in converting a logical update into sql.
	 * </p>
	 */
	public Map<String, String> getUpdateDeployMap() {
		return updateDeployMap;
	}

	/**
	 * Get a named extra attribute.
	 */
	public String getExtraAttribute(String key) {
		return (String) extraAttrMap.get(key);
	}

	/**
	 * Return the identity generation type.
	 */
	public char getIdentityGeneration() {
		return identityGeneration;
	}

	/**
	 * Return the sequence name with nextval wrapping.
	 */
	public String getSequenceNextVal() {
		return sequenceNextVal;
	}

	/**
	 * Return the name of the IdGenerator that should be used with this type of
	 * bean. A null value could be used to specify the 'default' IdGenerator.
	 */
	public String getIdGeneratorName() {
		return idGeneratorName;
	}

	/**
	 * Return the includes for getReference().
	 */
	public String getLazyFetchIncludes() {
		return lazyFetchIncludes;
	}

	/**
	 * Return the TableJoins.
	 * <p>
	 * For properties mapped to secondary tables rather than the base table.
	 * </p>
	 */
	public TableJoin[] tableJoins() {
		return derivedTableJoins;
	}

	/**
	 * Return an Iterator of all BeanProperty. This includes transient
	 * properties.
	 */
	public Iterator<BeanProperty> propertiesAll() {
		return propMap.values().iterator();
	}

	/**
	 * Return the BeanProperty that make up the unique id.
	 * <p>
	 * The order of these properties can be relied on to be consistent if the
	 * bean itself doesn't change or the xml deployment order does not change.
	 * </p>
	 */
	public BeanProperty[] propertiesId() {
		return propertiesId;
	}

	/**
	 * Return the transient properties.
	 */
	public BeanProperty[] propertiesTransient() {
		return propertiesTransient;
	}

	/**
	 * If the Id is a single non-embedded property then returns that, otherwise
	 * returns null.
	 */
	public BeanProperty getSingleIdProperty() {
		return propertySingleId;
	}

	/**
	 * Return the beans that are embedded. These share the base table with the
	 * owner bean.
	 */
	public BeanPropertyAssocOne[] propertiesEmbedded() {
		return propertiesEmbedded;
	}

	/**
	 * All the BeanPropertyAssocOne that are not embedded. These are effectively
	 * joined beans. For ManyToOne and OneToOne associations.
	 */
	public BeanPropertyAssocOne[] propertiesOne() {
		return propertiesOne;
	}

	/**
	 * Returns ManyToOnes and OneToOnes on the imported owning side.
	 * <p>
	 * Excludes OneToOnes on the exported side.
	 * </p>
	 */
	public BeanPropertyAssocOne[] propertiesOneImported() {
		return propertiesOneImported;
	}

	/**
	 * Imported Assoc Ones with cascade save true.
	 */
	public BeanPropertyAssocOne[] propertiesOneImportedSave() {
		return propertiesOneImportedSave;
	}

	/**
	 * Imported Assoc Ones with cascade delete true.
	 */
	public BeanPropertyAssocOne[] propertiesOneImportedDelete() {
		return propertiesOneImportedSave;
	}

	/**
	 * Returns OneToOnes that are on the exported side of a OneToOne.
	 * <p>
	 * These associations do not own the relationship.
	 * </p>
	 */
	public BeanPropertyAssocOne[] propertiesOneExported() {
		return propertiesOneExported;
	}

	/**
	 * Exported assoc ones with cascade save.
	 */
	public BeanPropertyAssocOne[] propertiesOneExportedSave() {
		return propertiesOneExportedSave;
	}

	/**
	 * Exported assoc ones with delete cascade.
	 */
	public BeanPropertyAssocOne[] propertiesOneExportedDelete() {
		return propertiesOneExportedDelete;
	}
	
	private Set<String> deriveManyPropNames() {
		
		LinkedHashSet<String> names = new LinkedHashSet<String>();
		for (int i = 0; i < propertiesMany.length; i++) {
			names.add(propertiesMany[i].getName());
		}

		return Collections.unmodifiableSet(names);
	}
	
	/**
	 * Return a hash of the names of the many properties on this bean
	 * type. This is used for efficient building of included properties
	 * sets for partial objects.
	 */
	public int getNamesOfManyPropsHash() {
		return namesOfManyPropsHash;
	}

	/**
	 * Returns the set of many property names for this bean type.
	 */
	public Set<String> getNamesOfManyProps() {
		return namesOfManyProps;
	}

	/**
	 * All Assoc Many's for this descriptor.
	 */
	public BeanPropertyAssocMany[] propertiesMany() {
		return propertiesMany;
	}

	/**
	 * Assoc Many's with save cascade.
	 */
	public BeanPropertyAssocMany[] propertiesManySave() {
		return propertiesManySave;
	}

	/**
	 * Assoc Many's with delete cascade.
	 */
	public BeanPropertyAssocMany[] propertiesManyDelete() {
		return propertiesManyDelete;
	}

	/**
	 * Return the first version property that exists on the bean. Returns null
	 * if no version property exists on the bean.
	 * <p>
	 * Note that this DOES NOT find a version property on an embedded bean.
	 * </p>
	 */
	public BeanProperty firstVersionProperty() {
		return propertyFirstVersion;
	}

	/**
	 * Returns 'Version' properties on this bean. These are 'Counter' or 'Update
	 * Timestamp' type properties. Note version properties can also be on
	 * embedded beans rather than on the bean itself.
	 */
	public BeanProperty[] propertiesVersion() {
		return propertiesVersion;
	}

	/**
	 * Scalar properties without the unique id or secondary table properties.
	 */
	public BeanProperty[] propertiesBaseScalar() {
		return propertiesBaseScalar;
	}

	/**
	 * Return the properties local to this type for inheritance.
	 */
	public BeanProperty[] propertiesLocal() {
		return propertiesLocal;
	}
}
