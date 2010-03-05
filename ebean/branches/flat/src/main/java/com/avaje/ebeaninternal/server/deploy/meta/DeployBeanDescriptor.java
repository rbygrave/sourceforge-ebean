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
package com.avaje.ebeaninternal.server.deploy.meta;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.config.TableName;
import com.avaje.ebean.config.dbplatform.IdGenerator;
import com.avaje.ebean.config.dbplatform.IdType;
import com.avaje.ebean.event.BeanFinder;
import com.avaje.ebean.event.BeanPersistController;
import com.avaje.ebean.event.BeanPersistListener;
import com.avaje.ebean.event.BeanQueryAdapter;
import com.avaje.ebean.meta.MetaAutoFetchStatistic;
import com.avaje.ebeaninternal.server.core.ConcurrencyMode;
import com.avaje.ebeaninternal.server.core.ReferenceOptions;
import com.avaje.ebeaninternal.server.deploy.ChainedBeanPersistController;
import com.avaje.ebeaninternal.server.deploy.ChainedBeanPersistListener;
import com.avaje.ebeaninternal.server.deploy.ChainedBeanQueryAdapter;
import com.avaje.ebeaninternal.server.deploy.DeployNamedQuery;
import com.avaje.ebeaninternal.server.deploy.DeployNamedUpdate;
import com.avaje.ebeaninternal.server.deploy.InheritInfo;
import com.avaje.ebeaninternal.server.deploy.RawSqlMeta;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor.EntityType;
import com.avaje.ebeaninternal.server.reflect.BeanReflect;

/**
 * Describes Beans including their deployment information.
 */
public class DeployBeanDescriptor<T> {
	
	static class PropOrder implements Comparator<DeployBeanProperty> {

		public int compare(DeployBeanProperty o1, DeployBeanProperty o2) {
			
			int v2 = o1.getSortOrder();
			int v1 = o2.getSortOrder();
			return (v1<v2 ? -1 : (v1==v2 ? 0 : 1));
		}
	}
	private static final PropOrder PROP_ORDER = new PropOrder();

	private static final Logger logger = Logger.getLogger(DeployBeanDescriptor.class.getName());
	
	private static final String META_BEAN_PREFIX = MetaAutoFetchStatistic.class.getName().substring(0,20);
	
	/**
	 * Map of BeanProperty Linked so as to preserve order.
	 */
	private LinkedHashMap<String, DeployBeanProperty> propMap = new LinkedHashMap<String, DeployBeanProperty>();

	/**
	 * The type of bean this describes.
	 */
	private final Class<T> beanType;

	private EntityType entityType;
	
	private final Map<String, DeployNamedQuery> namedQueries = new LinkedHashMap<String, DeployNamedQuery>();

	private final Map<String, DeployNamedUpdate> namedUpdates = new LinkedHashMap<String, DeployNamedUpdate>();
	
	private final Map<String, RawSqlMeta> rawSqlMetas = new LinkedHashMap<String, RawSqlMeta>();
	
	private DeployBeanPropertyAssocOne<?> unidirectional;

	/**
	 * Type of Identity generation strategy used.
	 */
	private IdType idType;

	/**
	 * The name of an IdGenerator (optional).
	 */
	private String idGeneratorName;

	private IdGenerator idGenerator;

	/**
	 * The database sequence name (optional).
	 */
	private String sequenceName;
	
	private String ldapBaseDn;
	
	private String[] ldapObjectclasses;
	
	/**
	 * Used with Identity columns but no getGeneratedKeys support.
	 */
	private String selectLastInsertedId;

	private String lazyFetchIncludes;

	/**
	 * The concurrency mode for beans of this type.
	 */
	private ConcurrencyMode concurrencyMode = ConcurrencyMode.ALL;

	private boolean updateChangesOnly;
	
	/**
	 * The tables this bean is dependent on.
	 */
	private String[] dependantTables;

	/**
	 * Extra deployment attributes.
	 */
	private HashMap<String, String> extraAttrMap = new HashMap<String, String>();

	/**
	 * The base database table.
	 */
	private String baseTable;
    private TableName baseTableFull;

