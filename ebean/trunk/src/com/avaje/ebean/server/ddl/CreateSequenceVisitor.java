package com.avaje.ebean.server.ddl;

import com.avaje.ebean.config.dbplatform.DbDdlSyntax;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;

/**
 * Used to generate the drop table DDL script.
 */
public class CreateSequenceVisitor implements BeanVisitor {

	final DdlGenContext ctx;
	
	final DbDdlSyntax ddlSyntax;

	public CreateSequenceVisitor(DdlGenContext ctx) {
		this.ctx = ctx;
		this.ddlSyntax = ctx.getDdlSyntax();
	}
	
	public void visitBean(BeanDescriptor<?> descriptor) {
		
		if (descriptor.getSequenceName() != null) {
			ctx.write("create sequence ");
			ctx.write(descriptor.getSequenceName());			
			ctx.write(";").writeNewLine().writeNewLine();
		}
	}

	
	public void visitBeanEnd(BeanDescriptor<?> descriptor) {
	}

	public void visitBegin() {	
	}

	public void visitEnd() {	
	}

	public PropertyVisitor visitProperty(BeanProperty p) {
		// Return null as we are not interested in properties
		return null;
	}
	
}
