package org.avaje.dao.factory;

import org.avaje.dao.Dao;

public interface DaoFactory {

	public <T> Dao<T> getGeneric(Class<T> beanType);
	
	public <T> T getSpecific(Class<T> daoType);
	
}
