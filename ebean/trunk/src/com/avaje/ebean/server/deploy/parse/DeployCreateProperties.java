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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.deploy.ManyType;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
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
public class DeployCreateProperties {

	private static final Logger logger = Logger.getLogger(DeployCreateProperties.class.getName());

	private final TypeManager typeManager;
	
    public DeployCreateProperties(TypeManager typeManager) {
    	this.typeManager = typeManager;
    }
    
    /**
     * Create the appropriate properties for a bean.
     */
    public void createProperties(DeployBeanDescriptor<?> desc) {
    	
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

    	// we are interested in this field
    	return false;
    }
    
    /**
     * reflect the bean properties from Class. Some of these properties may not
     * map to database columns.
     */
    private void createProperties(DeployBeanDescriptor<?> desc, Class<?> beanType, int level) {

        try {
        	Method[] declaredMethods = beanType.getDeclaredMethods();
        	Field[] fields = beanType.getDeclaredFields();
        	
            for (int i = 0; i < fields.length; i++) {
            	
            	Field field = fields[i];	
            	if (Modifier.isStatic(field.getModifiers())) {
            		// not interested in static fields 
            	
            	} else if (Modifier.isTransient(field.getModifiers())) {
            		// not interested in transient fields
            		logger.finer("Skipping transient field "+field.getName()+" in "+beanType.getName());

            	} else if (ignoreFieldByName(field.getName())) {
            		// not interested this field (ebean or aspectJ field)
            		
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
    
    /**
     * Return the bean spec field name (trim of "is" from boolean types)
     */
    private String getFieldName(Field field, Class<?> beanType){
    	
    	String name = field.getName();
    	
    	if ((Boolean.class.equals(field.getType()) || boolean.class.equals(field.getType())) 
    			&& name.startsWith("is") && name.length() > 2){
    		
    		// it is a boolean type field starting with "is"
    		char c = name.charAt(2);
    		if (Character.isUpperCase(c)){
    			String msg = "trimming off 'is' from boolean field name "+name+" in class "+beanType.getName();
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
    
    @SuppressWarnings("unchecked")
	private DeployBeanProperty createProp(int level, DeployBeanDescriptor<?> desc, Field field, Class<?> beanType, Method getter, Method setter) {
    	
        DeployBeanProperty prop = null;

        Class<?> propertyType = field.getType();
        ManyType manyType = ManyType.getManyType(propertyType);

        if (manyType != null) {
            // List, Set or Map based object
        	Class<?> targetType = determineTargetType(field);
        	if (targetType == null){
        		logger.warning("Could not find parameter type (via reflection) on "+desc.getFullName()+" "+field.getName());
        	}
            prop = new DeployBeanPropertyAssocMany(desc, targetType, manyType);

        } else if (propertyType.isEnum() || propertyType.isPrimitive()){
            prop = new DeployBeanProperty(desc, propertyType);
            
        } else if (isScalarType(propertyType)) {
        	prop = new DeployBeanProperty(desc, propertyType);
        
        } else {
        	prop = new DeployBeanPropertyAssocOne(desc, propertyType);
        }
        
        //field.setAccessible(true);
        
        prop.setOwningType(beanType);

        prop.setName(field.getName());
        
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
    
	/**
	 * Determine the type of the List,Set or Map. Not been set explicitly so
	 * determine this from ParameterizedType.
	 */
	private Class<?> determineTargetType(Field field) {
		
		Type genType = field.getGenericType();
		if (genType instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) genType;

			Type[] typeArgs = ptype.getActualTypeArguments();
			if (typeArgs.length == 1) {
				// probably a Set or List
				return (Class<?>) typeArgs[0];
			}
			if (typeArgs.length == 2) {
				// this is probably a Map
				return (Class<?>) typeArgs[1];
			}
		}
		// if targetType is null, then must be set in annotations
		return null;
	}
}
