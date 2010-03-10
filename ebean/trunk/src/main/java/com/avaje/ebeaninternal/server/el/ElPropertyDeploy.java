package com.avaje.ebeaninternal.server.el;

import com.avaje.ebeaninternal.server.deploy.BeanProperty;

/**
 * Used to parse expressions in queries (where, orderBy etc).
 * <p>
 * Maps an expression to deployment information such as 
 * the DB column and elPrefix/elPlaceHolder is used determine
 * joins and set place holders for table alias'.
 * </p>
 */
public interface ElPropertyDeploy {

	/**
	 * This is the elPrefix for all root level properties.
	 */
	public static final String ROOT_ELPREFIX = "${}";

	/**
	 * Return true if the property is on the path of a many property.
	 */
	public boolean containsMany();

	/**
	 * Return the prefix path of the property.
	 * <p>
	 * This is use to determine joins required to support
	 * this property.
	 * </p>
	 */
	public String getElPrefix();

	/**
	 * Return the place holder in the form of ${elPrefix}dbColumn.
	 * <p>
	 * The ${elPrefix} is replaced by the appropriate table alias.
	 * </p>
	 */
	public String getElPlaceholder(boolean encrypted);
	
	/**
	 * Return the name of the property.
	 */
	public String getName();
		
	/**
	 * Return the deployment db column for this property.
	 */
	public String getDbColumn();
	

    /**
     * Return the underlying bean property.
     */
    public BeanProperty getBeanProperty();
}
