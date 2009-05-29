package com.avaje.ebean.server.ddl;

import java.sql.Types;

import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.TableJoinColumn;
import com.avaje.ebean.server.deploy.id.ImportedId;
import com.avaje.ebean.server.type.ScalarType;

public class CreateTableColumnVisitor extends BaseTablePropertyVisitor {

	final DdlGenContext ctx;
	
	final DdlSyntax ddl;
	
	final int columnNameWidth;
	
	final CreateTableVisitor parent;
	
	public CreateTableColumnVisitor(CreateTableVisitor parent, DdlGenContext ctx){
		this.parent = parent;
		this.ctx = ctx;
		this.ddl = ctx.getDdlSyntax();
		this.columnNameWidth = ddl.getColumnNameWidth();
	}
	
	@Override
	public void visitEmbeddedScalar(BeanProperty p, BeanPropertyAssocOne<?> embedded) {

		visitScalar(p);
	}

	@Override
	public void visitOneImported(BeanPropertyAssocOne<?> p) {
		
		ImportedId importedId = p.getImportedId();
		
		TableJoinColumn[] columns = p.getTableJoin().columns();
		
		for (int i = 0; i < columns.length; i++) {
			String dbCol = columns[i].getLocalDbColumn();
			parent.writeColumnName(dbCol, p);
			
			BeanProperty importedProperty = importedId.findMatchImport(dbCol);
			if (importedProperty != null){

				DbType dbType = getDbType(importedProperty);
				String columnDefn = importedProperty.renderDbType(dbType);
				ctx.write(columnDefn);

			} else {
				throw new RuntimeException("Imported BeanProperty not found?");
			}
			
			if (!p.isNullable()){
				ctx.write(" not null");		
			}
			ctx.write(",").writeNewLine();
		}
	}

	@Override
	public void visitScalar(BeanProperty p) {

		parent.writeColumnName(p.getDbColumn(), p);
		
		DbType dbType = getDbType(p);
		String columnDefn = p.renderDbType(dbType);
		ctx.write(columnDefn);

		if (!p.isNullable()) {
			ctx.write(" not null");		
		}
		
		if (isIdentity(p)){
			writeIdentity();
		}
		
		parent.writeConstraint(p);
		
		ctx.write(",").writeNewLine();

	}
	
	
	protected void writeIdentity() {
		String identity = ddl.getIdentity();
		if (identity != null && identity.length() > 0){
			ctx.write(" ").write(identity);
		}
	}

	protected boolean isIdentity(BeanProperty p) {
		if (p.isId()){
			int jdbcType = p.getScalarType().getJdbcType();
			if (jdbcType == Types.INTEGER 
				|| jdbcType == Types.BIGINT
				|| jdbcType == Types.SMALLINT){
				
				return true;
			}
		}
		return false;
	}
	
	protected DbType getDbType(BeanProperty p) {

		ScalarType scalarType = p.getScalarType();
		if (scalarType == null){
			throw new RuntimeException("No scalarType for "+p.getFullBeanName());
		}
		return ctx.getDbTypeMap().get(scalarType.getJdbcType());
	}

}