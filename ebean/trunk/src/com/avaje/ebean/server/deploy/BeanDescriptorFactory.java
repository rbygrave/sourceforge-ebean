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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.NamingConvention;
import com.avaje.ebean.bean.BeanController;
import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.bean.BeanListener;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.enhance.subclass.SubClassManager;
import com.avaje.ebean.server.core.BootupClasses;
import com.avaje.ebean.server.core.ConcurrencyMode;
import com.avaje.ebean.server.deploy.DeploySqlSelectParser.Meta;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssoc;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;
import com.avaje.ebean.server.deploy.meta.DeployTableJoinColumn;
import com.avaje.ebean.server.deploy.parse.DeployBeanInfo;
import com.avaje.ebean.server.deploy.parse.DeployInheritInfoBuilder;
import com.avaje.ebean.server.deploy.parse.DeployUtil;
import com.avaje.ebean.server.deploy.parse.MissingTableException;
import com.avaje.ebean.server.deploy.parse.ReadAnnotations;
import com.avaje.ebean.server.deploy.parse.TransientProperties;
import com.avaje.ebean.server.lib.sql.ColumnInfo;
import com.avaje.ebean.server.lib.sql.DictionaryInfo;
import com.avaje.ebean.server.lib.sql.TableInfo;
import com.avaje.ebean.server.lib.util.Dnode;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.server.plugin.PluginProperties;
import com.avaje.ebean.server.reflect.BeanReflect;
import com.avaje.ebean.server.reflect.BeanReflectFactory;
import com.avaje.ebean.server.reflect.BeanReflectGetter;
import com.avaje.ebean.server.reflect.BeanReflectSetter;
import com.avaje.ebean.server.reflect.EnhanceBeanReflectFactory;
import com.avaje.ebean.server.type.TypeManager;
import com.avaje.ebean.server.validate.LengthValidatorFactory;
import com.avaje.ebean.server.validate.NotNullValidatorFactory;
import com.avaje.ebean.util.Message;

/**
 * Creates BeanDescriptors.
 */
public class BeanDescriptorFactory {

	private static final Logger logger = Logger.getLogger(BeanDescriptorFactory.class.getName());

	private final ReadAnnotations readAnnotations = new ReadAnnotations();

	private final TransientProperties transientProperties;

	/**
	 * Helper to derive inheritance information.
	 */
	private final DeployInheritInfoBuilder inheritInfoDeploy;

	private final BeanReflectFactory reflectFactory;

	private final DeployUtil deployUtil;

	/**
	 * Determines the IdentityGeneration used based on the support for
	 * getGeneratedKeys and sequences.
	 */
	private final IdentityGeneration defaultIdentityGeneration;

	/**
	 * Set to true if a Db supports Sequences.
	 */
	private final boolean supportsSequences;

	private final boolean autoAddValidators;

	private final boolean autoAddNotNullValidators;

	private final boolean autoAddLengthValidators;

	/**
	 * Stops adding length validators where length is really large (Clob).
	 */
	private final int autoMaxLength;

	private final PluginDbConfig dbConfig;

	private final DeploymentManager deploymentManager;

	private final DictionaryInfo dictionaryInfo;

	private final DeploySqlSelectParser sqlSelectParser;

	private final TypeManager typeManager;

	private boolean debugDeploy;

	private final PluginProperties properties;

	private final BeanControllerManager beanControllerManager;

	private final BeanFinderManager beanFinderManager;

	private final BeanListenerManager beanListenerManager;

	private final SubClassManager subClassManager;

	private final NamingConvention namingConvention;

	int enhancedClassCount;
	int subclassClassCount;
	HashSet<String> subclassedEntities = new HashSet<String>();

