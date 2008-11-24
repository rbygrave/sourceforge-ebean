package org.avaje.dao.imp.ebean;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.avaje.dao.Dao;
import org.avaje.dao.factory.DaoFactory;


public class EbDaoFactory implements DaoFactory {

	public <T> Dao<T> getGeneric(Class<T> beanType) {
		// null is the ebean server name
		return new EbDao<T>(null, beanType);
	}

	@SuppressWarnings("unchecked")
	public <T> T getSpecific(Class<T> daoInterfaceType) {
		
		
		try {
			Class<?> impClass = getImpClass(daoInterfaceType);
			
			Dao<?> baseDao = createBaseDao(daoInterfaceType);

			return (T)createSpecific(impClass, Dao.class, baseDao);

		} catch (Exception e) {
			throw new RuntimeException(e);
		} 		
	}

	

	
	private Dao<?> createBaseDao(Class<?> daoInterfaceType) {
		Type paramType = getParamType(daoInterfaceType, Dao.class);
		Class<?> paramCls = (Class<?>)paramType;
		return getGeneric(paramCls);
	}
	/**
	 * Use naming convention to determine the implementation
	 * for a given Dao interface.
	 * @throws ClassNotFoundException 
	 */
	private Class<?> getImpClass(Class<?> daoInterfaceType) throws ClassNotFoundException {
	
		String className = daoInterfaceType.getName();
		String impClassName = className+"Imp";
		return Class.forName(impClassName);
	}

	private Object createSpecific(Class<?> cimp, Class<?> paramType, Object baseDao) throws Exception {
		
		Constructor<?> c = cimp.getConstructor(paramType);			
		return c.newInstance(baseDao);			
	}
	
	private static Type getParamType(Class<?> cls, Class<?> matchRawType) {
		
		Type[] gis = cls.getGenericInterfaces();
		for (int i = 0; i < gis.length; i++) {
			Type type = gis[i];
			if (type instanceof ParameterizedType) {
				ParameterizedType paramType = (ParameterizedType) type;
				Type rawType = paramType.getRawType();
				if (rawType.equals(matchRawType)) {
					
					Type[] typeArguments = paramType.getActualTypeArguments();
					
					return typeArguments[0];
				}
			}
		}
		return null;
	}

}
