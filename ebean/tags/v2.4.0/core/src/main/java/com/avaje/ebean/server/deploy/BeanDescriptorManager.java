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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import com.avaje.ebean.BackgroundExecutor;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.cache.ServerCacheManager;
import com.avaje.ebean.config.EncryptKey;
import com.avaje.ebean.config.EncryptKeyManager;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.NamingConvention;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.dbplatform.DbIdentity;
import com.avaje.ebean.config.dbplatform.IdGenerator;
import com.avaje.ebean.config.dbplatform.IdType;
import com.avaje.ebean.event.BeanFinder;
import com.avaje.ebean.internal.SpiEbeanServer;
import com.avaje.ebean.internal.TransactionEventTable;
import com.avaje.ebean.server.core.BootupClasses;
import com.avaje.ebean.server.core.ConcurrencyMode;
import com.avaje.ebean.server.core.InternString;
import com.avaje.ebean.server.core.InternalConfiguration;
import com.avaje.ebean.server.core.Message;
import com.avaje.ebean.server.deploy.BeanDescriptor.EntityType;
import com.avaje.ebean.server.deploy.id.IdBinder;
import com.avaje.ebean.server.deploy.id.IdBinderEmbedded;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssoc;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.meta.DeployBeanTable;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;
import com.avaje.ebean.server.deploy.parse.DeployBeanInfo;
import com.avaje.ebean.server.deploy.parse.DeployCreateProperties;
import com.avaje.ebean.server.deploy.parse.DeployInherit;
import com.avaje.ebean.server.deploy.parse.DeployUtil;
import com.avaje.ebean.server.deploy.parse.ReadAnnotations;
import com.avaje.ebean.server.deploy.parse.TransientProperties;
import com.avaje.ebean.server.idgen.UuidIdGenerator;
import com.avaje.ebean.server.lib.util.Dnode;
import com.avaje.ebean.server.reflect.BeanReflect;
import com.avaje.ebean.server.reflect.BeanReflectFactory;
import com.avaje.ebean.server.reflect.BeanReflectGetter;
import com.avaje.ebean.server.reflect.BeanReflectSetter;
import com.avaje.ebean.server.reflect.EnhanceBeanReflectFactory;
import com.avaje.ebean.server.subclass.SubClassManager;
import com.avaje.ebean.server.subclass.SubClassUtil;
import com.avaje.ebean.server.type.TypeManager;
import com.avaje.ebean.validation.factory.LengthValidatorFactory;
import com.avaje.ebean.validation.factory.NotNullValidatorFactory;

/**
 * Creates BeanDescriptors.
 */
public class BeanDescriptorManager implements BeanDescriptorMap {

    private static final Logger logger = Logger.getLogger(BeanDescriptorManager.class.getName());

    private static final BeanDescComparator beanDescComparator = new BeanDescComparator();

    private final ReadAnnotations readAnnotations = new ReadAnnotations();

    private final TransientProperties transientProperties;

    /**
     * Helper to derive inheritance information.
     */
    private final DeployInherit deplyInherit;

    private final BeanReflectFactory reflectFactory;

    private final DeployUtil deployUtil;

    private final TypeManager typeManager;

    private final PersistControllerManager persistControllerManager;

    private final BeanFinderManager beanFinderManager;

    private final PersistListenerManager persistListenerManager;

    private final BeanQueryAdapterManager beanQueryAdapterManager;

    private final SubClassManager subClassManager;

    private final NamingConvention namingConvention;

    private final DeployCreateProperties createProperties;

    private final DeployOrmXml deployOrmXml;

    private final BeanManagerFactory beanManagerFactory;

    private int enhancedClassCount;
    private int subclassClassCount;
    private final HashSet<String> subclassedEntities = new HashSet<String>();

    private final boolean updateChangesOnly;

    private final BootupClasses bootupClasses;

    private final String serverName;

    private Map<Class<?>, DeployBeanInfo<?>> deplyInfoMap = new HashMap<Class<?>, DeployBeanInfo<?>>();

    private final Map<Class<?>, BeanTable> beanTableMap = new HashMap<Class<?>, BeanTable>();

    private final Map<String, BeanDescriptor<?>> descMap = new HashMap<String, BeanDescriptor<?>>();

    private final Map<String, BeanManager<?>> beanManagerMap = new HashMap<String, BeanManager<?>>();

    private final Map<String, List<BeanDescriptor<?>>> tableToDescMap = new HashMap<String, List<BeanDescriptor<?>>>();

    private List<BeanDescriptor<?>> immutableDescriptorList;

    private final DbIdentity dbIdentity;

    private final DataSource dataSource;

    private final DatabasePlatform databasePlatform;

    private final UuidIdGenerator uuidIdGenerator = new UuidIdGenerator();

    private final ServerCacheManager cacheManager;

    private final BackgroundExecutor backgroundExecutor;

    private final int dbSequenceBatchSize;

    private final EncryptKeyManager encryptKeyManager;

