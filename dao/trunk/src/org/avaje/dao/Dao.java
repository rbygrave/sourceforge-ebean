package org.avaje.dao;

public interface Dao<T> {

	public DaoQuery<T> query();
	
	public T find(Object id);
	
	public T reference(Object id);
	
	public void save(Object o);
	
	public void delete(Object o);
	
	//public void validate(T o);
	
}
