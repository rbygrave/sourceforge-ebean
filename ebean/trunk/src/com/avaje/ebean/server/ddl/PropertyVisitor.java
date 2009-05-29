package com.avaje.ebean.server.ddl;

import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;

public interface PropertyVisitor {

	public void visitMany(BeanPropertyAssocMany<?> p);
	
	public void visitOneImported(BeanPropertyAssocOne<?> p);
	
	public void visitOneExported(BeanPropertyAssocOne<?> p);
	
	public void visitEmbedded(BeanPropertyAssocOne<?> p);
	
	public void visitEmbeddedScalar(BeanProperty p, BeanPropertyAssocOne<?> embedded);

	public void visitScalar(BeanProperty p);


}
