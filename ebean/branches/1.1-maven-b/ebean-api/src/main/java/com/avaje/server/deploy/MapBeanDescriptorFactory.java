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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.MapBean;
import com.avaje.ebean.bean.BeanController;
import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.bean.BeanListener;
import com.avaje.ebean.server.core.ConcurrencyMode;
import com.avaje.ebean.server.deploy.generatedproperty.GeneratedPropertySettings;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployMapBeanDescriptor;
import com.avaje.ebean.server.lib.sql.ColumnInfo;
import com.avaje.ebean.server.lib.sql.TableInfo;
import com.avaje.ebean.server.naming.NamingConvention;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.server.type.ScalarType;
import com.avaje.ebean.server.type.TypeManager;
import com.avaje.ebean.util.Message;
import com.avaje.lib.log.LogFactory;

/**
 * Create a BeanDescriptor dynamically based on Table meta data.
 * <p>
 * Uses jdbc meta data to dynamically create a BeanDescriptor for a table. Used
 * in conjunction with MapBean to enable fetching and updating without any
 * deployment information.
 * </p>
 */
public class MapBeanDescriptorFactory {

	private static final Logger logger = LogFactory.get(MapBeanDescriptorFactory.class);
	
    /**
     * The set method on the MapBean.
     */
    public static final String SET_METHOD = "set";
    
    /**
     * The get method on the MapBean.
     */
    public static final String GET_METHOD = "get";
    
    /**
     * Used to make sure we have the correct set method.
     */
    public static final Class<?> SET_ARG_CLASS = String.class;
    
    /**
     * Used to make sure we have the correct get method.
     */
    public static final Class<?> GET_ARG_CLASS = Object.class;
    
    /**
     * the database plugin.
     */
    final PluginDbConfig dbConfig;

    /**
     * The base setter method.
     */
    Method setPropertyMethod;

    /**
     * The base getter method.
     */
    Method getPropertyMethod;

    /**
     * Determines if columns are Generate value type columns ssuch as update
     * timestamp, insert timestamp and counter.
     */
    final GeneratedPropertySettings generateSettings;

    /**
     * Used to convert db column names to property names.
     */
    final NamingConvention namingConvention;

    /**
     * True if idGeneration is used on all MapBeans.
     */
    final char defaultIdentityGeneration;
    
    /**
     * True if db sequences are used on all MapBeans.
     */
    final boolean supportsSequences;
    
    final DeploymentManager deploymentManager;
    
    final TypeManager typeManager;
    
    /**
     * Create a TableDescriptorFactory.
     */
    public MapBeanDescriptorFactory(DeploymentManager deploymentManager, PluginDbConfig dbConfig) {
    	this.deploymentManager = deploymentManager;
        this.dbConfig = dbConfig;
        this.typeManager = dbConfig.getTypeManager();

        defaultIdentityGeneration = dbConfig.getDefaultIdentityGeneration();
        supportsSequences = dbConfig.isSupportsSequences();
        namingConvention = dbConfig.getNamingConvention();

        generateSettings = new GeneratedPropertySettings(dbConfig.getProperties());
        // for TableDescriptors don't check the property names
        // as MapBean properties may be just the column names.
        generateSettings.setCheckProperty(false);
        
        findMethods();
    }
    
   
    
    /**
     * Create a BeanDescriptor based on the tableName.
     */
    public MapBeanDescriptor createBeanDescriptor(String tableName) {
        try {
            TableInfo ti = dbConfig.getDictionaryInfo().getTableInfo(tableName);
            if (ti == null) {
            	// probably bean based on raw sql query (for reporting purposes?)
            	return null;
            }

            DeployMapBeanDescriptor desc = new DeployMapBeanDescriptor(deploymentManager);
            desc.setIdentityGeneration(defaultIdentityGeneration);
            
            desc.setTableGenerated(true);
            
            
            desc.setBaseTable(ti.getName());

            addColumns(desc, ti);
            
            // estimate the initial capacity based on the number
            // of properties and the loadFactor
            desc.setMapInitialCapacity(0);

            determineUniqueId(desc, ti);

            determineConcurrencyMode(desc);
            
            if (supportsSequences){
                // Note: the sequence only gets used *IF* the value of the uid property
                // is null when inserted. default on Oracle. 
                String seqNextVal = namingConvention.getSequenceNextval(desc);
                desc.setSequenceNextVal(seqNextVal);
            }
            
            setBeanIntercept(desc);
            
            return new MapBeanDescriptor(typeManager, desc);

        } catch (Exception ex) {
            throw new PersistenceException(ex);
        }
    }

    /**
     * Set any BeanController, BeanFinder and BeanListener for MapBeans.
     */
    private void setBeanIntercept(DeployMapBeanDescriptor desc) {
    	
    	String tableName = desc.getBaseTable();
    	String key = "mapbean."+tableName+".beancontroller";
    	String cn = dbConfig.getProperties().getProperty(key, null);
    	if (cn != null){
    		BeanController bc = (BeanController)createInstance(cn);
    		desc.setBeanController(bc);
    	}
    	
    	key = "mapbean."+tableName+".beanlistener";
    	cn = dbConfig.getProperties().getProperty(key, null);
    	if (cn != null){
    		BeanListener bl = (BeanListener)createInstance(cn);
    		desc.setBeanListener(bl);
    	}
    	
    	key = "mapbean."+tableName+".beanfinder";
    	cn = dbConfig.getProperties().getProperty(key, null);
    	if (cn != null){
    		BeanFinder bf = (BeanFinder)createInstance(cn);
    		desc.setBeanFinder(bf);
    	}
    }
    
    
    private Object createInstance(String cn){
    	try {
	    	Class<?> cls = Class.forName(cn);
	    	return cls.newInstance();
    	} catch (Exception ex){
    		throw new PersistenceException(ex);
    	}
    }
    