    /**
     * Create for a given database dbConfig.
     */
    public BeanDescriptorManager(InternalConfiguration config) {

        this.serverName = InternString.intern(config.getServerConfig().getName());
        this.cacheManager = config.getCacheManager();
        this.dbSequenceBatchSize = config.getServerConfig().getDatabaseSequenceBatchSize();
        this.backgroundExecutor = config.getBackgroundExecutor();
        this.dataSource = config.getServerConfig().getDataSource();
        this.encryptKeyManager = config.getServerConfig().getEncryptKeyManager();
        this.databasePlatform = config.getServerConfig().getDatabasePlatform();
        this.bootupClasses = config.getBootupClasses();
        this.createProperties = config.getDeployCreateProperties();
        this.subClassManager = config.getSubClassManager();
        this.typeManager = config.getTypeManager();
        this.namingConvention = config.getServerConfig().getNamingConvention();
        this.dbIdentity = config.getDatabasePlatform().getDbIdentity();
        this.deplyInherit = config.getDeployInherit();
        this.deployOrmXml = config.getDeployOrmXml();
        this.deployUtil = config.getDeployUtil();

        this.beanManagerFactory = new BeanManagerFactory(config.getServerConfig(), config.getDatabasePlatform());

        this.updateChangesOnly = config.getServerConfig().isUpdateChangesOnly();

        this.persistControllerManager = new PersistControllerManager(bootupClasses);
        this.persistListenerManager = new PersistListenerManager(bootupClasses);
        this.beanQueryAdapterManager = new BeanQueryAdapterManager(bootupClasses);

        this.beanFinderManager = new DefaultBeanFinderManager();

        this.reflectFactory = createReflectionFactory();
        this.transientProperties = new TransientProperties();
    }

    @SuppressWarnings("unchecked")
    public <T> BeanDescriptor<T> getBeanDescriptor(Class<T> entityType) {

        // remove $$EntityBean stuff
        String className = SubClassUtil.getSuperClassName(entityType.getName());
        return (BeanDescriptor<T>) descMap.get(className);
    }

    @SuppressWarnings("unchecked")
    public <T> BeanDescriptor<T> getBeanDescriptor(String entityClassName) {

        // remove $$EntityBean stuff
        entityClassName = SubClassUtil.getSuperClassName(entityClassName);
        return (BeanDescriptor<T>) descMap.get(entityClassName);
    }

    public String getServerName() {
        return serverName;
    }

    public ServerCacheManager getCacheManager() {
        return cacheManager;
    }

    public NamingConvention getNamingConvention() {
        return namingConvention;
    }

    /**
     * Set the internal EbeanServer instance to all BeanDescriptors.
     */
    public void setEbeanServer(SpiEbeanServer internalEbean) {
        for (BeanDescriptor<?> desc : immutableDescriptorList) {
            desc.setEbeanServer(internalEbean);
        }
    }

    public void deploy() {

        try {
            createListeners();
            readEmbeddedDeployment();
            readEntityDeploymentInitial();
            readEntityBeanTable();
            readEntityDeploymentAssociations();
            readInheritedIdGenerators();

            // creates the BeanDescriptors
            readEntityRelationships();
            readRawSqlQueries();

            List<BeanDescriptor<?>> list = new ArrayList<BeanDescriptor<?>>(descMap.values());
            Collections.sort(list, beanDescComparator);
            immutableDescriptorList = Collections.unmodifiableList(list);

            initialiseAll();
            readForeignKeys();

            readTableToDescriptor();

            logStatus();

            deplyInfoMap.clear();
            deplyInfoMap = null;
        } catch (RuntimeException e) {
            String msg = "Error in deployment";
            logger.log(Level.SEVERE, msg, e);
            throw e;
        }
    }

    /**
     * Return the Encrypt key given the table and column name.
     */
    public EncryptKey getEncryptKey(String tableName, String columnName) {
        return encryptKeyManager.getEncryptKey(tableName, columnName);
    }