	/**
	 * Create for a given database dbConfig.
	 */
	public BeanDescriptorFactory(DeploymentManager deploymentManager, PluginDbConfig dbConfig) {

		this.subClassManager = new SubClassManager(dbConfig.getProperties());
		this.typeManager = dbConfig.getTypeManager();
		this.namingConvention = dbConfig.getNamingConvention();
		this.inheritInfoDeploy = new DeployInheritInfoBuilder(dbConfig.getProperties());
		this.deploymentManager = deploymentManager;
		this.deployUtil = deploymentManager.getDeployUtil();
		this.dbConfig = deploymentManager.getDbConfig();
		this.dictionaryInfo = dbConfig.getDictionaryInfo();

		// Databases that don't support Identity or sequences
		this.defaultIdentityGeneration = dbConfig.getDbSpecific().getDefaultIdentityGeneration();

		// Databases using Sequence rather than Identity/AutoIncrement?
		this.supportsSequences = dbConfig.getDbSpecific().isSupportsSequences();

		properties = dbConfig.getProperties();

		this.beanControllerManager = (BeanControllerManager) createManager("beanControllerManager",
			new DefaultBeanControllerManager());
		this.beanFinderManager = (BeanFinderManager) createManager("beanFinderManager", new DefaultBeanFinderManager());
		this.beanListenerManager = (BeanListenerManager) createManager("beanListenerManager",
			new DefaultBeanListenerManager());

		this.reflectFactory = createReflectionFactory();
		this.sqlSelectParser = new DeploySqlSelectParser(dbConfig);
		this.transientProperties = new TransientProperties();

		debugDeploy = properties.getPropertyBoolean("debug.deploy", false);

		boolean global = properties.getPropertyBoolean("validation", true);
		autoAddValidators = properties.getPropertyBoolean("validation.autocreate", global);
		autoAddLengthValidators = properties.getPropertyBoolean("validation.autocreate.length", autoAddValidators);
		autoMaxLength = properties.getPropertyInt("validation.autocreate.length.max", 4000);
		autoAddNotNullValidators = properties.getPropertyBoolean("validation.autocreate.notnull", autoAddValidators);

		if (!global) {
			logger.info("Validation: [off]");
		} else {
			String m = "autocreate.notnull=[" + autoAddNotNullValidators + "]";
			m += "  autocreate.length=[" + autoAddLengthValidators + " max " + autoMaxLength + "]";
			logger.info("Validation: [on] " + m);
		}
	}

