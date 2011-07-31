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
package com.avaje.ebeaninternal.server.core;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.avaje.ebean.annotation.LdapDomain;
import com.avaje.ebean.config.CompoundType;
import com.avaje.ebean.config.ScalarTypeConverter;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.lucene.IndexDefn;
import com.avaje.ebean.event.BeanFinder;
import com.avaje.ebean.event.BeanPersistController;
import com.avaje.ebean.event.BeanPersistListener;
import com.avaje.ebean.event.BeanQueryAdapter;
import com.avaje.ebean.event.ServerConfigStartup;
import com.avaje.ebeaninternal.server.type.ScalarType;
import com.avaje.ebeaninternal.server.util.ClassPathSearchMatcher;

/**
 * Interesting classes for a EbeanServer such as Embeddable, Entity,
 * ScalarTypes, Finders, Listeners and Controllers.
 */
public class BootupClasses implements ClassPathSearchMatcher {

    private static final Logger logger = Logger.getLogger(BootupClasses.class.getName());

    private ArrayList<Class<?>> xmlBeanList = new ArrayList<Class<?>>();

    private ArrayList<Class<?>> embeddableList = new ArrayList<Class<?>>();

    private ArrayList<Class<?>> entityList = new ArrayList<Class<?>>();

    private ArrayList<Class<?>> scalarTypeList = new ArrayList<Class<?>>();

    private ArrayList<Class<?>> scalarConverterList = new ArrayList<Class<?>>();

    private ArrayList<Class<?>> compoundTypeList = new ArrayList<Class<?>>();
    
    private ArrayList<Class<?>> beanControllerList = new ArrayList<Class<?>>();

    private ArrayList<Class<?>> beanFinderList = new ArrayList<Class<?>>();

    private ArrayList<Class<?>> beanListenerList = new ArrayList<Class<?>>();

    private ArrayList<Class<?>> beanQueryAdapterList = new ArrayList<Class<?>>();

    private ArrayList<Class<?>> luceneIndexList = new ArrayList<Class<?>>();

    private ArrayList<Class<?>> serverConfigStartupList = new ArrayList<Class<?>>();
    private ArrayList<ServerConfigStartup> serverConfigStartupInstances = new ArrayList<ServerConfigStartup>();

    private List<BeanPersistController> persistControllerInstances = new ArrayList<BeanPersistController>();
    private List<BeanPersistListener<?>> persistListenerInstances = new ArrayList<BeanPersistListener<?>>();
    private List<BeanQueryAdapter> queryAdapterInstances = new ArrayList<BeanQueryAdapter>();
    private List<IndexDefn<?>> luceneIndexInstances = new ArrayList<IndexDefn<?>>();

    public BootupClasses() {
    }

    public BootupClasses(List<Class<?>> list) {
        if (list != null) {
            process(list.iterator());
        }
    }

    private BootupClasses(BootupClasses parent) {
        this.xmlBeanList.addAll(parent.xmlBeanList);
        this.embeddableList.addAll(parent.embeddableList);
        this.entityList.addAll(parent.entityList);
        this.scalarTypeList.addAll(parent.scalarTypeList);
        this.scalarConverterList.addAll(parent.scalarConverterList);
        this.compoundTypeList.addAll(parent.compoundTypeList);
        this.beanControllerList.addAll(parent.beanControllerList);
        this.beanFinderList.addAll(parent.beanFinderList);
        this.beanListenerList.addAll(parent.beanListenerList);
        this.beanQueryAdapterList.addAll(parent.beanQueryAdapterList);
        this.luceneIndexList.addAll(parent.luceneIndexList);
        this.serverConfigStartupList.addAll(parent.serverConfigStartupList);
    }

    private void process(Iterator<Class<?>> it) {
        while (it.hasNext()) {
            Class<?> cls = it.next();
            isMatch(cls);
        }
    }

    /**
     * Create a copy of this object so that classes can be added to it.
     */
    public BootupClasses createCopy() {
        return new BootupClasses(this);
    }

