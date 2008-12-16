package com.avaje.ebean.server.deploy;

import java.sql.ResultSet;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.server.core.TransactionContextClass;
import com.avaje.ebean.server.deploy.jointree.JoinNode;

/**
 * Context provided when a BeanProperty reads from a ResultSet.
 * 
 * @see {@link BeanProperty#readSet(DbReadContext, Object)}
 * @see {@link BeanProperty#read(DbReadContext)}
 */
public interface DbReadContext {
	
	/**
	 * Set the JoinNode - used by proxy/reference beans for profiling.
	 */
	public void setCurrentJoinNode(JoinNode currentJoinNode);
	
	/**
	 * Return true if we are profiling this query.
	 */
	public boolean isAutoFetchProfiling();

	/**
	 * Create a AutoFetchNode for a given path.
	 */
	public ObjectGraphNode createAutoFetchNode(String extraPath, JoinNode joinNode);
	
	/**
	 * Add autoFetch profiling for a loaded entity bean.
	 */
	public void profileBean(EntityBeanIntercept ebi, String extraPath, JoinNode joinNode);

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
	 * Return the TransactionContextClass for a given bean type.
	 */
	public TransactionContextClass getClassContext(Class<?> beanType);

	/**
	 * Return the property that is associated with the many. There can only be
	 * one. This can be null.
	 */
	public BeanPropertyAssocMany getManyProperty();

	/**
	 * Set back the bean that has just been loaded with its id.
	 */
	public void setLoadedBean(EntityBean loadedBean, Object id);

	/**
	 * Set back the 'detail' bean that has just been loaded.
	 */
	public void setLoadedManyBean(EntityBean loadedBean);
}
