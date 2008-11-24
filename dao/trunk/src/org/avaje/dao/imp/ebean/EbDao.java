package org.avaje.dao.imp.ebean;

import org.avaje.dao.Dao;
import org.avaje.dao.DaoQuery;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;

public class EbDao<T> implements Dao<T> {

	final String serverName;
	
	final Class<T> beanType;
	
	public EbDao(String serverName, Class<T> beanType){
		this.serverName = serverName;
		this.beanType = beanType;
	}
	
	
	public DaoQuery<T> query() {
		
		Query<T> query = Ebean.getServer(serverName).createQuery(beanType);
		return new EbDaoQuery<T>(query);
	}


	public T find(Object id) {
		return Ebean.getServer(serverName).find(beanType, id);
	}

	public T reference(Object id) {
		return Ebean.getServer(serverName).getReference(beanType, id);
	}

	public void save(Object o) {
		Ebean.getServer(serverName).save(o);
	}

	public void delete(Object o) {
		Ebean.getServer(serverName).delete(o);
	}
	
}