	/**
	 * Create the BeanControllerFactory and BeanFinderFactory.
	 */
	private Object createManager(String name, Object defaultObject) {

		try {
			String ccn = properties.getProperty(name, null);
			if (ccn != null) {
				Class<?> clz = Class.forName(ccn);
				return clz.newInstance();
			} else {
				return defaultObject;
			}

		} catch (PersistenceException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Initialise the BeanControllers, BeanFinders and BeanListeners.
	 */
	public void initialise() {

		BootupClasses bootupClasses = dbConfig.getProperties().getBootupClasses();

		int cc = beanControllerManager.createBeanControllers(bootupClasses.getBeanControllers());
		int fc = beanFinderManager.createBeanFinders(bootupClasses.getBeanFinders());
		int lc = beanListenerManager.createBeanListeners(bootupClasses.getBeanListeners());

		logger.fine("BeanControllers[" + cc + "] BeanFinders[" + fc + "] BeanListeners[" + lc + "] ");

	}

	/**
	 * Log Warning if mixing subclass and enhancement.
	 * <p>
	 * If enhancement is used for some classes it is expected to be used for all
	 * and vice versa.
	 * </p>
	 */
	public void logStatus() {
		
		String msg = "Entities enhanced[" + enhancedClassCount + "] subclassed[" + subclassClassCount + "]";
		logger.info(msg);

		if (enhancedClassCount > 0) {
			if (subclassClassCount > 0) {
				String subclassEntityNames = subclassedEntities.toString();

				String m = "Mixing enhanced and subclassed entities. Subclassed classes:" + subclassEntityNames;
				logger.warning(m);
			}
		}
	}

	public BeanDescriptor createEmbedded(Class<?> beanClass) {

		DeployBeanInfo info = createDeployBeanInfo(beanClass);
		return new BeanDescriptor(typeManager, info.getDescriptor());
	}

	/**
	 * Create the BeanDescriptor for a type of bean.
	 * <p>
	 * Reads all the deployment information returning a BeanDescriptor.
	 * </p>
	 */
	public List<BeanDescriptor> createDescriptor(List<Class<?>> entityClasses) {

		Map<Class<?>, DeployBeanInfo> infoMap = new HashMap<Class<?>, DeployBeanInfo>();

		for (Class<?> entityClass : entityClasses) {
			DeployBeanInfo info = createDeployBeanInfo(entityClass);
			infoMap.put(entityClass, info);
		}

		// We only perform 'circular' checks etc after we have
		// all the DeployBeanDescriptors created and in the map.
		Iterator<DeployBeanInfo> it = infoMap.values().iterator();
		while (it.hasNext()) {
			DeployBeanInfo info = it.next();
			if (info.getDescriptor().isBaseTableNotFound()){
				// skip this, as it will just error badly
				
			} else {
				deriveCircularInfo(info, infoMap);
			}
		}

		List<BeanDescriptor> entityList = new ArrayList<BeanDescriptor>();
		for (DeployBeanInfo info : infoMap.values()) {
			entityList.add(new BeanDescriptor(typeManager, info.getDescriptor()));
		}

		return entityList;
	}

	/**
	 * Perform 'circular' checks such as mappedBy attributes.
	 * <p>
	 * This will also dynamically determine any table joins that have not been
	 * defined explicitly.
	 * </p>
	 */
	private void deriveCircularInfo(DeployBeanInfo info, Map<Class<?>, DeployBeanInfo> infoMap) {

		checkMappedBy(info, infoMap);

		// find any missing/undefined joins automatically
		deployUtil.defineJoins(info);
	}

	/**
	 * Check the mappedBy attribute on all the OneToMany associations for this
	 * descriptor.
	 */
	private void checkMappedBy(DeployBeanInfo info, Map<Class<?>, DeployBeanInfo> infoMap) {

		DeployBeanDescriptor desc = info.getDescriptor();

		List<DeployBeanPropertyAssocMany> manyList = desc.propertiesAssocMany();
		for (DeployBeanPropertyAssocMany manyProp : manyList) {
			if (!manyProp.isManyToMany() && !manyProp.isTransient()) {
				checkMappedBy(info, manyProp, infoMap);
			}
		}

	}

	private DeployBeanDescriptor getTargetDescriptor(DeployBeanPropertyAssocMany prop,
			Map<Class<?>, DeployBeanInfo> infoMap) {

		Class<?> targetType = prop.getTargetType();
		DeployBeanInfo info = infoMap.get(targetType);
		if (info == null) {
			String msg = "Can not find descriptor [" + targetType + "] for " + prop.getFullBeanName();
			throw new PersistenceException(msg);
		}

		return info.getDescriptor();
	}

	/**
	 * Check that the many property has either an implied mappedBy property or
	 * mark it as unidirectional.
	 */
	private boolean findMappedBy(DeployBeanPropertyAssocMany prop, Map<Class<?>, DeployBeanInfo> infoMap) {

		// this is the entity bean type - that owns this property
		Class<?> owningType = prop.getOwningType();

		Set<String> matchSet = new HashSet<String>();

		// get the bean descriptor that holds the mappedBy property
		DeployBeanDescriptor targetDesc = getTargetDescriptor(prop, infoMap);
		List<DeployBeanPropertyAssocOne> ones = targetDesc.propertiesAssocOne();
		for (DeployBeanPropertyAssocOne possibleMappedBy : ones) {
			Class<?> possibleMappedByType = possibleMappedBy.getTargetType();
			if (possibleMappedByType.equals(owningType)) {
				prop.setMappedBy(possibleMappedBy.getName());
				matchSet.add(possibleMappedBy.getName());
			}
		}

		if (matchSet.size() == 0) {
			// this is a unidirectional relationship
			// ... that is no matching property on the 'detail' bean
			return false;
		}
		if (matchSet.size() == 1) {
			// all right with the world
			return true;
		}
		if (matchSet.size() == 2) {
			// try to find a match implicitly using a common naming convention
			// e.g. List<Bug> loggedBugs; ... search for "logged" in matchSet
			String name = prop.getName();

			// get the target type short name
			String targetType = prop.getTargetType().getName();
			String shortTypeName = targetType.substring(targetType.lastIndexOf(".") + 1);

			// name includes (probably ends with) the target type short name?
			int p = name.indexOf(shortTypeName);
			if (p > 1) {
				// ok, get the 'interesting' part of the property name
				// That is the name without the target type
				String searchName = name.substring(0, p).toLowerCase();

				// search for this in the possible matches
				Iterator<String> it = matchSet.iterator();
				while (it.hasNext()) {
					String possibleMappedBy = it.next();
					String possibleLower = possibleMappedBy.toLowerCase();
					if (possibleLower.indexOf(searchName) > -1) {
						// we have a match..
						prop.setMappedBy(possibleMappedBy);

						if (debugDeploy) {
							String m = "Implicitly found mappedBy for " + targetDesc + "." + prop;
							m += " by searching for [" + searchName + "] against " + matchSet;
							logger.warning(m);
						}

						return true;
					}
				}

			}
		}
		// multiple options so should specify mappedBy property
		String msg = "Error on " + prop.getFullBeanName() + " missing mappedBy.";
		msg += " There are [" + matchSet.size() + "] possible properties in " + targetDesc;
		msg += " that this association could be mapped to. Please specify one using ";
		msg += "the mappedBy attribute on @OneToMany.";
		throw new PersistenceException(msg);
	}

	/**
	 * A OneToMany with no matching mappedBy property in the target so must be
	 * unidirectional.
	 * <p>
	 * This means that inserts MUST cascade for this property.
	 * </p>
	 * <p>
	 * Create a "Shadow"/Unidirectional property on the target. It is used with
	 * inserts to set the foreign key value (e.g. inserts the foreign key value
	 * into the order_id column on the order_lines table).
	 * </p>
	 */
	private void makeUnidirectional(DeployBeanInfo info, DeployBeanPropertyAssocMany oneToMany,
			Map<Class<?>, DeployBeanInfo> infoMap, DeployBeanDescriptor targetDesc) {

		Class<?> owningType = oneToMany.getOwningType();

		if (!oneToMany.getCascadeInfo().isSave()) {
			// The property MUST have persist cascading so that inserts work.

			Class<?> targetType = oneToMany.getTargetType();
			String msg = "Error on " + oneToMany.getFullBeanName() + ". @OneToMany MUST have ";
			msg += "Cascade.PERSIST or Cascade.ALL because this is a unidirectional ";
			msg += "relationship. That is, there is no property of type " + owningType + " on " + targetType;

			throw new PersistenceException(msg);
		}

		// mark this property as unidirectional
		oneToMany.setUnidirectional(true);

		// create the 'shadow' unidirectional property
		// which is put on the target descriptor
		DeployBeanPropertyAssocOne unidirectional = new DeployBeanPropertyAssocOne(targetDesc);
		unidirectional.setPropertyType(owningType);
		targetDesc.setUnidirectional(unidirectional);

		// specify table and table alias...
		BeanTable beanTable = deployUtil.getBeanTable(owningType);
		unidirectional.setBeanTable(beanTable);
		unidirectional.setName(beanTable.getBaseTable());

		info.setBeanJoinAlias(unidirectional, false);

		// define the TableJoin
		DeployTableJoin oneToManyJoin = oneToMany.getTableJoin();
		if (oneToManyJoin.hasJoinColumns()) {
			// inverse of the oneToManyJoin
			DeployTableJoin unidirectionalJoin = unidirectional.getTableJoin();
			DeployTableJoinColumn[] cols = oneToManyJoin.columns();
			for (int i = 0; i < cols.length; i++) {
				unidirectionalJoin.addJoinColumn(cols[i].createInverse());
			}

		} else {
			try {
				// dynamically determine from the database meta data
				deployUtil.defineJoinDynamically(targetDesc, unidirectional);

			} catch (RuntimeException e) {
				String msg = "Error on " + oneToMany.getFullBeanName() + ". Unable to find a foreign key ";
				msg += "relationship to " + targetDesc;
				throw new PersistenceException(msg, e);
				
			} catch (MissingTableException e) {
				String msg = "Making "+oneToMany.getFullBeanName()+" transient as join table not found";
				logger.log(Level.WARNING, msg, e);
			} 
		}

	}

	/**
	 * If the property has mappedBy set then do two things. Make sure the
	 * mappedBy property exists, and secondly read its join information.
	 * <p>
	 * We can use the join information from the mappedBy property and reverse it
	 * for using in the OneToMany direction.
	 * </p>
	 */
	private void checkMappedBy(DeployBeanInfo info, DeployBeanPropertyAssocMany prop,
			Map<Class<?>, DeployBeanInfo> infoMap) {

		// get the bean descriptor that holds the mappedBy property
		DeployBeanDescriptor targetDesc = getTargetDescriptor(prop, infoMap);

		if (prop.getMappedBy() == null) {
			if (!findMappedBy(prop, infoMap)) {
				makeUnidirectional(info, prop, infoMap, targetDesc);
				return;
			}
		}

		// check that the mappedBy property is valid and read
		// its associated join information if it is available
		String mappedBy = prop.getMappedBy();

		// get the mappedBy property
		DeployBeanProperty mappedProp = targetDesc.getBeanProperty(mappedBy);
		if (mappedProp == null) {

			String m = "Error on " + prop.getFullBeanName();
			m += "  Can not find mappedBy property [" + mappedBy + "] ";
			m += "in [" + targetDesc + "]";
			throw new PersistenceException(m);
		}

		if (!(mappedProp instanceof DeployBeanPropertyAssocOne)) {
			String m = "Error on " + prop.getFullBeanName();
			m += ". mappedBy property [" + mappedBy + "]is not a ManyToOne?";
			m += "in [" + targetDesc + "]";
			throw new PersistenceException(m);
		}

		DeployBeanPropertyAssocOne mappedAssocOne = (DeployBeanPropertyAssocOne) mappedProp;

		DeployTableJoin tableJoin = prop.getTableJoin();
		if (!tableJoin.hasJoinColumns()) {
			// try to defined TableJoin as the inverse of the
			// TableJoin on mappedBy property

			// read table join info on foreign mappedBy property
			// and apply to local property (with inverse)
			DeployTableJoin otherTableJoin = mappedAssocOne.getTableJoin();
			if (otherTableJoin.hasJoinColumns()) {
				// add the columns from the other join but
				// reverse the db columns
				DeployTableJoinColumn[] cols = otherTableJoin.columns();
				for (int i = 0; i < cols.length; i++) {
					tableJoin.addJoinColumn(cols[i].createInverse());
				}
			}
		}

	}

	private void setBeanControllerFinderListener(DeployBeanDescriptor descriptor) {

		Class<?> beanType = descriptor.getBeanType();

		BeanController controller = beanControllerManager.getBeanController(beanType);
		if (controller != null) {
			descriptor.setBeanController(controller);
			logger.fine("BeanController on[" + descriptor.getFullName() + "] " + controller.getClass().getName());

		}
		BeanFinder beanFinder = beanFinderManager.getBeanFinder(beanType);
		if (beanFinder != null) {
			descriptor.setBeanFinder(beanFinder);
			logger.fine("BeanFinder on[" + descriptor.getFullName() + "] " + beanFinder.getClass().getName());
		}
		BeanListener beanListener = beanListenerManager.getBeanListener(beanType);
		if (beanListener != null) {
			descriptor.setBeanListener(beanListener);
			logger.fine("BeanListener on[" + descriptor.getFullName() + "] " + beanListener.getClass().getName());
		}
	}

	/**
	 * Read all the deployment information for a given bean type.
	 */
	private DeployBeanInfo createDeployBeanInfo(Class<?> beanClass) {

		DeployBeanDescriptor desc = new DeployBeanDescriptor(deploymentManager, beanClass);

		// set bean controller, finder and listener
		setBeanControllerFinderListener(desc);

		deployUtil.createProperties(desc);

		DeployBeanInfo info = new DeployBeanInfo(deployUtil, desc);

		readAnnotations.process(info);
		inheritInfoDeploy.process(desc);

		readXml(desc);

		if (desc.isSqlSelectBased()) {
			desc.setBaseTable(null);
			desc.setBaseTableAlias(null);
		}
		
		if (desc.isMeta()){
			desc.setBaseTable(null);
			desc.setBaseTableAlias(null);			
		}

		boolean embedded = desc.isEmbedded();
		
		if (!embedded && !desc.isSqlSelectBased() && !desc.isMeta()) {
			// Entity is based on a table so check
			// that the base table exists
			String baseTable = desc.getBaseTable();
			TableInfo ti = dictionaryInfo.getTableInfo(baseTable);
			if (ti == null) {
				if (desc.getBeanFinder() != null) {
					// Using finder so could be in-memory type beans
					// like the Meta ones
				} else {
					// Base table not found. Assuming that the bean is
					// not valid for this database...
					String msg = "Error with [" + desc.getFullName() + "]  table [" + baseTable + "] not found?";
					logger.log(Level.WARNING, msg);
					desc.setBaseTableNotFound(true);
				}
			} else {
				// get column max length info etc...
				setColumnInfo(desc, ti);

				if (desc.getDependantTables() == null) {
					// Sets the baseTable as a dependent in case where it
					// hasn't been set (by Xml or annotation).
					// NB: Table dependency is used to determine cache
					// invalidation.
					String[] dep = new String[1];
					dep[0] = desc.getBaseTable();
					desc.setDependantTables(dep);
				}
			}
		}

		// mark transient properties
		transientProperties.process(desc);
		setScalarType(desc);

		if (!embedded) {
			// check to make sure bean has a Id
			if (desc.propertiesId().size() == 0 && !desc.isSqlSelectBased()) {
				if (desc.getBeanFinder() != null) {
					// using BeanFinder so perhaps valid without an id
				} else {
					// expecting at least one id property
					logger.warning(Message.msg("deploy.nouid", desc.getFullName()));
				}
			}

			if (supportsSequences && (desc.getBaseTable() != null)) {
				// Derive the SequenceNextVal and set it to the descriptor.
				// Note: the sequence only gets used *IF* the value of the
				// id property is null when inserted. This is the default
				// approach on Oracle.
				String dbSeqNextVal = desc.getSequenceNextVal();
				if (dbSeqNextVal == null) {
					String seqName = desc.getIdGeneratorName();
					if (seqName == null){
						seqName = namingConvention.getSequenceName(desc.getBaseTable());
					}
					dbSeqNextVal = namingConvention.getSequenceNextVal(seqName);
					desc.setSequenceNextVal(dbSeqNextVal);
				}
			}
			
			if (desc.getBaseTable() != null){
				// used only with Identity columns and getGeneratedKeys is not supported
				String selectLastInsertedId = namingConvention.getSelectLastInsertedId(desc.getBaseTable());
				desc.setSelectLastInsertedId(selectLastInsertedId);
			}

			IdentityGeneration idType = desc.getIdentityGeneration();
			if (IdentityGeneration.AUTO.equals(idType)) {
				desc.setIdentityGeneration(defaultIdentityGeneration);
			}
		}

		if (!embedded) {
			// find the appropriate default concurrency mode
			setConcurrencyMode(desc);
		}

		// generate the bytecode
		createByteCode(desc);

		return info;
	}

	private void createByteCode(DeployBeanDescriptor deploy) {
		// check to see if the bean supports EntityBean interface
		// generate a subclass if required
		setEntityBeanClass(deploy);

		// use Code generation or Standard reflection to support
		// getter and setter methods
		setBeanReflect(deploy);
	}

	private void setColumnInfo(DeployBeanDescriptor deployDesc, TableInfo tableInfo) {

		List<DeployBeanProperty> list = deployDesc.propertiesBase();
		for (int i = 0; i < list.size(); i++) {
			DeployBeanProperty prop = list.get(i);
			if (!prop.isTransient()) {
				if (prop.isDbWrite()) {
					String dbColumn = prop.getDbColumn();
					ColumnInfo info = tableInfo.getColumnInfo(dbColumn);
					if (info == null) {
						String msg = "Db Column " + dbColumn + " not found ";
						msg += "for property " + prop.getFullBeanName();
						logger.warning(msg);

					} else {
						prop.readColumnInfo(info);
						autoAddValidators(prop);
					}
				}
			}
		}
	}

	/**
	 * Automatically add Length and NotNull validators based on database meta
	 * data.
	 */
	private void autoAddValidators(DeployBeanProperty prop) {
		if (!autoAddValidators) {
			return;
		}
		if (autoAddLengthValidators) {
			int dbMaxLength = prop.getDbColumnSize();
			if (dbMaxLength > 0 && prop.getPropertyType().equals(String.class)) {
				if (dbMaxLength > autoMaxLength) {
					if (logger.isLoggable(Level.FINEST)) {
						String msg = "Not automatically adding length validator to " + prop.getFullBeanName();
						msg += " due to big length " + dbMaxLength + " > max " + autoMaxLength;
						logger.finest(msg);
					}
				} else {
					// check if the property already has the LengthValidator
					if (!prop.containsValidatorType(LengthValidatorFactory.LengthValidator.class)) {
						prop.addValidator(LengthValidatorFactory.create(0, dbMaxLength));
					}
				}
			}
		}
		if (autoAddNotNullValidators) {
			if (!prop.isNullable() && !prop.isId() && !prop.isGenerated()) {
				// check if the property already has the NotNullValidator
				if (!prop.containsValidatorType(NotNullValidatorFactory.NotNullValidator.class)) {
					prop.addValidator(NotNullValidatorFactory.NOT_NULL);
				}
			}
		}
	}

	/**
	 * Set the Scalar Types on all the simple types. This is done AFTER
	 * transients have been identified. This is because a non-transient field
	 * MUST have a ScalarType. It is useful for transients to have ScalarTypes
	 * because then they can be used in a SqlSelect query.
	 * <p>
	 * Enums are treated a bit differently in that they always have a ScalarType
	 * as one is built for them.
	 * </p>
	 */
	private void setScalarType(DeployBeanDescriptor deployDesc) {

		Iterator<DeployBeanProperty> it = deployDesc.propertiesAll();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();
			if (prop instanceof DeployBeanPropertyAssoc) {

			} else {
				deployUtil.setScalarType(prop);
			}
		}
	}

	private void readXml(DeployBeanDescriptor deployDesc) {

		Dnode entityXml = deploymentManager.findEntityDeploymentXml(deployDesc.getFullName());

		if (entityXml != null) {
			readXmlNamedQueries(deployDesc, entityXml);
			readXmlSql(deployDesc, entityXml);
		}
	}

	/**
	 * Read sql-select (FUTURE: additionally sql-insert, sql-update,
	 * sql-delete). If found this entity bean is based on raw sql.
	 */
	private void readXmlSql(DeployBeanDescriptor deployDesc, Dnode entityXml) {

		List<Dnode> sqlSelectList = entityXml.findAll("sql-select", entityXml.getLevel() + 1);
		for (int i = 0; i < sqlSelectList.size(); i++) {
			Dnode sqlSelect = sqlSelectList.get(i);
			readSqlSelect(deployDesc, sqlSelect);
		}
	}

	private String findContent(Dnode node, String nodeName) {
		Dnode found = node.find(nodeName);
		if (found != null) {
			return found.getNodeContent();
		} else {
			return null;
		}
	}

	private void readSqlSelect(DeployBeanDescriptor deployDesc, Dnode sqlSelect) {

		String name = sqlSelect.getStringAttr("name", "default");
		String extend = sqlSelect.getStringAttr("extend", null);
		String queryDebug = sqlSelect.getStringAttr("debug", null);
		boolean debug = (queryDebug != null && queryDebug.equalsIgnoreCase("true"));

		// the raw sql select
		String query = findContent(sqlSelect, "query");
		String where = findContent(sqlSelect, "where");
		String having = findContent(sqlSelect, "having");
		String columnMapping = findContent(sqlSelect, "columnMapping");

		Meta meta = DeploySqlSelectParser.createMeta(deployDesc, name, extend, query, debug, where, having,
			columnMapping);

		if (meta.query == null) {
			String msg = "Error with sql-select xml: <query> tag missing <entity class=\"" + deployDesc.getFullName()
					+ "\"><sql-select> ?";
			throw new PersistenceException(msg);
		}

		DeploySqlSelect parsedSql = sqlSelectParser.parse(deployDesc, meta);

		DeployNamedQuery namedQuery = new DeployNamedQuery(name, query, null, parsedSql);
		deployDesc.add(namedQuery);

	}

	/**
	 * Read named queries for this bean type.
	 */
	private void readXmlNamedQueries(DeployBeanDescriptor deployDesc, Dnode entityXml) {

		// look for named-query...
		List<Dnode> namedQueries = entityXml.findAll("named-query", 1);

		for (Dnode namedQueryXml : namedQueries) {

			String name = (String) namedQueryXml.getAttribute("name");
			Dnode query = namedQueryXml.find("query");
			if (query == null) {
				logger.warning("orm.xml " + deployDesc.getFullName() + " named-query missing query element?");

			} else {
				String oql = query.getNodeContent();
				// TODO: QueryHints not read from xml yet
				if (name == null || oql == null) {
					logger.warning("orm.xml " + deployDesc.getFullName() + " named-query has no query content?");
				} else {
					// add the named query
					DeployNamedQuery q = new DeployNamedQuery(name, oql, null);
					deployDesc.add(q);
				}
			}
		}
	}

	private BeanReflectFactory createReflectionFactory() {

		String cn = dbConfig.getProperties().getProperty("beanreflectfactory", null);
		if (cn != null) {
			try {
				Class<?> cls = Class.forName(cn);
				return (BeanReflectFactory) cls.newInstance();
			} catch (Exception ex) {
				logger.log(Level.SEVERE, null, ex);
			}
		}
		// new StandardReflectFactory();
		return new EnhanceBeanReflectFactory();
	}

	/**
	 * Set BeanReflect BeanReflectGetter and BeanReflectSetter properties.
	 * <p>
	 * This sets the implementation of constructing entity beans and the setting
	 * and getting of properties. It is generally faster to use code generation
	 * rather than reflection to do this.
	 * </p>
	 */
	private void setBeanReflect(DeployBeanDescriptor desc) {
		Class<?> beanType = desc.getBeanType();
		Class<?> factType = desc.getFactoryType();

		BeanReflect beanReflect = reflectFactory.create(beanType, factType);
		desc.setBeanReflect(beanReflect);

		try {
			Iterator<DeployBeanProperty> it = desc.propertiesAll();
			while (it.hasNext()) {
				DeployBeanProperty prop = it.next();
				String propName = prop.getName();
	
				// use reflection or generated code for getting setting
				BeanReflectGetter getter = beanReflect.getGetter(propName);
				BeanReflectSetter setter = beanReflect.getSetter(propName);
				prop.setGetter(getter);
				prop.setSetter(setter);
			}
		} catch (IllegalArgumentException e){
			Class<?> superClass = desc.getBeanType().getSuperclass();
			String msg = "Error with ["+desc.getFullName()
				+"] I believe it is not enhanced but it's superClass ["+superClass+"] is?"
				+" (You are not allowed to mix enhancement in a single inheritance hierarchy)";
			throw new PersistenceException(msg, e);
		}
		
	}

	/**
	 * DevNote: It is assumed that Embedded can contain version properties. It
	 * is also assumed that Embedded beans do NOT themselves contain Embedded
	 * beans which contain version properties.
	 */
	private void setConcurrencyMode(DeployBeanDescriptor desc) {

		int cmode = desc.getConcurrencyMode();
		if (cmode != ConcurrencyMode.ALL) {
			// concurrency mode explicitly set during deployment
			return;
		}

		if (checkForVersionProperties(desc)) {
			desc.setConcurrencyMode(ConcurrencyMode.VERSION);
		}
	}

	/**
	 * Search for version properties also including embedded beans.
	 */
	private boolean checkForVersionProperties(DeployBeanDescriptor desc) {

		boolean hasVersionProperty = false;

		List<DeployBeanProperty> props = desc.propertiesBase();
		for (int i = 0; i < props.size(); i++) {
			if (props.get(i).isVersionColumn()) {
				hasVersionProperty = true;
			}
		}

		return hasVersionProperty;
	}

	/**
	 * Test the bean type to see if it implements EntityBean natively without
	 * any Byte code enhancement.
	 */
	private void setEntityBeanClass(DeployBeanDescriptor desc) {

		if (!desc.hasDefaultConstructor()) {
			// no default constructor expected so bean must
			// be created by a BeanFinder (or externally)
			desc.setFactoryType(null);
			return;
		}

		Class<?> beanClass = desc.getBeanType();

		try {
			Object testBean = beanClass.newInstance();
			
			if (testBean instanceof EntityBean) {
				String className = beanClass.getName();
				try {
					// check that it really is enhanced (rather than mixed enhancement)
					String marker = ((EntityBean)testBean)._ebean_getMarker();
					if (!marker.equals(className)){
						String msg = "Error with ["+desc.getFullName()
							+"] It has not been enhanced but it's superClass ["+beanClass.getSuperclass()+"] is?"
							+" (You are not allowed to mix enhancement in a single inheritance hierarchy)";
						throw new PersistenceException(msg);
					}
				} catch (AbstractMethodError e){
					throw new PersistenceException("Old Ebean v1.0 enhancement detected in Ebean v1.1 - please do a clean enhancement.", e);
				}
				
				// the bean already implements EntityBean
				checkInheritedClasses(true, beanClass);

				desc.setFactoryType(beanClass);
				if (!beanClass.getName().startsWith("com.avaje.ebean.meta")) {
					enhancedClassCount++;
				}

			} else {
				checkInheritedClasses(false, beanClass);
				desc.checkReadAndWriteMethods();
				
				subclassClassCount++;

				Class<?> subClass = subClassManager.resolve(beanClass.getName());
				desc.setFactoryType(subClass);

				subclassedEntities.add(desc.getName());
			}

		} catch (PersistenceException ex){
			throw ex;
			
		} catch (Exception ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Check that the inherited classes are the same as the entity bean (aka all enhanced
	 * or all dynamically subclassed).
	 */
	private void checkInheritedClasses(boolean ensureEnhanced, Class<?> beanClass) {
		Class<?> superclass = beanClass.getSuperclass();
		if (Object.class.equals(superclass)){
			// we got to the top of the inheritance 
			return;
		}
		boolean isClassEnhanced = EntityBean.class.isAssignableFrom(superclass);
		
		if (ensureEnhanced != isClassEnhanced){
			String msg;
			if (ensureEnhanced){
				msg = "Class ["+superclass+"] is not enhanced and ["+beanClass+"] is - (you can not mix!!)";
			} else {
				msg = "Class ["+superclass+"] is enhanced and ["+beanClass+"] is not - (you can not mix!!)";				
			}
			throw new IllegalStateException(msg);
		} 

		// recursively continue up the inheritance hierarchy
		checkInheritedClasses(ensureEnhanced, superclass);
	}
	
}
