package com.avaje.ebean.server.ddl;

import java.util.logging.Logger;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.parse.SqlReservedWords;

/**
 * Used to generated the create table DDL script.
 */
public class CreateTableVisitor implements BeanVisitor {
	
	protected static final Logger logger = Logger.getLogger(CreateTableVisitor.class.getName());
	
	final DdlGenContext ctx;
	
	final PropertyVisitor pv;
	
	final DdlSyntax ddl;
	
	final int columnNameWidth;
	
	public CreateTableVisitor(DdlGenContext ctx) {
		this.ctx = ctx;
		this.ddl = ctx.getDdlSyntax();
		this.columnNameWidth = ddl.getColumnNameWidth();
		this.pv = new CreateTableColumnVisitor(this, ctx);
	}

	/**
	 * Write the table name including a check for SQL reserved words.
	 */
	protected void writeTableName(BeanDescriptor<?> descriptor) {
		
		String tableName = descriptor.getBaseTable();
		
		if (SqlReservedWords.isKeyword(tableName)) {
			logger.warning("Table name ["+tableName+"] is a suspected SQL reserved word for bean "+descriptor.getFullName());
		}

		ctx.write(tableName);		
	}

	/**
	 * Write the column name including a check for the SQL reserved words.
	 * @param p
	 */
	protected void writeColumnName(String columnName, BeanProperty p) {
		
		if (SqlReservedWords.isKeyword(columnName)) {
			logger.warning("Column name ["+columnName+"] is a suspected SQL reserved word for property "+p.getFullBeanName());
		}

		ctx.write("  ").write(columnName, columnNameWidth).write(" ");
	}
	
	protected void writeConstraint(BeanProperty p) {
		
		String constraintExpression = p.getDbConstraintExpression();
		
		if (constraintExpression != null){
//			ctx.write(",");
//			ctx.write(" ").write("constraint ");
//			ctx.write("chk_").write(p.getDbColumn());
//			ctx.write(" ").write(constraintExpression);

			ctx.write(" ").write(constraintExpression);
		}
	}
	
	protected void writePrimaryKeyName(BeanDescriptor<?> descriptor) {
		
		if (ddl.isRenderPrimaryKeyName()){
			String pkName = ddl.getPrimaryKeyName(descriptor);
			if (pkName != null && pkName.length() > 0){
				ctx.write(pkName).write(" ");
			}
		}
	}
	
	public void visitBean(BeanDescriptor<?> descriptor) {
		ctx.write("create table ");
		writeTableName(descriptor);
		ctx.write(" (").writeNewLine();
	}
	
	public void visitBeanEnd(BeanDescriptor<?> descriptor) {
		
		ctx.write("  primary key ");
		writePrimaryKeyName(descriptor);
		
		ctx.write("(");
		
		BeanProperty[] ids = descriptor.propertiesId();
		VisitorUtil.visit(ids, new AbstractPropertyVisitor() {

			@Override
			public void visitEmbeddedScalar(BeanProperty p, BeanPropertyAssocOne<?> embedded) {
				ctx.write(p.getDbColumn()).write(", ");
			}

			@Override
			public void visitScalar(BeanProperty p) {
				ctx.write(p.getDbColumn()).write(", ");
			}
			
		});
		
		// remove the last comma, end of PK
		ctx.removeLast().write(")"); 
		
		// end of table
		ctx.write(")").writeNewLine(); 
		ctx.write(";").writeNewLine().writeNewLine();
	}
	
	public void visitBeanDescriptorEnd() {
		ctx.write(");").writeNewLine().writeNewLine();
	}


	public PropertyVisitor visitProperty(BeanProperty p) {
		return pv;
	}

	public void visitBegin() {
		
	}

	public void visitEnd() {
		
	}

}