    /**
     * For SQL based modifications we need to invalidate appropriate parts of
     * the cache.
     */
    public void cacheNotify(TransactionEventTable.TableIUD tableIUD) {

        List<BeanDescriptor<?>> list = tableToDescMap.get(tableIUD.getTable());
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                list.get(i).cacheNotify(tableIUD);
            }
        }
    }

    /**
     * Build a map of table names to BeanDescriptors.
     * <p>
     * This is generally used to maintain caches from table names.
     * </p>
     */
    private void readTableToDescriptor() {

        for (BeanDescriptor<?> desc : descMap.values()) {
            String baseTable = desc.getBaseTable();
            if (baseTable == null) {

            } else {
                baseTable = baseTable.toUpperCase();

                List<BeanDescriptor<?>> list = tableToDescMap.get(baseTable);
                if (list == null) {
                    list = new ArrayList<BeanDescriptor<?>>(1);
                    tableToDescMap.put(baseTable, list);
                }
                list.add(desc);
            }
        }
    }

    private void readForeignKeys() {

        for (BeanDescriptor<?> d : descMap.values()) {
            d.initialiseFkeys();
        }
    }

    /**
     * Initialise all the BeanDescriptors.
     * <p>
     * This occurs after all the BeanDescriptors have been created. This
     * resolves circular relationships between BeanDescriptors.
     * </p>
     * <p>
     * Also responsible for creating all the BeanManagers which contain the
     * persister, listener etc.
     * </p>
     */
    private void initialiseAll() {

        // now that all the BeanDescriptors are in their map
        // we can initialise them which sorts out circular
        // dependencies for OneToMany and ManyToOne etc

        // PASS 1:
        // initialise the ID properties of all the beans
        // first (as they are needed to initialise the
        // associated properties in the second pass).
        for (BeanDescriptor<?> d : descMap.values()) {
            d.initialiseId();
        }

        // PASS 2:
        // now initialise all the associated properties
        for (BeanDescriptor<?> d : descMap.values()) {
            d.initialiseOther();
        }

        // create BeanManager for each non-embedded entity bean
        for (BeanDescriptor<?> d : descMap.values()) {
            if (!d.isEmbedded()) {
                BeanManager<?> m = beanManagerFactory.create(d);
                beanManagerMap.put(d.getFullName(), m);

                checkForValidEmbeddedId(d);
            }
        }
    }

    private void checkForValidEmbeddedId(BeanDescriptor<?> d) {
        IdBinder idBinder = d.getIdBinder();
        if (idBinder != null && idBinder instanceof IdBinderEmbedded) {
            IdBinderEmbedded embId = (IdBinderEmbedded) idBinder;
            BeanDescriptor<?> idBeanDescriptor = embId.getIdBeanDescriptor();
            Class<?> idType = idBeanDescriptor.getBeanType();
            try {
                idType.getDeclaredMethod("hashCode", new Class[] {});
                idType.getDeclaredMethod("equals", new Class[] { Object.class });
            } catch (NoSuchMethodException e) {
                checkMissingHashCodeOrEquals(e, idType, d.getBeanType());
            }
        }
    }

    private void checkMissingHashCodeOrEquals(Exception source, Class<?> idType, Class<?> beanType) {

        String msg = "SERIOUS ERROR: The hashCode() and equals() methods *MUST* be implemented ";
        msg += "on Embedded bean " + idType + " as it is used as an Id for " + beanType;

        if (GlobalProperties.getBoolean("ebean.strict", true)) {
            throw new PersistenceException(msg, source);
        } else {
            logger.log(Level.SEVERE, msg, source);
        }
    }

    /**
     * Return an immutable list of all the BeanDescriptors.
     */
    public List<BeanDescriptor<?>> getBeanDescriptorList() {
        return immutableDescriptorList;
    }

    public Map<Class<?>, BeanTable> getBeanTables() {
        return beanTableMap;
    }

    public BeanTable getBeanTable(Class<?> type) {
        return beanTableMap.get(type);
    }

    public Map<String, BeanDescriptor<?>> getBeanDescriptors() {
        return descMap;
    }

    @SuppressWarnings("unchecked")
    public <T> BeanManager<T> getBeanManager(Class<T> entityType) {

        return (BeanManager<T>) getBeanManager(entityType.getName());
    }

    public BeanManager<?> getBeanManager(String beanClassName) {

        beanClassName = SubClassUtil.getSuperClassName(beanClassName);
        return beanManagerMap.get(beanClassName);
    }

    public DNativeQuery getNativeQuery(String name) {
        return deployOrmXml.getNativeQuery(name);
    }

    /**
     * Create the BeanControllers, BeanFinders and BeanListeners.
     */
    private void createListeners() {

        int qa = beanQueryAdapterManager.getRegisterCount();
        int cc = persistControllerManager.getRegisterCount();
        int lc = persistListenerManager.getRegisterCount();
        int fc = beanFinderManager.createBeanFinders(bootupClasses.getBeanFinders());

        logger.fine("BeanPersistControllers[" + cc + "] BeanFinders[" + fc + "] BeanPersistListeners[" + lc
                + "] BeanQueryAdapters[" + qa + "]");
    }

    /**
     * Log Warning if mixing subclass and enhancement.
     * <p>
     * If enhancement is used for some classes it is expected to be used for all
     * and vice versa.
     * </p>
     */
    private void logStatus() {

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

    private <T> BeanDescriptor<T> createEmbedded(Class<T> beanClass) {

        DeployBeanInfo<T> info = createDeployBeanInfo(beanClass);
        readDeployAssociations(info);
        return new BeanDescriptor<T>(this, typeManager, info.getDescriptor());
    }

    private void registerBeanDescriptor(BeanDescriptor<?> desc) {
        descMap.put(desc.getBeanType().getName(), desc);
    }

    /**
     * Read deployment information for all the embedded beans.
     */
    private void readEmbeddedDeployment() {

        ArrayList<Class<?>> embeddedClasses = bootupClasses.getEmbeddables();
        for (int i = 0; i < embeddedClasses.size(); i++) {
            Class<?> cls = embeddedClasses.get(i);
            if (logger.isLoggable(Level.FINER)) {
                String msg = "load deployinfo for embeddable:" + cls.getName();
                logger.finer(msg);
            }
            BeanDescriptor<?> embDesc = createEmbedded(cls);
            registerBeanDescriptor(embDesc);
        }
    }

    /**
     * Read the initial deployment information for the entities.
     * <p>
     * This stops short of reading relationship meta data until after the
     * BeanTables have all been created.
     * </p>
     */
    private void readEntityDeploymentInitial() {

        ArrayList<Class<?>> entityClasses = bootupClasses.getEntities();

        for (Class<?> entityClass : entityClasses) {
            DeployBeanInfo<?> info = createDeployBeanInfo(entityClass);
            deplyInfoMap.put(entityClass, info);
        }
    }

    /**
     * Create the BeanTable information which has the base table and id.
     * <p>
     * This is determined prior to resolving relationship information.
     * </p>
     */
    private void readEntityBeanTable() {

        Iterator<DeployBeanInfo<?>> it = deplyInfoMap.values().iterator();
        while (it.hasNext()) {
            DeployBeanInfo<?> info = it.next();
            BeanTable beanTable = createBeanTable(info);
            beanTableMap.put(beanTable.getBeanType(), beanTable);
        }
    }

    /**
     * Create the BeanTable information which has the base table and id.
     * <p>
     * This is determined prior to resolving relationship information.
     * </p>
     */
    private void readEntityDeploymentAssociations() {

        Iterator<DeployBeanInfo<?>> it = deplyInfoMap.values().iterator();
        while (it.hasNext()) {
            DeployBeanInfo<?> info = it.next();
            readDeployAssociations(info);
        }
    }

    private void readInheritedIdGenerators() {

        Iterator<DeployBeanInfo<?>> it = deplyInfoMap.values().iterator();
        while (it.hasNext()) {
            DeployBeanInfo<?> info = it.next();
            DeployBeanDescriptor<?> descriptor = info.getDescriptor();
            InheritInfo inheritInfo = descriptor.getInheritInfo();
            if (inheritInfo != null && !inheritInfo.isRoot()) {
                DeployBeanInfo<?> rootBeanInfo = deplyInfoMap.get(inheritInfo.getRoot().getType());
                IdGenerator rootIdGen = rootBeanInfo.getDescriptor().getIdGenerator();
                if (rootIdGen != null) {
                    descriptor.setIdGenerator(rootIdGen);
                }
            }
        }
    }

    /**
     * Create the BeanTable from the deployment information gathered so far.
     */
    private BeanTable createBeanTable(DeployBeanInfo<?> info) {

        DeployBeanDescriptor<?> deployDescriptor = info.getDescriptor();
        DeployBeanTable beanTable = deployDescriptor.createDeployBeanTable();
        return new BeanTable(beanTable, this);
    }

    /**
     * Parse the named Raw Sql queries using BeanDescriptor.
     */
    private void readRawSqlQueries() {

        for (DeployBeanInfo<?> info : deplyInfoMap.values()) {

            DeployBeanDescriptor<?> deployDesc = info.getDescriptor();
            BeanDescriptor<?> desc = getBeanDescriptor(deployDesc.getBeanType());

            for (RawSqlMeta rawSqlMeta : deployDesc.getRawSqlMeta()) {
                DeployNamedQuery nq = new RawSqlSelectBuilder(namingConvention, desc, rawSqlMeta).parse();
                desc.addNamedQuery(nq);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void readEntityRelationships() {

        // We only perform 'circular' checks etc after we have
        // all the DeployBeanDescriptors created and in the map.

        for (DeployBeanInfo<?> info : deplyInfoMap.values()) {
            checkMappedBy(info);
        }

        for (DeployBeanInfo<?> info : deplyInfoMap.values()) {
            secondaryPropsJoins(info);
        }
        
        for (DeployBeanInfo<?> info : deplyInfoMap.values()) {
            registerBeanDescriptor(new BeanDescriptor(this, typeManager, info.getDescriptor()));
        }
    }

    private void secondaryPropsJoins(DeployBeanInfo<?> info) {

        DeployBeanDescriptor<?> descriptor = info.getDescriptor();
        for (DeployBeanProperty prop : descriptor.propertiesBase()) {
            if (prop.isSecondaryTable()) {
                String tableName = prop.getSecondaryTable();
                // find a join to that table...
                DeployBeanPropertyAssocOne<?> assocOne = descriptor.findJoinToTable(tableName);
                if (assocOne == null){
                    String msg = "Error with property "+prop.getFullBeanName()
                        + ". Could not find a Relationship to table "+tableName
                        + ". Perhaps you could use a @JoinColumn instead.";
                    throw new RuntimeException(msg);
                } 
                DeployTableJoin tableJoin = assocOne.getTableJoin();
                prop.setSecondaryTableJoin(tableJoin, assocOne.getName());
            }
        }
    }
    
    /**
     * Check the mappedBy attributes for properties on this descriptor.
     * <p>
     * This will read join information defined on the 'owning/other' side of the
     * relationship. It also does some extra work for unidirectional
     * relationships.
     * </p>
     */
    private void checkMappedBy(DeployBeanInfo<?> info) {

        for (DeployBeanPropertyAssocOne<?> oneProp : info.getDescriptor().propertiesAssocOne()) {
            if (!oneProp.isTransient()) {
                if (oneProp.getMappedBy() != null) {
                    checkMappedByOneToOne(info, oneProp);
                }
            }
        }

        for (DeployBeanPropertyAssocMany<?> manyProp : info.getDescriptor().propertiesAssocMany()) {
            if (!manyProp.isTransient()) {
                if (manyProp.isManyToMany()) {
                    checkMappedByManyToMany(info, manyProp);
                } else {
                    checkMappedByOneToMany(info, manyProp);
                }
            }
        }
    }

    private DeployBeanDescriptor<?> getTargetDescriptor(DeployBeanPropertyAssoc<?> prop) {

        Class<?> targetType = prop.getTargetType();
        DeployBeanInfo<?> info = deplyInfoMap.get(targetType);
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
    private boolean findMappedBy(DeployBeanPropertyAssocMany<?> prop) {

        // this is the entity bean type - that owns this property
        Class<?> owningType = prop.getOwningType();

        Set<String> matchSet = new HashSet<String>();

        // get the bean descriptor that holds the mappedBy property
        DeployBeanDescriptor<?> targetDesc = getTargetDescriptor(prop);
        List<DeployBeanPropertyAssocOne<?>> ones = targetDesc.propertiesAssocOne();
        for (DeployBeanPropertyAssocOne<?> possibleMappedBy : ones) {
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

                        String m = "Implicitly found mappedBy for " + targetDesc + "." + prop;
                        m += " by searching for [" + searchName + "] against " + matchSet;
                        logger.fine(m);

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
    @SuppressWarnings("unchecked")
    private void makeUnidirectional(DeployBeanInfo<?> info, DeployBeanPropertyAssocMany<?> oneToMany) {

        DeployBeanDescriptor<?> targetDesc = getTargetDescriptor(oneToMany);

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
        DeployBeanPropertyAssocOne<?> unidirectional = new DeployBeanPropertyAssocOne(targetDesc, owningType);
        targetDesc.setUnidirectional(unidirectional);

        // specify table and table alias...
        BeanTable beanTable = getBeanTable(owningType);
        unidirectional.setBeanTable(beanTable);
        unidirectional.setName(beanTable.getBaseTable());

        info.setBeanJoinType(unidirectional, true);

        // define the TableJoin
        DeployTableJoin oneToManyJoin = oneToMany.getTableJoin();
        if (!oneToManyJoin.hasJoinColumns()) {
            throw new RuntimeException("No join columns");
        }

        // inverse of the oneToManyJoin
        DeployTableJoin unidirectionalJoin = unidirectional.getTableJoin();
        unidirectionalJoin.setColumns(oneToManyJoin.columns(), true);

    }

    private void checkMappedByOneToOne(DeployBeanInfo<?> info, DeployBeanPropertyAssocOne<?> prop) {

        // check that the mappedBy property is valid and read
        // its associated join information if it is available
        String mappedBy = prop.getMappedBy();

        // get the mappedBy property
        DeployBeanDescriptor<?> targetDesc = getTargetDescriptor(prop);
        DeployBeanProperty mappedProp = targetDesc.getBeanProperty(mappedBy);
        if (mappedProp == null) {
            String m = "Error on " + prop.getFullBeanName();
            m += "  Can not find mappedBy property [" + targetDesc + "." + mappedBy + "] ";
            throw new PersistenceException(m);
        }

        if (!(mappedProp instanceof DeployBeanPropertyAssocOne<?>)) {
            String m = "Error on " + prop.getFullBeanName();
            m += ". mappedBy property [" + targetDesc + "." + mappedBy + "]is not a OneToOne?";
            throw new PersistenceException(m);
        }

        DeployBeanPropertyAssocOne<?> mappedAssocOne = (DeployBeanPropertyAssocOne<?>) mappedProp;

        if (!mappedAssocOne.isOneToOne()) {
            String m = "Error on " + prop.getFullBeanName();
            m += ". mappedBy property [" + targetDesc + "." + mappedBy + "]is not a OneToOne?";
            throw new PersistenceException(m);
        }

        DeployTableJoin tableJoin = prop.getTableJoin();
        if (!tableJoin.hasJoinColumns()) {
            // define Join as the inverse of the mappedBy property
            DeployTableJoin otherTableJoin = mappedAssocOne.getTableJoin();
            otherTableJoin.copyTo(tableJoin, true, tableJoin.getTable());
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
    private void checkMappedByOneToMany(DeployBeanInfo<?> info, DeployBeanPropertyAssocMany<?> prop) {

        // get the bean descriptor that holds the mappedBy property

        if (prop.getMappedBy() == null) {
            if (!findMappedBy(prop)) {
                makeUnidirectional(info, prop);
                return;
            }
        }

        // check that the mappedBy property is valid and read
        // its associated join information if it is available
        String mappedBy = prop.getMappedBy();

        // get the mappedBy property
        DeployBeanDescriptor<?> targetDesc = getTargetDescriptor(prop);
        DeployBeanProperty mappedProp = targetDesc.getBeanProperty(mappedBy);
        if (mappedProp == null) {

            String m = "Error on " + prop.getFullBeanName();
            m += "  Can not find mappedBy property [" + mappedBy + "] ";
            m += "in [" + targetDesc + "]";
            throw new PersistenceException(m);
        }

        if (!(mappedProp instanceof DeployBeanPropertyAssocOne<?>)) {
            String m = "Error on " + prop.getFullBeanName();
            m += ". mappedBy property [" + mappedBy + "]is not a ManyToOne?";
            m += "in [" + targetDesc + "]";
            throw new PersistenceException(m);
        }

        DeployBeanPropertyAssocOne<?> mappedAssocOne = (DeployBeanPropertyAssocOne<?>) mappedProp;

        DeployTableJoin tableJoin = prop.getTableJoin();
        if (!tableJoin.hasJoinColumns()) {
            // define Join as the inverse of the mappedBy property
            DeployTableJoin otherTableJoin = mappedAssocOne.getTableJoin();
            otherTableJoin.copyTo(tableJoin, true, tableJoin.getTable());
        }

    }

    /**
     * For mappedBy copy the joins from the other side.
     */
    private void checkMappedByManyToMany(DeployBeanInfo<?> info, DeployBeanPropertyAssocMany<?> prop) {

        // get the bean descriptor that holds the mappedBy property
        String mappedBy = prop.getMappedBy();
        if (mappedBy == null) {
            return;
        }

        // get the mappedBy property
        DeployBeanDescriptor<?> targetDesc = getTargetDescriptor(prop);
        DeployBeanProperty mappedProp = targetDesc.getBeanProperty(mappedBy);

        if (mappedProp == null) {
            String m = "Error on " + prop.getFullBeanName();
            m += "  Can not find mappedBy property [" + mappedBy + "] ";
            m += "in [" + targetDesc + "]";
            throw new PersistenceException(m);
        }

        if (!(mappedProp instanceof DeployBeanPropertyAssocMany<?>)) {
            String m = "Error on " + prop.getFullBeanName();
            m += ". mappedBy property [" + targetDesc + "." + mappedBy + "] is not a ManyToMany?";
            throw new PersistenceException(m);
        }

        DeployBeanPropertyAssocMany<?> mappedAssocMany = (DeployBeanPropertyAssocMany<?>) mappedProp;

        if (!mappedAssocMany.isManyToMany()) {
            String m = "Error on " + prop.getFullBeanName();
            m += ". mappedBy property [" + targetDesc + "." + mappedBy + "] is not a ManyToMany?";
            throw new PersistenceException(m);
        }

        // define the relationships/joins on this side as the
        // reverse of the other mappedBy side ...

        // DeployTableJoin mappedJoin = mappedAssocMany.getTableJoin();
        DeployTableJoin mappedIntJoin = mappedAssocMany.getIntersectionJoin();
        DeployTableJoin mappendInverseJoin = mappedAssocMany.getInverseJoin();

        String intTableName = mappedIntJoin.getTable();

        DeployTableJoin tableJoin = prop.getTableJoin();
        mappedIntJoin.copyTo(tableJoin, true, targetDesc.getBaseTable());

        DeployTableJoin intJoin = new DeployTableJoin();
        mappendInverseJoin.copyTo(intJoin, false, intTableName);
        prop.setIntersectionJoin(intJoin);

        DeployTableJoin inverseJoin = new DeployTableJoin();
        mappedIntJoin.copyTo(inverseJoin, false, intTableName);
        prop.setInverseJoin(inverseJoin);
    }

    private <T> void setBeanControllerFinderListener(DeployBeanDescriptor<T> descriptor) {

        Class<T> beanType = descriptor.getBeanType();

        persistControllerManager.addPersistControllers(descriptor);
        persistListenerManager.addPersistListeners(descriptor);
        beanQueryAdapterManager.addQueryAdapter(descriptor);

        BeanFinder<T> beanFinder = beanFinderManager.getBeanFinder(beanType);
        if (beanFinder != null) {
            descriptor.setBeanFinder(beanFinder);
            logger.fine("BeanFinder on[" + descriptor.getFullName() + "] " + beanFinder.getClass().getName());
        }
    }

    /**
     * Read the initial deployment information for a given bean type.
     */
    private <T> DeployBeanInfo<T> createDeployBeanInfo(Class<T> beanClass) {

        DeployBeanDescriptor<T> desc = new DeployBeanDescriptor<T>(beanClass);

        desc.setUpdateChangesOnly(updateChangesOnly);

        // set bean controller, finder and listener
        setBeanControllerFinderListener(desc);
        deplyInherit.process(desc);

        createProperties.createProperties(desc);

        DeployBeanInfo<T> info = new DeployBeanInfo<T>(deployUtil, desc);

        readAnnotations.readInitial(info);
        return info;
    }

    private <T> void readDeployAssociations(DeployBeanInfo<T> info) {

        DeployBeanDescriptor<T> desc = info.getDescriptor();

        readAnnotations.readAssociations(info, this);

        readXml(desc);

        if (!EntityType.ORM.equals(desc.getEntityType())){
            // not using base table
            desc.setBaseTable(null);
        }

        // mark transient properties
        transientProperties.process(desc);
        setScalarType(desc);

        if (!desc.isEmbedded()) {
            // Set IdGenerator or use DB Identity
            setIdGeneration(desc);

            // find the appropriate default concurrency mode
            setConcurrencyMode(desc);
        }

        autoAddValidators(desc);

        // generate the byte code
        createByteCode(desc);
    }

    /**
     * Set the Identity generation mechanism.
     */
    private <T> IdType setIdGeneration(DeployBeanDescriptor<T> desc) {

        if (desc.propertiesId().size() == 0) {
            // bean doen't have an Id property
            if (!desc.isBaseTableType() || desc.getBeanFinder() != null) {
                // using BeanFinder so perhaps valid without an id
            } else {
                // expecting an id property
                logger.warning(Message.msg("deploy.nouid", desc.getFullName()));
            }
            return null;
        }

        if (IdType.SEQUENCE.equals(desc.getIdType()) && !dbIdentity.isSupportsSequence()) {
            // explicit sequence but not supported by the DatabasePlatform
            logger.info("Explicit sequence on " + desc.getFullName() + " but not supported by DB Platform - ignored");
            desc.setIdType(null);
        }
        if (IdType.IDENTITY.equals(desc.getIdType()) && !dbIdentity.isSupportsIdentity()) {
            // explicit identity but not supported by the DatabasePlatform
            logger.info("Explicit Identity on " + desc.getFullName() + " but not supported by DB Platform - ignored");
            desc.setIdType(null);
        }
        
        if (desc.getIdType() == null) {
            // use the default. IDENTITY or SEQUENCE.
            desc.setIdType(dbIdentity.getIdType());
        }

        if (IdType.GENERATOR.equals(desc.getIdType())) {
            String genName = desc.getIdGeneratorName();
            if (UuidIdGenerator.AUTO_UUID.equals(genName)) {
                desc.setIdGenerator(uuidIdGenerator);
                return IdType.GENERATOR;
            }
        }

        if (desc.getBaseTable() == null) {
            // no base table so not going to set Identity
            // of sequence information
            return null;
        }

        if (IdType.IDENTITY.equals(desc.getIdType())) {
            // used when getGeneratedKeys is not supported (SQL Server 2000)
            String selectLastInsertedId = dbIdentity.getSelectLastInsertedId(desc.getBaseTable());
            desc.setSelectLastInsertedId(selectLastInsertedId);
            return IdType.IDENTITY;
        }

        String seqName = desc.getIdGeneratorName();
        if (seqName != null) {
            logger.fine("explicit sequence " + seqName + " on " + desc.getFullName());
        } else {
            // use namingConvention to define sequence name
            seqName = namingConvention.getSequenceName(desc.getBaseTable());
        }

        // create the sequence based IdGenerator
        IdGenerator seqIdGen = createSequenceIdGenerator(seqName);
        desc.setIdGenerator(seqIdGen);

        return IdType.SEQUENCE;
    }

    private IdGenerator createSequenceIdGenerator(String seqName) {
        return databasePlatform.createSequenceIdGenerator(backgroundExecutor, dataSource, seqName, dbSequenceBatchSize);
    }

    private void createByteCode(DeployBeanDescriptor<?> deploy) {

        // check to see if the bean supports EntityBean interface
        // generate a subclass if required
        setEntityBeanClass(deploy);

        // use Code generation or Standard reflection to support
        // getter and setter methods
        setBeanReflect(deploy);
    }

    /**
     * Add Length and NotNull validators based on Column annotation etc.
     */
    private void autoAddValidators(DeployBeanDescriptor<?> deployDesc) {

        for (DeployBeanProperty prop : deployDesc.propertiesBase()) {
            autoAddValidators(prop);
        }
    }

    /**
     * Add Length and NotNull validators based on Column annotation etc.
     */
    private void autoAddValidators(DeployBeanProperty prop) {

        if (String.class.equals(prop.getPropertyType()) && prop.getDbLength() > 0) {
            // check if the property already has the LengthValidator
            if (!prop.containsValidatorType(LengthValidatorFactory.LengthValidator.class)) {
                prop.addValidator(LengthValidatorFactory.create(0, prop.getDbLength()));
            }
        }
        if (!prop.isNullable() && !prop.isId() && !prop.isGenerated()) {
            // check if the property already has the NotNullValidator
            if (!prop.containsValidatorType(NotNullValidatorFactory.NotNullValidator.class)) {
                prop.addValidator(NotNullValidatorFactory.NOT_NULL);
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
    private void setScalarType(DeployBeanDescriptor<?> deployDesc) {

        Iterator<DeployBeanProperty> it = deployDesc.propertiesAll();
        while (it.hasNext()) {
            DeployBeanProperty prop = it.next();
            if (prop instanceof DeployBeanPropertyAssoc<?>) {

            } else {
                deployUtil.setScalarType(prop);
            }
        }
    }

    private void readXml(DeployBeanDescriptor<?> deployDesc) {

        Dnode entityXml = deployOrmXml.findEntityDeploymentXml(deployDesc.getFullName());

        if (entityXml != null) {
            readXmlNamedQueries(deployDesc, entityXml);
            readXmlSql(deployDesc, entityXml);
        }
    }

    /**
     * Read sql-select (FUTURE: additionally sql-insert, sql-update,
     * sql-delete). If found this entity bean is based on raw sql.
     */
    private void readXmlSql(DeployBeanDescriptor<?> deployDesc, Dnode entityXml) {

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

    private void readSqlSelect(DeployBeanDescriptor<?> deployDesc, Dnode sqlSelect) {

        String name = sqlSelect.getStringAttr("name", "default");
        String extend = sqlSelect.getStringAttr("extend", null);
        String queryDebug = sqlSelect.getStringAttr("debug", null);
        boolean debug = (queryDebug != null && queryDebug.equalsIgnoreCase("true"));

        // the raw sql select
        String query = findContent(sqlSelect, "query");
        String where = findContent(sqlSelect, "where");
        String having = findContent(sqlSelect, "having");
        String columnMapping = findContent(sqlSelect, "columnMapping");

        RawSqlMeta m = new RawSqlMeta(name, extend, query, debug, where, having, columnMapping);

        deployDesc.add(m);

    }

    /**
     * Read named queries for this bean type.
     */
    private void readXmlNamedQueries(DeployBeanDescriptor<?> deployDesc, Dnode entityXml) {

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
    private void setBeanReflect(DeployBeanDescriptor<?> desc) {

        // Set the BeanReflectGetter and BeanReflectSetter that typically
        // use generated code. NB: Due to Bug 166 so now doing this for
        // abstract classes as well.

        Class<?> beanType = desc.getBeanType();
        Class<?> factType = desc.getFactoryType();

        BeanReflect beanReflect = reflectFactory.create(beanType, factType);
        desc.setBeanReflect(beanReflect);

        try {
            Iterator<DeployBeanProperty> it = desc.propertiesAll();
            while (it.hasNext()) {
                DeployBeanProperty prop = it.next();
                String propName = prop.getName();

                if (desc.isAbstract()) {
                    // use reflection in the case of imported abstract class
                    // with
                    // inheritance. Refer Bug 166
                    prop.setGetter(ReflectGetter.create(prop));
                    prop.setSetter(ReflectSetter.create(prop));

                } else {
                    // use generated code for getting setting property values
                    BeanReflectGetter getter = beanReflect.getGetter(propName);
                    BeanReflectSetter setter = beanReflect.getSetter(propName);
                    prop.setGetter(getter);
                    prop.setSetter(setter);
                    if (getter == null) {
                        // should never happen
                        String m = "BeanReflectGetter for " + prop.getFullBeanName() + " was not found?";
                        throw new RuntimeException(m);
                    }
                }

            }
        } catch (IllegalArgumentException e) {
            Class<?> superClass = desc.getBeanType().getSuperclass();
            String msg = "Error with [" + desc.getFullName() + "] I believe it is not enhanced but it's superClass ["
                    + superClass + "] is?"
                    + " (You are not allowed to mix enhancement in a single inheritance hierarchy)";
            throw new PersistenceException(msg, e);
        }
    }

    /**
     * DevNote: It is assumed that Embedded can contain version properties. It
     * is also assumed that Embedded beans do NOT themselves contain Embedded
     * beans which contain version properties.
     */
    private void setConcurrencyMode(DeployBeanDescriptor<?> desc) {

        if (!desc.getConcurrencyMode().equals(ConcurrencyMode.ALL)) {
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
    private boolean checkForVersionProperties(DeployBeanDescriptor<?> desc) {

        boolean hasVersionProperty = false;

        List<DeployBeanProperty> props = desc.propertiesBase();
        for (int i = 0; i < props.size(); i++) {
            if (props.get(i).isVersionColumn()) {
                hasVersionProperty = true;
            }
        }

        return hasVersionProperty;
    }

    private boolean hasEntityBeanInterface(Class<?> beanClass) {
        
        Class<?>[] interfaces = beanClass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].equals(EntityBean.class)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Test the bean type to see if it implements EntityBean interface already.
     */
    private void setEntityBeanClass(DeployBeanDescriptor<?> desc) {

        Class<?> beanClass = desc.getBeanType();

        if (desc.isAbstract()) {
            if (hasEntityBeanInterface(beanClass)) {
                checkEnhanced(desc, beanClass);
            } else {
                checkSubclass(desc, beanClass);
            }
            return;
        }
        try {
            Object testBean =  null;
            try {
                testBean = beanClass.newInstance();
            } catch (InstantiationException e){
                // expected when no default constructor
                logger.fine("no default constructor on "+beanClass+" e:"+e);
            } catch (IllegalAccessException e){
                // expected when no default constructor
                logger.fine("no default constructor on "+beanClass+" e:"+e);
            }
            if (testBean instanceof EntityBean == false) {
                checkSubclass(desc, beanClass);
                
            } else {
                String className = beanClass.getName();
                try {
                    // check that it really is enhanced (rather than mixed
                    // enhancement)
                    String marker = ((EntityBean) testBean)._ebean_getMarker();
                    if (!marker.equals(className)) {
                        String msg = "Error with [" + desc.getFullName()
                                + "] It has not been enhanced but it's superClass [" + beanClass.getSuperclass()
                                + "] is?"
                                + " (You are not allowed to mix enhancement in a single inheritance hierarchy)"
                                + " marker[" + marker + "] className[" + className + "]";
                        throw new PersistenceException(msg);
                    }
                } catch (AbstractMethodError e) {
                    throw new PersistenceException(
                            "Old Ebean v1.0 enhancement detected in Ebean v1.1 - please do a clean enhancement.", e);
                }

                checkEnhanced(desc, beanClass);
            } 

        } catch (PersistenceException ex) {
            throw ex;

        } catch (Exception ex) {
            throw new PersistenceException(ex);
        }
    }

    private void checkEnhanced(DeployBeanDescriptor<?> desc, Class<?> beanClass) {
        // the bean already implements EntityBean
        checkInheritedClasses(true, beanClass);

        desc.setFactoryType(beanClass);
        if (!beanClass.getName().startsWith("com.avaje.ebean.meta")) {
            enhancedClassCount++;
        }
    }

    private void checkSubclass(DeployBeanDescriptor<?> desc, Class<?> beanClass) {

        checkInheritedClasses(false, beanClass);
        desc.checkReadAndWriteMethods();

        subclassClassCount++;

        Class<?> subClass = subClassManager.resolve(beanClass.getName());
        desc.setFactoryType(subClass);

        subclassedEntities.add(desc.getName());
    }

    /**
     * Check that the inherited classes are the same as the entity bean (aka all
     * enhanced or all dynamically subclassed).
     */
    private void checkInheritedClasses(boolean ensureEnhanced, Class<?> beanClass) {
        Class<?> superclass = beanClass.getSuperclass();
        if (Object.class.equals(superclass)) {
            // we got to the top of the inheritance
            return;
        }
        boolean isClassEnhanced = EntityBean.class.isAssignableFrom(superclass);

        if (ensureEnhanced != isClassEnhanced) {
            String msg;
            if (ensureEnhanced) {
                msg = "Class [" + superclass + "] is not enhanced and [" + beanClass + "] is - (you can not mix!!)";
            } else {
                msg = "Class [" + superclass + "] is enhanced and [" + beanClass + "] is not - (you can not mix!!)";
            }
            throw new IllegalStateException(msg);
        }

        // recursively continue up the inheritance hierarchy
        checkInheritedClasses(ensureEnhanced, superclass);
    }

    /**
     * Comparator to sort the BeanDescriptors by name.
     */
    private static final class BeanDescComparator implements Comparator<BeanDescriptor<?>>, Serializable {

        private static final long serialVersionUID = 1L;

        public int compare(BeanDescriptor<?> o1, BeanDescriptor<?> o2) {

            return o1.getName().compareTo(o2.getName());
        }
    }
}