	/**
	 * Used to provide mechanism to new EntityBean instances. Generated code
	 * faster than reflection at this stage.
	 */
	private BeanReflect beanReflect;


	/**
	 * The EntityBean type used to create new EntityBeans.
	 */
	private Class<?> factoryType;

	private List<BeanPersistController> persistControllers = new ArrayList<BeanPersistController>();
	private List<BeanPersistListener<T>> persistListeners = new ArrayList<BeanPersistListener<T>>();
	private List<BeanQueryAdapter> queryAdapters = new ArrayList<BeanQueryAdapter>();

	private ReferenceOptions referenceOptions;
	
	/**
	 * If set overrides the find implementation. Server side only.
	 */
	private BeanFinder<T> beanFinder;

	/**
	 * The table joins for this bean. Server side only.
	 */
	private ArrayList<DeployTableJoin> tableJoinList = new ArrayList<DeployTableJoin>();

	/**
	 * Inheritance information. Server side only.
	 */
	private InheritInfo inheritInfo;

	private String name;
	
	private boolean processedRawSqlExtend;
	
	/**
	 * Construct the BeanDescriptor.
	 */
	public DeployBeanDescriptor(Class<T> beanType) {
		this.beanType = beanType;
	}
	
	/**
	 * Return true if this beanType is an abstract class.
	 */
	public boolean isAbstract() {
		
		return Modifier.isAbstract(beanType.getModifiers());
	}
	
	public Collection<RawSqlMeta> getRawSqlMeta() {
		if (!processedRawSqlExtend){
			rawSqlProcessExtend();
			processedRawSqlExtend = true;
		}
		return rawSqlMetas.values();
	}
	
	/**
	 * Process the "extend" attributes of raw SQL.
	 * Aka inherit the query and column mapping.
	 */
	private void rawSqlProcessExtend() {
		
		for (RawSqlMeta rawSqlMeta : rawSqlMetas.values()) {
			String extend = rawSqlMeta.getExtend();
			if (extend != null){
				RawSqlMeta parentQuery = rawSqlMetas.get(extend);
				if (parentQuery == null) {
					throw new RuntimeException("parent query ["+extend+"] not found for sql-select "+rawSqlMeta.getName());
				}
				rawSqlMeta.extend(parentQuery);
			}
		}
	}

	
	public DeployBeanTable createDeployBeanTable() {
		
		DeployBeanTable beanTable = new DeployBeanTable(getBeanType());
		beanTable.setBaseTable(baseTable);
		beanTable.setIdProperties(propertiesId());
		
		return beanTable;
	}
	