    /**
     * Run any ServerConfigStartup listeners.
     */
    public void runServerConfigStartup(ServerConfig serverConfig) {
    	
        for (Class<?> cls : serverConfigStartupList) {
            try {
            	ServerConfigStartup newInstance = (ServerConfigStartup) cls.newInstance();
            	newInstance.onStart(serverConfig);
            	
            } catch (Exception e) {
                String msg = "Error creating BeanQueryAdapter " + cls;
                logger.log(Level.SEVERE, msg, e);
            }
        }
    }
    
    public void addIndexDefns(List<IndexDefn<?>> indexInstances) {
        if (indexInstances != null) {
            this.luceneIndexInstances.addAll(indexInstances);
        }
    }
    
    public void addQueryAdapters(List<BeanQueryAdapter> queryAdapterInstances) {
        if (queryAdapterInstances != null) {
            for (BeanQueryAdapter a : queryAdapterInstances) {
            	this.queryAdapterInstances.add(a);
            	// don't automatically instantiate
            	this.beanQueryAdapterList.remove(a.getClass());
            }
        }
    }

    /**
     * Add BeanPersistController instances.
     */
    public void addPersistControllers(List<BeanPersistController> beanControllerInstances) {
        if (beanControllerInstances != null) {
        	for (BeanPersistController c : beanControllerInstances) {
        		this.persistControllerInstances.add(c);
        		// don't automatically instantiate
        		this.beanControllerList.remove(c.getClass());
            }
        }
    }

    public void addPersistListeners(List<BeanPersistListener<?>> listenerInstances) {
        if (listenerInstances != null) {
            for (BeanPersistListener<?> l : listenerInstances) {
                this.persistListenerInstances.add(l);
        		// don't automatically instantiate
	            this.beanListenerList.remove(l.getClass());
            }
        }
    }    

    public void addServerConfigStartup(List<ServerConfigStartup> startupInstances) {
        if (startupInstances != null) {
            for (ServerConfigStartup l : startupInstances) {
                this.serverConfigStartupInstances.add(l);
        		// don't automatically instantiate
	            this.serverConfigStartupList.remove(l.getClass());
            }
        }
    }
    
    public List<BeanQueryAdapter> getBeanQueryAdapters() {
        // add class registered BeanQueryAdapter to the
        // already created instances
        for (Class<?> cls : beanQueryAdapterList) {
            try {
                BeanQueryAdapter newInstance = (BeanQueryAdapter) cls.newInstance();
                queryAdapterInstances.add(newInstance);
            } catch (Exception e) {
                String msg = "Error creating BeanQueryAdapter " + cls;
                logger.log(Level.SEVERE, msg, e);
            }
        }

        return queryAdapterInstances;
    }

    public List<BeanPersistListener<?>> getBeanPersistListeners() {
        // add class registered BeanPersistController to the
        // already created instances
        for (Class<?> cls : beanListenerList) {
            try {
                BeanPersistListener<?> newInstance = (BeanPersistListener<?>) cls.newInstance();
                persistListenerInstances.add(newInstance);
            } catch (Exception e) {
                String msg = "Error creating BeanPersistController " + cls;
                logger.log(Level.SEVERE, msg, e);
            }
        }

        return persistListenerInstances;
    }

    public List<BeanPersistController> getBeanPersistControllers() {
        // add class registered BeanPersistController to the
        // already created instances
        for (Class<?> cls : beanControllerList) {
            try {
                BeanPersistController newInstance = (BeanPersistController) cls.newInstance();
                persistControllerInstances.add(newInstance);
            } catch (Exception e) {
                String msg = "Error creating BeanPersistController " + cls;
                logger.log(Level.SEVERE, msg, e);
            }
        }

        return persistControllerInstances;
    }

    /**
     * Return already constructed Lucene Index definitions.
     */
    public List<IndexDefn<?>> getLuceneIndexInstances() {
        
        for (Class<?> cls: luceneIndexList) {
            try {
                IndexDefn<?> indexDefn = (IndexDefn<?>)cls.newInstance();
                luceneIndexInstances.add(indexDefn);
            } catch (Exception e) {
                String msg = "Error creating BeanPersistController " + cls;
                logger.log(Level.SEVERE, msg, e);
            }
        }
        
        return luceneIndexInstances;
    }
    
    /**
     * Return the list of Embeddable classes.
     */
    public ArrayList<Class<?>> getEmbeddables() {
        return embeddableList;
    }

