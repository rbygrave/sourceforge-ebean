package org.avaje.dao;

public class BaseDao<T> implements Dao<T> {

	protected final Dao<T> base;
	
	public BaseDao(Dao<T> base) {
		this.base = base;
	}
	
	public DaoQuery<T> query(){
		return base.query();
	}
	
	public void delete(Object o) {
		base.delete(o);
	}

	public T find(Object id) {
		return base.find(id);
	}

	public T reference(Object id) {
		return base.reference(id);
	}

	public void save(Object o) {
		base.save(o);
	}

	
}
