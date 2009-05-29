package com.avaje.ebean.server.ddl;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;

public class DropTableVisitor implements BeanVisitor {

	final DdlGenContext ctx;
	
	final DdlSyntax ddlSyntax;

	public DropTableVisitor(DdlGenContext ctx) {
		this.ctx = ctx;
		this.ddlSyntax = ctx.getDdlSyntax();
	}

	protected void writeDropTable(BeanDescriptor<?> descriptor){
		ctx.write("drop table ");
		ctx.write(descriptor.getBaseTable());
		
		if (ddlSyntax.getDropTableCascade() != null){
			ctx.write(" ").write(ddlSyntax.getDropTableCascade());
		}
	}
	
	public void visitBean(BeanDescriptor<?> descriptor) {
		writeDropTable(descriptor);
		ctx.write(";").writeNewLine().writeNewLine();
	}

	public void visitBeanEnd(BeanDescriptor<?> descriptor) {
	}

	public void visitBegin() {	
		
		if (ddlSyntax.getDisableReferentialIntegrity() != null){
			ctx.write(ddlSyntax.getDisableReferentialIntegrity());
			ctx.write(";").writeNewLine().writeNewLine();
		}
	}

	public void visitEnd() {	
		if (ddlSyntax.getEnableReferentialIntegrity() != null){
			ctx.write(ddlSyntax.getEnableReferentialIntegrity());
			ctx.write(";").writeNewLine().writeNewLine();
		}
	}

	public PropertyVisitor visitProperty(BeanProperty p) {
		// Return null as we are not interested in properties
		return null;
	}
	
}
