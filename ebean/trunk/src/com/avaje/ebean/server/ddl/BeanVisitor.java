package com.avaje.ebean.server.ddl;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;

public interface BeanVisitor {

	public void visitBegin();
	
	public void visitBean(BeanDescriptor<?> descriptor);

	public PropertyVisitor visitProperty(BeanProperty p);

	public void visitBeanEnd(BeanDescriptor<?> descriptor);

	
//	public void visitPropertyMany(BeanPropertyAssocMany<?> p);
//	public void visitPropertyOneImported(BeanPropertyAssocOne<?> p);
//	public void visitPropertyOneExported(BeanPropertyAssocOne<?> p);
//	public void visitPropertyEmbedded(BeanPropertyAssocOne<?> p);
//	public void visitPropertyEmbeddedScalar(BeanProperty p, BeanPropertyAssocOne<?> embedded);
//	public void visitPropertyScalar(BeanProperty p);

	public void visitEnd();

}