	/**
	 * Check all the properties to see if they all have read and write
	 * methods (required if using "subclassing" but not for "enhancement"). 
	 */
	public boolean checkReadAndWriteMethods() {
		
		if (isMeta()){
			return true;
		}
		boolean missingMethods = false;
		
		Iterator<DeployBeanProperty> it = propMap.values().iterator();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();
			if (!prop.isTransient()){
				String m = "";
				if (prop.getReadMethod() == null){
					m += " missing readMethod ";
				}
				if (prop.getWriteMethod() == null){
					m += " missing writeMethod ";
				}
				if (!"".equals(m)){
				    m += ". Should it be transient?";
					String msg = "Bean property "+getFullName()+"."+prop.getName()+" has "+m;
					logger.log(Level.SEVERE, msg);
					missingMethods = true;
				}			
			}
		}
		return !missingMethods;
	}

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }
    
    public boolean isEmbedded() {
        return EntityType.EMBEDDED.equals(entityType);
    }
    
    public boolean isBaseTableType() {
        EntityType et = getEntityType();
        return EntityType.ORM.equals(et);
    }

    public EntityType getEntityType() {
        if (entityType == null){
            entityType = isMeta() ? EntityType.META : EntityType.ORM; 
        }
        return entityType;
    }

    /**
     * Return true if this is a Meta entity bean.
     * <p>
     * The Meta entity beans are not based on real tables but get meta information
     * from memory such as all the entity bean meta data.
     * </p>
     */
    private boolean isMeta() {
        return beanType.getName().startsWith(META_BEAN_PREFIX);
    }

	public void add(RawSqlMeta rawSqlMeta) {
		rawSqlMetas.put(rawSqlMeta.getName(), rawSqlMeta);
		if ("default".equals(rawSqlMeta.getName())) {
		    setEntityType(EntityType.SQL);
		}
	}

	public void add(DeployNamedUpdate namedUpdate) {
		namedUpdates.put(namedUpdate.getName(), namedUpdate);
	}

    public void add(DeployNamedQuery namedQuery) {
        namedQueries.put(namedQuery.getName(), namedQuery);
        if ("default".equals(namedQuery.getName())) {
            setEntityType(EntityType.SQL);
        }
    }

	public Map<String, DeployNamedQuery> getNamedQueries() {
		return namedQueries;
	}

	public Map<String, DeployNamedUpdate> getNamedUpdates() {
		return namedUpdates;
	}
	
	public BeanReflect getBeanReflect() {
		return beanReflect;
	}

	/**
	 * Return the class type this BeanDescriptor describes.
	 */
	public Class<T> getBeanType() {
		return beanType;
	}

	/**
	 * Return the class type this BeanDescriptor describes.
	 */
	public Class<?> getFactoryType() {
		return factoryType;
	}

	/**
	 * Set the class used to create new EntityBean instances.
	 * <p>
	 * Normally this would be a subclass dynamically generated for this bean.
	 * </p>
	 */
	public void setFactoryType(Class<?> factoryType) {
		this.factoryType = factoryType;
	}

	/**
	 * Set the BeanReflect used to create new instances of an EntityBean. This
	 * could use reflection or code generation to do this.
	 */
	public void setBeanReflect(BeanReflect beanReflect) {
		this.beanReflect = beanReflect;
	}
	
	/**
	 * Returns the Inheritance mapping information. This will be null if this
	 * type of bean is not involved in any ORM inheritance mapping.
	 */
	public InheritInfo getInheritInfo() {
		return inheritInfo;
	}

	/**
	 * Set the ORM inheritance mapping information.
	 */
	public void setInheritInfo(InheritInfo inheritInfo) {
		this.inheritInfo = inheritInfo;
	}
	
	/**
	 * Return the reference options. 
	 */
	public ReferenceOptions getReferenceOptions() {
		return referenceOptions;
	}

	/**
	 * Set the reference options.
	 */
	public void setReferenceOptions(ReferenceOptions referenceOptions) {
		this.referenceOptions = referenceOptions;
	}
	
	public DeployBeanPropertyAssocOne<?> getUnidirectional() {
		return unidirectional;
	}

	public void setUnidirectional(DeployBeanPropertyAssocOne<?> unidirectional) {
		this.unidirectional = unidirectional;
	}

	/**
	 * Return the concurrency mode used for beans of this type.
	 */
	public ConcurrencyMode getConcurrencyMode() {
		return concurrencyMode;
	}

	/**
	 * Set the concurrency mode used for beans of this type.
	 */
	public void setConcurrencyMode(ConcurrencyMode concurrencyMode) {
		this.concurrencyMode = concurrencyMode;
	}

	public String getLdapBaseDn() {
        return ldapBaseDn;
    }

    public void setLdapBaseDn(String ldapBaseDn) {
        this.ldapBaseDn = ldapBaseDn;
    }
    
    public String[] getLdapObjectclasses() {
        return ldapObjectclasses;
    }

    public void setLdapObjectclasses(String[] ldapObjectclasses) {
        this.ldapObjectclasses = ldapObjectclasses;
    }

    public boolean isUpdateChangesOnly() {
		return updateChangesOnly;
	}

	public void setUpdateChangesOnly(boolean updateChangesOnly) {
		this.updateChangesOnly = updateChangesOnly;
	}

	/**
	 * Return the tables this bean is dependant on. This implies that if any of
	 * these tables are modified then cached beans may be invalidated.
	 */
	public String[] getDependantTables() {
		return dependantTables;
	}

	/**
	 * Set the tables this bean is dependant on. This implies that if any of
	 * these tables are modified then cached beans may be invalidated.
	 */
	public void setDependantTables(String[] dependantTables) {
		this.dependantTables = dependantTables;
	}

	/**
	 * Return the beanFinder. Usually null unless overriding the finder.
	 */
	public BeanFinder<T> getBeanFinder() {
		return beanFinder;
	}

	/**
	 * Set the BeanFinder to use for beans of this type. This is set to override
	 * the finding from the default.
	 */
	public void setBeanFinder(BeanFinder<T> beanFinder) {
		this.beanFinder = beanFinder;
	}

	/**
	 * Return the BeanPersistController (could be a chain of them, 1 or null).
	 */
	public BeanPersistController getPersistController() {
		if (persistControllers.size() == 0){
			return null;
		} else if (persistControllers.size() == 1) {
			return persistControllers.get(0);
		} else {
			return new ChainedBeanPersistController(persistControllers);			
		}
	}

	/**
	 * Return the BeanPersistListener (could be a chain of them, 1 or null).
	 */
	public BeanPersistListener<T> getPersistListener() {
		if (persistListeners.size() == 0){
			return null;
		} else if (persistListeners.size() == 1) {
			return persistListeners.get(0);
		} else {
			return new ChainedBeanPersistListener<T>(persistListeners);			
		}
	}

	public BeanQueryAdapter getQueryAdapter() {
		if (queryAdapters.size() == 0){
			return null;
		} else if (queryAdapters.size() == 1) {
			return queryAdapters.get(0);
		} else {
			return new ChainedBeanQueryAdapter(queryAdapters);			
		}
	}
	
	/**
	 * Set the Controller.
	 */
	public void addPersistController(BeanPersistController controller) {
		persistControllers.add(controller);
	}

	public void addPersistListener(BeanPersistListener<T> listener) {
		persistListeners.add(listener);
	}
	
	public void addQueryAdapter(BeanQueryAdapter queryAdapter) {
		queryAdapters.add(queryAdapter);
	}
	
	/**
	 * Return true if this bean type should use IdGeneration.
	 * <p>
	 * If this is false and the Id is null it is assumed that a database auto
	 * increment feature is being used to populate the id.
	 * </p>
	 */
	public boolean isUseIdGenerator() {
		return idType == IdType.GENERATOR;
	}

	/**
	 * Return the base table. Only properties mapped to the base table are by
	 * default persisted.
	 */
	public String getBaseTable() {
		return baseTable;
	}

	/**
	 * Return the base table with full structure.
	 */
	public TableName getBaseTableFull() {
        return baseTableFull;
    }

    /**
	 * Set the base table. Only properties mapped to the base table are by
	 * default persisted.
	 */
	public void setBaseTable(TableName baseTableFull) {
	    this.baseTableFull = baseTableFull;
		this.baseTable = baseTableFull == null ? null : baseTableFull.getQualifiedName();
	}

	public void sortProperties() {
		
		ArrayList<DeployBeanProperty> list = new ArrayList<DeployBeanProperty>();
		list.addAll(propMap.values());
		
		Collections.sort(list,PROP_ORDER);
		
		propMap = new LinkedHashMap<String, DeployBeanProperty>(list.size());
		for (int i = 0; i < list.size(); i++) {
			addBeanProperty(list.get(i));
		}
	}
	
	/**
	 * Add a bean property.
	 */
	public DeployBeanProperty addBeanProperty(DeployBeanProperty prop) {
		return propMap.put(prop.getName(), prop);
	}

	/**
	 * Get a BeanProperty by its name.
	 */
	public DeployBeanProperty getBeanProperty(String propName) {
		return propMap.get(propName);
	}

	public Map<String, String> getExtraAttributeMap() {
		return extraAttrMap;
	}

	/**
	 * Get a named extra attribute.
	 */
	public String getExtraAttribute(String key) {
		return (String) extraAttrMap.get(key);
	}

	/**
	 * Set an extra attribute with a given name.
	 * 
	 * @param key
	 *            the name of the extra attribute
	 * @param value
	 *            the value of the extra attribute
	 */
	public void setExtraAttribute(String key, String value) {
		extraAttrMap.put(key, value);
	}

	/**
	 * Return the bean class name this descriptor is used for.
	 * <p>
	 * If this BeanDescriptor is for a table then this returns the table name
	 * instead.
	 * </p>
	 */
	public String getFullName() {
		return beanType.getName();
	}

	/**
	 * Return the bean short name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the bean shortName.
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * Return the identity generation type.
	 */
	public IdType getIdType() {
		return idType;
	}

	/**
	 * Set the identity generation type.
	 */
	public void setIdType(IdType idType) {
		this.idType = idType;
	}

	/**
	 * Return the DB sequence name (can be null).
	 */
	public String getSequenceName() {
		return sequenceName;
	}

	/**
	 * Set the DB sequence name.
	 */
	public void setSequenceName(String sequenceName) {
		this.sequenceName = sequenceName;
	}
	
	/**
	 * Return the SQL used to return the last inserted Id.
	 * <p>
	 * Used with Identity columns where getGeneratedKeys is not supported.
	 * </p>
	 */
	public String getSelectLastInsertedId() {
		return selectLastInsertedId;
	}

	/**
	 * Set the SQL used to return the last inserted Id.
	 */
	public void setSelectLastInsertedId(String selectLastInsertedId) {
		this.selectLastInsertedId = selectLastInsertedId;
	}

	/**
	 * Return the name of the IdGenerator that should be used with this type of
	 * bean. A null value could be used to specify the 'default' IdGenerator.
	 */
	public String getIdGeneratorName() {
		return idGeneratorName;
	}

	/**
	 * Set the name of the IdGenerator that should be used with this type of
	 * bean.
	 */
	public void setIdGeneratorName(String idGeneratorName) {
		this.idGeneratorName = idGeneratorName;
	}

	/**
	 * Return the actual IdGenerator for this bean type (can be null).
	 */
	public IdGenerator getIdGenerator() {
		return idGenerator;
	}

	/**
	 * Set the actual IdGenerator for this bean type.
	 */
	public void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
		if (idGenerator != null && idGenerator.isDbSequence()){
			setSequenceName(idGenerator.getName());
		}
	}

	/**
	 * Return the includes for getReference().
	 */
	public String getLazyFetchIncludes() {
		return lazyFetchIncludes;
	}

	/**
	 * Set includes to use for lazy loading by getReference(). Note queries also
	 * build references and includes on the actual association are used for
	 * those references.
	 */
	public void setLazyFetchIncludes(String lazyFetchIncludes) {
		if (lazyFetchIncludes != null && lazyFetchIncludes.length() > 0) {
			this.lazyFetchIncludes = lazyFetchIncludes;
		}
	}

	/**
	 * Summary description.
	 */
	public String toString() {
		return getFullName();
	}

	/**
	 * Add a TableJoin to this type of bean. For Secondary table properties.
	 */
	public void addTableJoin(DeployTableJoin join) {
		tableJoinList.add(join);
	}

	public List<DeployTableJoin> getTableJoins() {
		return tableJoinList;
	}

	/**
	 * Return an Iterator of all BeanProperty.
	 */
	public Iterator<DeployBeanProperty> propertiesAll() {
		return propMap.values().iterator();
	}

	/**
	 * Return the defaultSelectClause using FetchType.LAZY and FetchType.EAGER.
	 */
	public String getDefaultSelectClause() {
		
		StringBuilder sb = new StringBuilder();
		
		boolean hasLazyFetch = false;
				
		Iterator<DeployBeanProperty> it  = propMap.values().iterator();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();
			if (prop instanceof DeployBeanPropertyAssocMany<?>){
				// ignore the associated many properties
			} else {
				if (prop.isFetchEager()){
					sb.append(prop.getName()).append(",");
				} else {
					hasLazyFetch = true;
				}
			}
		}
		
		if (!hasLazyFetch){
			return null;
		}
		String selectClause = sb.toString();
		return selectClause.substring(0, selectClause.length()-1);
	}
	
	/**
     * Return an Array of properties to include in default fetch (for LDAP).
     */
	public String[] getDefaultSelectDbArray(Set<String> defaultSelect) {
	    
	    ArrayList<String> list = new ArrayList<String>();
	    for (DeployBeanProperty p : propMap.values()) {
	        if (defaultSelect != null) {
	            if (defaultSelect.contains(p.getName())){
	                // properties in defaultSelect
	                list.add(p.getDbColumn());   
	            }
	        } else if (!p.isTransient() && p.isDbRead()) {
	            // non transient db properties
	            list.add(p.getDbColumn());	            
	        }
        }
	    return list.toArray(new String[list.size()]);
	}
	
    /**
     * Parse the include separating by comma or semicolon.
     */
    public Set<String> parseDefaultSelectClause(String rawList) {
            	
    	if (rawList == null){
    		return null;
    	}
    	
        String[] res = rawList.split(",");
    	
        LinkedHashSet<String> set = new LinkedHashSet<String>(res.length+3);

        String temp = null;
        for (int i = 0; i < res.length; i++) {
            temp = res[i].trim();
            if (temp.length() > 0){
            	set.add(temp);
            }
        }
        return Collections.unmodifiableSet(set);
    }
	
	/**
	 * Return the BeanProperty that make up the unique id.
	 * <p>
	 * The order of these properties can be relied on to be consistent if the
	 * bean itself doesn't change or the xml deployment order does not change.
	 * </p>
	 */
	public List<DeployBeanProperty> propertiesId() {

		ArrayList<DeployBeanProperty> list = new ArrayList<DeployBeanProperty>();

		Iterator<DeployBeanProperty> it = propMap.values().iterator();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();
			if (prop.isId()) {
				list.add(prop);
			}
		}

		return list;
	}
	
	public DeployBeanPropertyAssocOne<?> findJoinToTable(String tableName){
	    
	    List<DeployBeanPropertyAssocOne<?>> assocOne = propertiesAssocOne();
	    for (DeployBeanPropertyAssocOne<?> prop : assocOne) {
	        DeployTableJoin tableJoin = prop.getTableJoin();
	        if (tableJoin != null && tableJoin.getTable().equalsIgnoreCase(tableName)){
	            return prop;
	        }
        }
	    return null;
	}
	
	/**
	 * Return an Iterator of BeanPropertyAssocOne that are not embedded. These
	 * are effectively joined beans. For ManyToOne and OneToOne associations.
	 */
	public List<DeployBeanPropertyAssocOne<?>> propertiesAssocOne() {

		ArrayList<DeployBeanPropertyAssocOne<?>> list = new ArrayList<DeployBeanPropertyAssocOne<?>>();

		Iterator<DeployBeanProperty> it = propMap.values().iterator();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();
			if (prop instanceof DeployBeanPropertyAssocOne<?>) {
				if (!prop.isEmbedded()) {
					list.add((DeployBeanPropertyAssocOne<?>) prop);
				}
			}
		}

		return list;

	}

	/**
	 * Return BeanPropertyAssocMany for this descriptor.
	 */
	public List<DeployBeanPropertyAssocMany<?>> propertiesAssocMany() {

		ArrayList<DeployBeanPropertyAssocMany<?>> list = new ArrayList<DeployBeanPropertyAssocMany<?>>();

		Iterator<DeployBeanProperty> it = propMap.values().iterator();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();
			if (prop instanceof DeployBeanPropertyAssocMany<?>) {
				list.add((DeployBeanPropertyAssocMany<?>) prop);
			}
		}

		return list;
	}

	/**
	 * Returns 'Version' properties on this bean. These are 'Counter' or 'Update
	 * Timestamp' type properties. Note version properties can also be on
	 * embedded beans rather than on the bean itself.
	 */
	public List<DeployBeanProperty> propertiesVersion() {

		ArrayList<DeployBeanProperty> list = new ArrayList<DeployBeanProperty>();

		Iterator<DeployBeanProperty> it = propMap.values().iterator();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();

			if (prop instanceof DeployBeanPropertyAssoc<?>) {

			} else {
				if (!prop.isId() && prop.isVersionColumn()) {
					list.add(prop);
				}
			}
		}

		return list;
	}

	/**
	 * base properties without the unique id properties.
	 */
	public List<DeployBeanProperty> propertiesBase() {

		ArrayList<DeployBeanProperty> list = new ArrayList<DeployBeanProperty>();

		Iterator<DeployBeanProperty> it = propMap.values().iterator();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();

			if (prop instanceof DeployBeanPropertyAssoc<?>) {

			} else {
				if (!prop.isId()) {
					list.add(prop);
				}
			}
		}

		return list;
	}

}
