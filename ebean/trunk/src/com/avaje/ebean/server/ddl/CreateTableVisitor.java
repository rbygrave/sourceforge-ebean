package com.avaje.ebean.server.ddl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.avaje.ebean.config.dbplatform.DbDdlSyntax;
import com.avaje.ebean.config.dbplatform.DbType;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.InheritInfo;
import com.avaje.ebean.server.deploy.InheritInfoVisitor;
import com.avaje.ebean.server.deploy.parse.SqlReservedWords;

/**
 * Used to generated the create table DDL script.
 */
public class CreateTableVisitor implements BeanVisitor {
	
	protected static final Logger logger = Logger.getLogger(CreateTableVisitor.class.getName());
	
	final DdlGenContext ctx;
	
	final PropertyVisitor pv;
	
	final DbDdlSyntax ddl;
	
	final int columnNameWidth;

	// avoid writing columns twice, e.g. when used in associations with insertable=false and updateable=false
	final Set<String> wroteColumns = new HashSet<String>();

	ArrayList<String> checkConstraints = new ArrayList<String>();
	

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
			String propName = p == null ? "(Unknown)" : p.getFullBeanName();
			logger.warning("Column name ["+columnName+"] is a suspected SQL reserved word for property "+propName);
		}

		ctx.write("  ").write(columnName, columnNameWidth).write(" ");
	}
	
	/**
	 * Build a check constraint for the property if required.
	 * <p>
	 * Typically check constraint based on Enum mapping values.
	 * </p>
	 */
	protected void writeConstraint(BeanProperty p) {
		
		String constraintExpression = p.getDbConstraintExpression();
		
		if (constraintExpression != null){

			// build constraint clause 
			String s = "constraint ck_"+p.getBeanDescriptor().getBaseTable()
					+"_"+p.getDbColumn()+" "+constraintExpression;
			
			// add to list as we render all check constraints just prior to primary key
			checkConstraints.add(s);
		}
	}
		
	public boolean visitBean(BeanDescriptor<?> descriptor) {
		
		wroteColumns.clear();
		
		if (!descriptor.isInheritanceRoot()){
			return false;
		}
		 
		ctx.write("create table ");
		writeTableName(descriptor);
		ctx.write(" (").writeNewLine();
		
		InheritInfo inheritInfo = descriptor.getInheritInfo();
		if (inheritInfo != null && inheritInfo.isRoot()){
			String discColumn = inheritInfo.getDiscriminatorColumn();
			int discType = inheritInfo.getDiscriminatorType();
			int discLength = inheritInfo.getDiscriminatorLength();
			DbType dbType = ctx.getDbTypeMap().get(discType);
			String discDbType = dbType.renderType(discLength, 0);
			
			writeColumnName(discColumn, null);
			ctx.write(discDbType);
			ctx.write(" not null,");
			ctx.writeNewLine();
			
		}
		
		return true;
	}
	
	/**
	 * Helper used to visit all the inheritInfo/BeanDescriptor in
	 * the inheritance hierarchy (to add their 'local' properties).
	 */
	class InheritChildVisitor implements InheritInfoVisitor {

		public void visit(InheritInfo inheritInfo) {
			BeanProperty[] propertiesLocal = inheritInfo.getBeanDescriptor().propertiesLocal();
			VisitorUtil.visit(propertiesLocal, pv);			
		}
	}
	
	public void visitBeanEnd(BeanDescriptor<?> descriptor) {

		InheritInfo inheritInfo = descriptor.getInheritInfo();
		if (inheritInfo != null && inheritInfo.isRoot()){
			// add all properties on the children objects
			InheritChildVisitor childVisitor = new InheritChildVisitor();
			inheritInfo.visitChildren(childVisitor);
		}
		
		if (checkConstraints.size() > 0){
			for (String checkConstraint : checkConstraints) {
				ctx.write("  ").write(checkConstraint).write(",").writeNewLine();
			}
			checkConstraints = new ArrayList<String>();
		}
		
		String pkName = ddl.getPrimaryKeyName(descriptor);

		ctx.write("  constraint ").write(pkName).write(" primary key (");
		
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
		ctx.addIntersectionCreateTables();
	}

}