    /**
     * Return the list of entity classes.
     */
    public ArrayList<Class<?>> getEntities() {
        return entityList;
    }

    /**
     * Return the list of ScalarTypes found.
     */
    public ArrayList<Class<?>> getScalarTypes() {
        return scalarTypeList;
    }

    /**
     * Return the list of ScalarConverters found.
     */
    public ArrayList<Class<?>> getScalarConverters() {
        return scalarConverterList;
    }

    /**
     * Return the list of ScalarConverters found.
     */
    public ArrayList<Class<?>> getCompoundTypes() {
        return compoundTypeList;
    }
    
    /**
     * Return the list of BeanControllers found.
     */
    public ArrayList<Class<?>> getBeanControllers() {
        return beanControllerList;
    }

    /**
     * Return the list of BeanFinders found.
     */
    public ArrayList<Class<?>> getBeanFinders() {
        return beanFinderList;
    }

    /**
     * Return the list of BeanListeners found.
     */
    public ArrayList<Class<?>> getBeanListeners() {
        return beanListenerList;
    }

    /**
     * Return the list of XML Beans.
     */
    public ArrayList<Class<?>> getXmlBeanList() {
        return xmlBeanList;
    }

    public void add(Iterator<Class<?>> it) {
        while (it.hasNext()) {
            Class<?> clazz = it.next();
            isMatch(clazz);
        }
    }

    public boolean isMatch(Class<?> cls) {

        if (isEmbeddable(cls)) {
            embeddableList.add(cls);

        } else if (isEntity(cls)) {
            entityList.add(cls);

        } else if (isXmlBean(cls)){
            entityList.add(cls);
            //xmlBeanList.add(cls);
            
        } else if (isInterestingInterface(cls)) {
            return true;

        } else {
            return false;
        }

        return true;
    }

    /**
     * Look for interesting interfaces.
     * <p>
     * This includes ScalarType, BeanController, BeanFinder and BeanListener.
     * </p>
     */
    private boolean isInterestingInterface(Class<?> cls) {

        boolean interesting = false;

        if (BeanPersistController.class.isAssignableFrom(cls)) {
            beanControllerList.add(cls);
            interesting = true;
        }

        if (ScalarType.class.isAssignableFrom(cls)) {
            scalarTypeList.add(cls);
            interesting = true;
        }

        if (ScalarTypeConverter.class.isAssignableFrom(cls)) {
            scalarConverterList.add(cls);
            interesting = true;
        }

        if (CompoundType.class.isAssignableFrom(cls)) {
            compoundTypeList.add(cls);
            interesting = true;
        }

        if (BeanFinder.class.isAssignableFrom(cls)) {
            beanFinderList.add(cls);
            interesting = true;
        }

        if (BeanPersistListener.class.isAssignableFrom(cls)) {
            beanListenerList.add(cls);
            interesting = true;
        }

        if (BeanQueryAdapter.class.isAssignableFrom(cls)) {
            beanQueryAdapterList.add(cls);
            interesting = true;
        }
        
        if (IndexDefn.class.isAssignableFrom(cls)) {
            luceneIndexList.add(cls);
            interesting = true;
        }
        if (ServerConfigStartup.class.isAssignableFrom(cls)){
        	serverConfigStartupList.add(cls);
        	interesting = true;
        }
        
        return interesting;
    }

    private boolean isEntity(Class<?> cls) {

        Annotation ann = cls.getAnnotation(Entity.class);
        if (ann != null) {
            return true;
        }
        ann = cls.getAnnotation(Table.class);
        if (ann != null) {
            return true;
        }
        ann = cls.getAnnotation(LdapDomain.class);
        if (ann != null) {
            return true;
        }
        return false;
    }

    private boolean isEmbeddable(Class<?> cls) {

        Annotation ann = cls.getAnnotation(Embeddable.class);
        if (ann != null) {
            return true;
        }
        return false;
    }
    
    private boolean isXmlBean(Class<?> cls) {

        Annotation ann = cls.getAnnotation(XmlRootElement.class);
        if (ann != null) {
            return true;
        }
        ann = cls.getAnnotation(XmlType.class);
        if (ann != null) {
            // Only looking for Beans and not Enums
            return !cls.isEnum();
        }
        return false;
    }
}
