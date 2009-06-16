package com.avaje.ebean.server.ddl;

import com.avaje.ebean.config.dbplatform.DbDdlSyntax;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;

/**
 * Used to generate the drop table DDL script.
 */
public class DropTableVisitor implements BeanVisitor {

	final DdlGenContext ctx;
	
	final DbDdlSyntax ddlSyntax;

	public DropTableVisitor(DdlGenContext ctx) {
		this.ctx = ctx;
		this.ddlSyntax = ctx.getDdlSyntax();
	}

	protected void writeDropTable(BeanDescriptor<?> descriptor){
		writeDropTable(descriptor.getBaseTable());
	}
	
	protected void writeDropTable(String tableName){
		ctx.write("drop table ");
		ctx.write(tableName);
		
		if (ddlSyntax.getDropTableCascade() != null){
			ctx.write(" ").write(ddlSyntax.getDropTableCascade());
		}
		ctx.write(";").writeNewLine().writeNewLine();
	}
	
	public void visitBean(BeanDescriptor<?> descriptor) {
		writeDropTable(descriptor);
		
		dropIntersectionTables(descriptor);
	}

	private void dropIntersectionTables(BeanDescriptor<?> descriptor) {
		
		BeanPropertyAssocMany<?>[] manyProps = descriptor.propertiesMany();
		for (int i = 0; i < manyProps.length; i++) {
			if (manyProps[i].isManyToMany()){
				String intTable = manyProps[i].getIntersectionTableJoin().getTable();
				if (ctx.isProcessIntersectionTable(intTable)) {
					writeDropTable(intTable);
				}
			}
		}
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
