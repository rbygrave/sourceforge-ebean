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
package org.avaje.ebean.server.deploy.parse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.avaje.ebean.server.deploy.ManyType;
import org.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import org.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import org.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import org.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
import org.avaje.ebean.server.plugin.PluginDbConfig;
import org.avaje.ebean.server.type.ScalarType;
import org.avaje.ebean.server.type.TypeManager;
import org.avaje.ebean.util.Message;
import org.avaje.lib.log.LogFactory;

/**
 * Create the properties for a bean.
 * <p>
 * This also needs to determine if the property is a associated many, associated
 * one or normal scalar property.
 * </p>
 */
public class CreateProperties {

	private static final Logger logger = LogFactory.get(CreateProperties.class);

	private final TypeManager typeManager;
	
    public CreateProperties(PluginDbConfig dbConfig) {
    	typeManager = dbConfig.getTypeManager();
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
     * reflect the bean properties from Class. Some of these properties may not
     * map to database columns.
     */
    private void createProperties(DeployBeanDescriptor desc, Class<?> beanType, int level) {

        try {
        	Method[] declaredMethods = beanType.getDeclaredMethods();
        	Field[] fields = beanType.getDeclaredFields();
        	
            for (int i = 0; i < fields.length; i++) {
            	
            	Field field = fields[i];
            	if (field.getName().startsWith("_ebean_")){
            		// not interested in ebean added fields
            		
            	} else if (Modifier.isStatic(field.getModifiers())) {
            		// not interested in static fields 
            		
            	} else {
            	
            		Method getter = findGetter(field, declaredMethods);
            		Method setter = findSetter(field, declaredMethods);
            		
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
    
    /**
     * Find a public non-static getter method that matches this field (according to bean-spec rules).
     */
    private Method findGetter(Field field, Method[] declaredMethods){
    	
    	String initFieldName = initCap(field.getName());
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
    private Method findSetter(Field field, Method[] declaredMethods){
    	
    	String initFieldName = initCap(field.getName());
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