    private void determineConcurrencyMode(DeployMapBeanDescriptor desc) {
    	List<DeployBeanProperty> verProps = desc.propertiesVersion();
    	if (verProps.size() > 0){
    		desc.setConcurrencyMode(ConcurrencyMode.VERSION);
    	} else {
    		desc.setConcurrencyMode(ConcurrencyMode.ALL);
    	}
    }
    
    /**
     * Get the property name using the naming convention.
     */
    private String getPropertyName(String dbColumnName) {
        
        return namingConvention.mapPropertyFromColumn(dbColumnName);
    }
    
    /**
     * Find the set and get methods on MapBean class.
     */
    private void findMethods() {
        try {
            BeanInfo bi = Introspector.getBeanInfo(MapBean.class);
            MethodDescriptor[] md = bi.getMethodDescriptors();
            for (int i = 0; i < md.length; i++) {
                if (setPropertyMethod != null && getPropertyMethod != null) {
                    // already found both of the methods
                    break;
                }
                String methodName = md[i].getName();
                if (methodName.equals(SET_METHOD)) {
                    Class<?>[] types = md[i].getMethod().getParameterTypes();
                    if (types != null && types[0].equals(SET_ARG_CLASS)) {
                        setPropertyMethod = md[i].getMethod();
                    }
                } else if (methodName.equals(GET_METHOD)) {
                    Class<?>[] types = md[i].getMethod().getParameterTypes();
                    if (types != null && types[0].equals(GET_ARG_CLASS)) {
                        getPropertyMethod = md[i].getMethod();
                    }
                }
            }
        } catch (IntrospectionException ex) {
            throw new RuntimeException(ex);
        }
        if (setPropertyMethod == null){
            throw new RuntimeException("MapBean set method not found?");
        }
        if (getPropertyMethod == null){
            throw new RuntimeException("MapBean get method not found?");
        }
    }


    /**
     * add a BeanProperty for each column.
     */
    private void addColumns(DeployMapBeanDescriptor desc, TableInfo ti) {
    	
    	ColumnInfo[] columns = ti.getColumns();
    	for (int i = 0; i < columns.length; i++) {
		
            ColumnInfo colInfo = columns[i];

            DeployBeanProperty prop = new DeployBeanProperty(desc);
            prop.setDbColumn(colInfo.getName());
            
            // always native jdbc types for Map beans.
            // No scalar type setting
            prop.setDbType(colInfo.getDataType());
            
            // get the Class type the jdbc type maps to
            ScalarType scalarType = typeManager.getScalarType(colInfo.getDataType());
            if (scalarType == null){
            	// Some JDBC Types such as Types.OTHER are not mapped 
            	String msg = "Could not map JDBC type ["+colInfo.getDataType()+"]"
            	+" to a Java type for ["+ti.getName()+"."+colInfo.getName()+"]";
            	logger.warning(msg);
            } else {
            	prop.setPropertyType(scalarType.getType());
            }

            // convert the column name
            String propName = getPropertyName(colInfo.getName());
            prop.setName(propName);

            prop.setReadMethod(getPropertyMethod);

            prop.setWriteMethod(setPropertyMethod);

            MapBeanSetter setter = new MapBeanSetter(propName);
            prop.setSetter(setter);
            
            MapBeanGetter getter = new MapBeanGetter(propName);
            prop.setGetter(getter);
            
            prop.setDbRead(true);
            prop.setDbWrite(true);

            // determine if this is a GeneratedProperty
            // based on db column name and data type
            generateSettings.setGeneratedProperty(prop);

            desc.addBeanProperty(prop);
        }
    }
    
    /**
     * Determine the properties making the unique id.
     */
    private void determineUniqueId(DeployMapBeanDescriptor desc, TableInfo ti) {
        // determine the uniqueId properties
    	ColumnInfo[] keys = ti.getKeyColumns();
        for (int i = 0; i < keys.length; i++) {
            
            String propName = getPropertyName(keys[i].getName());
            DeployBeanProperty prop = desc.getBeanProperty(propName);
            prop.setId(true);
        }

        if (desc.propertiesId().size() == 0) {
            // there is no Primary key on this table? 
        	List<DeployBeanProperty> props = desc.propertiesBase();
        	
        	if (props.size() > 0){
                // assume that the first property is the Uid Property.
                // Only other choice is to throw fatal error?
        		DeployBeanProperty firstProp = props.get(0);
                firstProp.setId(true);
                
                logger.warning(Message.msg("deploy.uidassume", desc.getFullName(), firstProp.getName()));                    
            }
        }
        
        // check to make sure this bean has a Unique Id
        if (desc.propertiesId().size() == 0) {
            logger.warning(Message.msg("deploy.nouid", desc.getFullName()));
        }
    }
}
