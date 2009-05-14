package com.avaje.ebean.server.reflect;

/**
 * Creates a BeanReflectFactory based on the enhancement that 
 * creates EntityBean implementations.
 */
public final class EnhanceBeanReflectFactory implements BeanReflectFactory {

	public BeanReflect create(Class<?> vanillaType, Class<?> entityBeanType) {
		return new EnhanceBeanReflect(entityBeanType);
	}

	
}
