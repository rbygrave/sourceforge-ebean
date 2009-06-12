package com.avaje.ebean.server.ddl;

import java.sql.Types;

import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.TableJoinColumn;
import com.avaje.ebean.server.deploy.id.ImportedId;

/**
 * Used as part of CreateTableVisitor to generated the create table DDL script.
 */
public class CreateTableColumnVisitor extends BaseTablePropertyVisitor {

	final DdlGenContext ctx;

	final DdlSyntax ddl;

	final int columnNameWidth;

	final CreateTableVisitor parent;

	public CreateTableColumnVisitor(CreateTableVisitor parent, DdlGenContext ctx) {
		this.parent = parent;
		this.ctx = ctx;
		this.ddl = ctx.getDdlSyntax();
		this.columnNameWidth = ddl.getColumnNameWidth();
	}

	
	@Override
	public void visitMany(BeanPropertyAssocMany<?> p) {
		if (p.isManyToMany()){
			TableJoin intersectionTableJoin = p.getIntersectionTableJoin();
			
			// check if the intersection table has already been created
			if (ctx.createIntersectionTable(intersectionTableJoin.getTable())){
				
				CreateIntersectionTable ic = new CreateIntersectionTable(ctx, p);
				String ct = ic.build();

			}
			
		}
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
			if (importedProperty != null) {

				String columnDefn = ctx.getColumnDefn(importedProperty);
				ctx.write(columnDefn);

			} else {
				throw new RuntimeException("Imported BeanProperty not found?");
			}

			if (!p.isNullable()) {
				ctx.write(" not null");
			}
			ctx.write(",").writeNewLine();
		}
	}

	@Override
	public void visitScalar(BeanProperty p) {

		parent.writeColumnName(p.getDbColumn(), p);

		String columnDefn = ctx.getColumnDefn(p);
		ctx.write(columnDefn);

		if (!p.isNullable()) {
			ctx.write(" not null");
		}

		if (isIdentity(p)) {
			writeIdentity();
		}

		parent.writeConstraint(p);

		ctx.write(",").writeNewLine();

	}

	protected void writeIdentity() {
		String identity = ddl.getIdentity();
		if (identity != null && identity.length() > 0) {
			ctx.write(" ").write(identity);
		}
	}

	protected boolean isIdentity(BeanProperty p) {
		if (p.isId()) {
			int jdbcType = p.getScalarType().getJdbcType();
			if (jdbcType == Types.INTEGER || jdbcType == Types.BIGINT || jdbcType == Types.SMALLINT) {

				return true;
			}
		}
		return false;
	}


}