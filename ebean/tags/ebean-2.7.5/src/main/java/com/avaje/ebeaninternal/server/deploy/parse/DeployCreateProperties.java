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
package com.avaje.ebeaninternal.server.deploy.parse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.persistence.Transient;

import com.avaje.ebean.config.ScalarTypeConverter;
import com.avaje.ebeaninternal.server.core.Message;
import com.avaje.ebeaninternal.server.deploy.DetermineManyType;
import com.avaje.ebeaninternal.server.deploy.ManyType;
import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanPropertyAssocOne;
import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanPropertyCompound;
import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanPropertySimpleCollection;
import com.avaje.ebeaninternal.server.type.CtCompoundType;
import com.avaje.ebeaninternal.server.type.ScalaOptionTypeConverter;
import com.avaje.ebeaninternal.server.type.ScalarType;
import com.avaje.ebeaninternal.server.type.TypeManager;
import com.avaje.ebeaninternal.server.type.reflect.CheckImmutableResponse;

/**
 * Create the properties for a bean.
 * <p>
 * This also needs to determine if the property is a associated many, associated
 * one or normal scalar property.
 * </p>
 */
public class DeployCreateProperties {

	private static final Logger logger = Logger.getLogger(DeployCreateProperties.class.getName());

	private final Class<?> scalaOptionClass;
	/**
	 * Use to wrap and unwrap Scala Option.
	 */
    @SuppressWarnings("rawtypes")
    private final ScalarTypeConverter scalaOptionTypeConverter;
    
    private final DetermineManyType determineManyType;

	private final TypeManager typeManager;
	
    @SuppressWarnings("rawtypes")
    public DeployCreateProperties(TypeManager typeManager) {
    	this.typeManager = typeManager;
    	
    	Class<?> tmpOptionClass = DetectScala.getScalaOptionClass();

        if (tmpOptionClass == null){
            scalaOptionClass = null;
            scalaOptionTypeConverter = null;
        } else {
            scalaOptionClass = tmpOptionClass;
            scalaOptionTypeConverter = new ScalaOptionTypeConverter();
        }
        
        this.determineManyType = new DetermineManyType(tmpOptionClass != null);
    }
    
