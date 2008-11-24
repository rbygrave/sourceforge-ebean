package org.avaje.dao;

import org.avaje.dao.factory.DaoFactory;
import org.avaje.dao.imp.ebean.EbDaoFactory;


public class DaoManager {

	private static DaoFactory factory = new EbDaoFactory();
	
	public static <T> Dao<T> getGeneric(Class<T> beanType){
		return factory.getGeneric(beanType);
	}
	
	public static <T> T getSpecific(Class<T> daoType){
		return factory.getSpecific(daoType);
	}
}
