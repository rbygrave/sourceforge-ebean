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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.deploy.ManyType;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.server.type.ScalarType;
import com.avaje.ebean.server.type.TypeManager;
import com.avaje.ebean.util.Message;

/**
 * Create the properties for a bean.
 * <p>
 * This also needs to determine if the property is a associated many, associated
 * one or normal scalar property.
 * </p>
 */
public class CreateProperties {

	private static final Logger logger = Logger.getLogger(CreateProperties.class.getName());

	private final TypeManager typeManager;
	
	private final String[] ignoreFieldPrefixes;
	
    public CreateProperties(PluginDbConfig dbConfig) {
    	typeManager = dbConfig.getTypeManager();
    	
    	// get field name prefixes of fields we want to ignore
    	String ignoreValue = dbConfig.getProperties().getProperty("enhancement.ignorefields", null);
    	if (ignoreValue == null){
    		ignoreFieldPrefixes = null;
    	} else {
    		ignoreFieldPrefixes = ignoreValue.split(",");
    	}
    }
    
    /**
     * Create the appropriate properties for a bean.
     */
    public void createProperties(DeployBeanDescriptor desc) {
    	
        createProperties(desc, desc.getBeanType(), 0);
        
        // check the transient properties...
        Iterator<DeployBeanProperty> it = desc.propertiesAll();
        
        while (it.hasNext()) {
			DeployBeanProperty prop = it.next();
			if (prop.isTransient()){
				if (prop.getWriteMethod() == null || prop.getReadMethod() == null){
		    		// Typically a helper method ... this is expected
		    		logger.finest("... transient: "+prop.getFullBeanName());					
				} else {
					// dubious, possible error...
		            String msg = Message.msg("deploy.property.nofield", desc.getFullName(), prop.getName());
		            logger.warning(msg);
				}
			}
		}
    }

    /**
     * Return true if we should ignore this field.
     * <p>
     * We want to ignore ebean internal fields and some others as well.
     * </p>
     */
    private boolean ignoreFieldByName(String fieldName) {
    	if (fieldName.startsWith("_ebean_")){
    		// ignore Ebean internal fields
    		return true;
    	} 
    	if (fieldName.startsWith("ajc$instance$")) {
    		// ignore AspectJ internal fields
    		return true;
    	}
    	if (ignoreFieldPrefixes != null){
    		// ignore user defined field prefixes
    		for (int i = 0; i < ignoreFieldPrefixes.length; i++) {
    			if (fieldName.startsWith(ignoreFieldPrefixes[i])) {
    				return true;
    			}
			}
    	}
    	// we are interested in this field
    	return false;
    }
    
    /**
     * reflect the bean properties from Class. Some of these properties may not
     * map to database columns.
     */
    private void createProperties(DeployBeanDescriptor desc, Class<?> beanType, int level) {

        try {
        	Method[] declaredMethods = beanType.getDeclaredMethods();
        	Field[] fields = beanType.getDeclaredFields();
        	
            for (int i = 0; i < fields.length; i++) {
            	
            	Field field = fields[i];
            	if (ignoreFieldByName(field.getName())){
            		// not interested this field
            		
            	} else if (Modifier.isStatic(field.getModifiers())) {
            		// not interested in static fields 
            		
            	} else {
            	
                	String fieldName = getFieldName(field, beanType);
                	String initFieldName = initCap(fieldName);

            		Method getter = findGetter(field, initFieldName, declaredMethods);
            		Method setter = findSetter(field, initFieldName, declaredMethods);
            		
	                DeployBeanProperty prop = createProp(level, desc, field, beanType, getter, setter);                    
	                DeployBeanProperty replaced = desc.addBeanProperty(prop);
	                if (replaced != null){
	                	if (replaced.isTransient()) {
	                		// expected for inheritance...
	                	} else {
	                		String msg = "Huh??? property "+prop.getFullBeanName()+" being defined twice";
	                		msg += " but replaced property was not transient? This is not expected?";
	                		logger.warning(msg);
	                	}
	                }
            	}
            }

            Class<?> superClass = beanType.getSuperclass();
            
            if (!superClass.equals(Object.class)) {
                // recursively add any properties in the inheritance heirarchy
                // up to the Object.class level...
                createProperties(desc, superClass, level + 1);
            }

        } catch (PersistenceException ex) {
            throw ex;
            
        } catch (Exception ex) {
            throw new PersistenceException(ex);
        }
    }