    /**
     * Create the appropriate properties for a bean.
     */
    public void createProperties(DeployBeanDescriptor<?> desc) {
    	
        createProperties(desc, desc.getBeanType(), 0);
        desc.sortProperties();
        
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

        boolean scalaObject = desc.isScalaObject();

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

            		Method getter = findGetter(field, initFieldName, declaredMethods, scalaObject);
            		Method setter = findSetter(field, initFieldName, declaredMethods, scalaObject);
            		
	                DeployBeanProperty prop = createProp(level, desc, field, beanType, getter, setter);
	                if (prop == null){
	                	// transient annotation on unsupported type
	                	
	                } else {
		                // set a order that gives priority to inherited properties
		                // push Id/EmbeddedId up and CreatedTimestamp/UpdatedTimestamp down
		                int sortOverride = prop.getSortOverride();
		                prop.setSortOrder((level*10000+100-i + sortOverride));
		                
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
    private Method findGetter(Field field, String initFieldName, Method[] declaredMethods, boolean scalaObject){
 
    	String methGetName = "get"+initFieldName;
    	String methIsName = "is"+initFieldName;
    	String scalaGet = field.getName();
  
    	for (int i = 0; i < declaredMethods.length; i++) {
    		Method m = declaredMethods[i];
			if ((scalaObject && m.getName().equals(scalaGet)) 
			        || m.getName().equals(methGetName) || m.getName().equals(methIsName)){
			    
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
    private Method findSetter(Field field, String initFieldName, Method[] declaredMethods, boolean scalaObject){
    	
    	String methSetName = "set"+initFieldName;
    	String scalaSetName = field.getName()+"_$eq";
    	
    	for (int i = 0; i < declaredMethods.length; i++) {
    		Method m = declaredMethods[i];
    		
			if ((scalaObject && m.getName().equals(scalaSetName)) 
			        || m.getName().equals(methSetName)){
			    
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
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private DeployBeanProperty createManyType(DeployBeanDescriptor<?> desc, Class<?> targetType, ManyType manyType) {

        ScalarType<?> scalarType = typeManager.getScalarType(targetType);
        if (scalarType != null) {
            return new DeployBeanPropertySimpleCollection(desc, targetType, scalarType, manyType);
        }
        //TODO: Handle Collection of CompoundType and Embedded Type
        return new DeployBeanPropertyAssocMany(desc, targetType, manyType);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private DeployBeanProperty createProp(DeployBeanDescriptor<?> desc, Field field) {
        
        Class<?> propertyType = field.getType();
        Class<?> innerType = propertyType;
        ScalarTypeConverter<?, ?> typeConverter = null;
        
        if (propertyType.equals(scalaOptionClass)){
            innerType = determineTargetType(field);
            typeConverter = scalaOptionTypeConverter;
        }
        
        // check for Collection type (list, set or map)
        ManyType manyType = determineManyType.getManyType(propertyType);

        if (manyType != null) {
            // List, Set or Map based object
            Class<?> targetType = determineTargetType(field);
            if (targetType == null){
            	Transient transAnnotation = field.getAnnotation(Transient.class);
            	if (transAnnotation != null) {
            		// not supporting this field (generic type used)
            		return null;
            	}
        		logger.warning("Could not find parameter type (via reflection) on "+desc.getFullName()+" "+field.getName());
            }
            return createManyType(desc, targetType, manyType);
        } 
        
        if (innerType.isEnum() || innerType.isPrimitive()){
            return new DeployBeanProperty(desc, propertyType, null, typeConverter);
        }
        
        ScalarType<?> scalarType = typeManager.getScalarType(innerType);
        if (scalarType != null) {
            return new DeployBeanProperty(desc, propertyType, scalarType, typeConverter);
        }
        
        CtCompoundType<?> compoundType = typeManager.getCompoundType(innerType);
        if (compoundType != null) {
            return new DeployBeanPropertyCompound(desc, propertyType, compoundType, typeConverter);
        }
     
        if (!isTransientField(field)){
            try {
                CheckImmutableResponse checkImmutable = typeManager.checkImmutable(innerType);
                if (checkImmutable.isImmutable()){
                    if (checkImmutable.isCompoundType()){
                        // use reflection to support compound immutable value objects
                        typeManager.recursiveCreateScalarDataReader(innerType);
                        compoundType = typeManager.getCompoundType(innerType);
                        if (compoundType != null) {
                            return new DeployBeanPropertyCompound(desc, propertyType, compoundType, typeConverter);
                        }
                        
                    } else {
                        // use reflection to support simple immutable value objects
                        scalarType = typeManager.recursiveCreateScalarTypes(innerType);
                        return new DeployBeanProperty(desc, propertyType, scalarType, typeConverter);
                    }
                }
            } catch (Exception e){
                logger.log(Level.SEVERE, "Error with "+desc+" field:"+field.getName(), e);
            }
        }
        
        return new DeployBeanPropertyAssocOne(desc, propertyType);
    }
    
    private boolean isTransientField(Field field) {
        
        Transient t = field.getAnnotation(Transient.class);
        return (t != null);
    }
    
	private DeployBeanProperty createProp(int level, DeployBeanDescriptor<?> desc, Field field, Class<?> beanType, Method getter, Method setter) {
    	
        DeployBeanProperty prop = createProp(desc, field);
        if (prop == null){
        	// transient annotation on unsupported type
        	return null;
        } else {
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
			    if (typeArgs[0] instanceof Class<?>){
			        return (Class<?>) typeArgs[0];
			    }
			    throw new RuntimeException("Unexpected Parameterised Type? "+typeArgs[0]);
			}
			if (typeArgs.length == 2) {
				// this is probably a Map
				if (typeArgs[1] instanceof ParameterizedType) {
					// not supporting ParameterizedType on Map.
					return null;
				}
				return (Class<?>) typeArgs[1];
			}
		}
		// if targetType is null, then must be set in annotations
		return null;
	}
}
