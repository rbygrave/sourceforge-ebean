package com.avaje.ebean.server.deploy;

import java.sql.ResultSet;

import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.server.core.ReferenceOptions;

/**
 * Context provided when a BeanProperty reads from a ResultSet.
 * 
 * @see {@link BeanProperty#readSet(DbReadContext, Object)}
 * @see {@link BeanProperty#read(DbReadContext)}
 */
public interface DbReadContext {
	
	/**
	 * Return the reference options for a given bean property.
	 */
	public ReferenceOptions getReferenceOptionsFor(BeanPropertyAssocOne<?> beanProperty);
	
	/**
	 * Set the JoinNode - used by proxy/reference beans for profiling.
	 */
	public void setCurrentPrefix(String currentPrefix);

		
	/**
	 * Return true if we are profiling this query.
	 */
	public boolean isAutoFetchProfiling();

	/**
	 * Create a AutoFetchNode for a given path.
	 */
	public ObjectGraphNode createAutoFetchNode(String extraPath, String prefix);
	
	/**
	 * Add autoFetch profiling for a loaded entity bean.
	 */
	public void profileBean(EntityBeanIntercept ebi, String extraPath, String prefix);

	/**
	 * Add autoFetch profiling for a proxy/reference bean.
	 */
	public void profileReference(EntityBeanIntercept ebi, String extraPath);
	
	/**
	 * Return the ResultSet being read from.
	 */
	public ResultSet getRset();

	/**
	 * Return the next column index in the ResultSet.
	 */
	public int nextRsetIndex();

	/**
	 * Return the persistence context. 
	 */
	public PersistenceContext getPersistenceContext();

	/**
	 * Return the property that is associated with the many. There can only be
	 * one. This can be null.
	 */
	public BeanPropertyAssocMany<?> getManyProperty();

	/**
	 * Set back the bean that has just been loaded with its id.
	 */
	public void setLoadedBean(Object loadedBean, Object id);

	/**
	 * Set back the 'detail' bean that has just been loaded.
	 */
	public void setLoadedManyBean(Object loadedBean);
}
