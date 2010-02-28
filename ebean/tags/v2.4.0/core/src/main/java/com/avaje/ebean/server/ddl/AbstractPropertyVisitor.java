package com.avaje.ebean.server.ddl;

import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.BeanPropertyCompound;

/**
 * Has no implementation. Can be used as a base object so that
 * selective methods can be implemented. 
 */
public abstract class AbstractPropertyVisitor implements PropertyVisitor {

	public void visitEmbedded(BeanPropertyAssocOne<?> p) {
	}

	public void visitEmbeddedScalar(BeanProperty p, BeanPropertyAssocOne<?> embedded) {
	}

	public void visitMany(BeanPropertyAssocMany<?> p) {	
	}

	public void visitOneExported(BeanPropertyAssocOne<?> p) {
	}

	public void visitOneImported(BeanPropertyAssocOne<?> p) {	
	}

	public void visitScalar(BeanProperty p) {
	}

    public void visitCompound(BeanPropertyCompound p) {    
    }

    public void visitCompoundScalar(BeanPropertyCompound compound, BeanProperty p) {
    }

	
	
	
}
