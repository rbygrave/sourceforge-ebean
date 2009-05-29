package com.avaje.ebean.server.ddl;

import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;

public abstract class BaseTablePropertyVisitor implements PropertyVisitor {

	/**
	 * Not required in that you can use the visitEmbeddedScalar.
	 */
	public void visitEmbedded(BeanPropertyAssocOne<?> p) {
	}

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

	public abstract void visitOneImported(BeanPropertyAssocOne<?> p);

	public abstract void visitScalar(BeanProperty p);

	
}
