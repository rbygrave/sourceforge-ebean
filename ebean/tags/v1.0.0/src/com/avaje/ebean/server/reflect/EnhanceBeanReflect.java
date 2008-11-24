package com.avaje.ebean.server.reflect;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import javax.persistence.PersistenceException;

import com.avaje.ebean.bean.EntityBean;

/**
 * A BeanReflect implementation based on the enhancement that creates EntityBean
 * implementations.
 * <p>
 * That is, based on the fact that instances of the class passed in implement
 * the EntityBean interface.
 * </p>
 */
public final class EnhanceBeanReflect implements BeanReflect {

	private static final Object[] constuctorArgs = new Object[0];

	final Class<?> clazz;
	final EntityBean entityBean;
	final Constructor<?> constructor;

	public EnhanceBeanReflect(Class<?> clazz) {
		try {
			this.clazz = clazz;
			this.entityBean = (EntityBean) clazz.newInstance();
			constructor = defaultConstructor(clazz);

		} catch (InstantiationException e) {
			throw new PersistenceException(e);
		} catch (IllegalAccessException e) {
			throw new PersistenceException(e);
		}
	}

	private Constructor<?> defaultConstructor(Class<?> cls) {
		try {
			Class<?>[] params = new Class[0];
			return cls.getConstructor(params);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public Object createEntityBean() {
		try {
			return constructor.newInstance(constuctorArgs);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public Object createVanillaBean() {
		return createEntityBean();
	}

	private int getFieldIndex(String fieldName) {
		String[] fields = entityBean._ebean_getFieldNames();
		for (int i = 0; i < fields.length; i++) {
			if (fieldName.equals(fields[i])) {
				return i;
			}
		}
		String fieldList = Arrays.toString(fields);
		String msg = "field [" + fieldName + "] not found in [" + clazz.getName() + "]" + fieldList;
		throw new PersistenceException(msg);
	}

	public BeanReflectGetter getGetter(String name) {
		int i = getFieldIndex(name);
		return new Getter(i, entityBean);
	}

	public BeanReflectSetter getSetter(String name) {
		int i = getFieldIndex(name);
		return new Setter(i, entityBean);
	}

	static final class Getter implements BeanReflectGetter {
		final int fieldIndex;
		final EntityBean entityBean;

		Getter(int fieldIndex, EntityBean entityBean) {
			this.fieldIndex = fieldIndex;
			this.entityBean = entityBean;
		}

		public Object get(Object bean) {
			return entityBean._ebean_getField(fieldIndex, bean);
		}

		public Object getIntercept(Object bean) {
			return entityBean._ebean_getFieldIntercept(fieldIndex, bean);
		}
	}

	static final class Setter implements BeanReflectSetter {
		final int fieldIndex;
		final EntityBean entityBean;

		Setter(int fieldIndex, EntityBean entityBean) {
			this.fieldIndex = fieldIndex;
			this.entityBean = entityBean;
		}

		public void set(Object bean, Object value) {
			entityBean._ebean_setField(fieldIndex, bean, value);
		}

		public void setIntercept(Object bean, Object value) {
			entityBean._ebean_setFieldIntercept(fieldIndex, bean, value);
		}

	}
}
