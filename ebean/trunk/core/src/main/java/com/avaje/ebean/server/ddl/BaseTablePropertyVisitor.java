package com.avaje.ebean.server.ddl;

import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;

/**
 * Used to help mark PropertyVisitor methods that need to be implemented
 * to visit base table properties.
 */
public abstract class BaseTablePropertyVisitor implements PropertyVisitor {

	/**
	 * Not required in that you can use the visitEmbeddedScalar.
	 */
	public void visitEmbedded(BeanPropertyAssocOne<?> p) {
	}

	/**
	 * Override this method.
	 */
	public abstract void visitEmbeddedScalar(BeanProperty p, BeanPropertyAssocOne<?> embedded);

	/**
	 * Not part of base table.
	 */
	public void visitMany(BeanPropertyAssocMany<?> p) {
	}

	/**
	 * Not part of base table.
	 */
	public void visitOneExported(BeanPropertyAssocOne<?> p) {
	}

	/**
	 * Override this method for the foreign key.
	 */
	public abstract void visitOneImported(BeanPropertyAssocOne<?> p);

	/**
	 * Override this method for normal scalar property.
	 */
	public abstract void visitScalar(BeanProperty p);

	
}