    /**
     * Make the first letter of the string upper case.
     */
    private String initCap(String str){
    	if (str.length() > 1){
    		return Character.toUpperCase(str.charAt(0))+str.substring(1);
    	} else {
    		// only a single char
    		return str.toUpperCase();
    	}
    }
    
    private String getFieldName(Field field, Class<?> beanType){
    	String name = field.getName();
    	if (name.startsWith("is") && name.length() > 2){
    		char c = name.charAt(2);
    		if (Character.isUpperCase(c)){
    			String msg = "trimming off 'is' from field name "+name+" in class "+beanType.getName();
    			logger.log(Level.INFO, msg);
    			
    			return name.substring(2);
    		}
    	}
    	return name;
    }
    
    /**
     * Find a public non-static getter method that matches this field (according to bean-spec rules).
     */
    private Method findGetter(Field field, String initFieldName, Method[] declaredMethods){
 
    	String methGetName = "get"+initFieldName;
    	String methIsName = "is"+initFieldName;
  
    	for (int i = 0; i < declaredMethods.length; i++) {
    		Method m = declaredMethods[i];
			if (m.getName().equals(methGetName) || m.getName().equals(methIsName)){
				Class<?>[] params = m.getParameterTypes();
				if (params.length == 0){
					if (field.getType().equals(m.getReturnType())){
						int modifiers = m.getModifiers();
						if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
							// we find it...
							return m;
						}
					}
				}
			}
		}
    	return null;
    }
    
    /**
     * Find a public non-static setter method that matches this field (according to bean-spec rules).
     */
    private Method findSetter(Field field, String initFieldName, Method[] declaredMethods){
    	
    	String methSetName = "set"+initFieldName;
    	
    	for (int i = 0; i < declaredMethods.length; i++) {
    		Method m = declaredMethods[i];
    		
			if (m.getName().equals(methSetName)){
				Class<?>[] params = m.getParameterTypes();
				if (params.length == 1 && field.getType().equals(params[0])){
					if (void.class.equals(m.getReturnType())){
						int modifiers = m.getModifiers();
						if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
							return m;
						}
					}
				}
			}
		}
    	return null;
    }
    
    private DeployBeanProperty createProp(int level, DeployBeanDescriptor desc, Field field, Class<?> beanType, Method getter, Method setter) {
    	
    	Class<?> propertyType = field.getType();
        DeployBeanProperty prop = null;

        ManyType manyType = ManyType.getManyType(propertyType);

        if (manyType != null) {
            // List, Set or Map based object
            prop = new DeployBeanPropertyAssocMany(desc, manyType);

        } else if (propertyType.isEnum() || propertyType.isPrimitive()){
            prop = new DeployBeanProperty(desc);
            
        } else if (isScalarType(propertyType)) {
        	prop = new DeployBeanProperty(desc);
        
        } else {
        	prop = new DeployBeanPropertyAssocOne(desc);
        }
        
        //field.setAccessible(true);
        
        prop.setOwningType(beanType);

        prop.setName(field.getName());
        prop.setPropertyType(propertyType);
        
        // the getter or setter could be null if we are using
        // javaagent type enhancement. If we are using subclass
        // generation then we do need to find the getter and setter
        prop.setReadMethod(getter);
        prop.setWriteMethod(setter);

        prop.setField(field);
        
        return prop;
    }
    
    private boolean isScalarType(Class<?> propertyType) {
    	
    	ScalarType scalarType = typeManager.getScalarType(propertyType);
    	return scalarType != null;
    }
    
}
