package com.avaje.ebean.server.ddl;

import java.sql.Types;

import com.avaje.ebean.config.dbplatform.DbDdlSyntax;
import com.avaje.ebean.config.dbplatform.IdType;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.BeanPropertyCompound;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.TableJoinColumn;
import com.avaje.ebean.server.deploy.id.ImportedId;

/**
 * Used as part of CreateTableVisitor to generated the create table DDL script.
 */
public class CreateTableColumnVisitor extends BaseTablePropertyVisitor {

	private final DdlGenContext ctx;

	private final DbDdlSyntax ddl;

	private final CreateTableVisitor parent;

	public CreateTableColumnVisitor(CreateTableVisitor parent, DdlGenContext ctx) {
		this.parent = parent;
		this.ctx = ctx;
		this.ddl = ctx.getDdlSyntax();
	}

	
	@Override
	public void visitMany(BeanPropertyAssocMany<?> p) {
		if (p.isManyToMany()){
			if (p.getMappedBy() != null) {
				// only create on other 'owning' side
				
			} else {
				TableJoin intersectionTableJoin = p.getIntersectionTableJoin();
				
				// check if the intersection table has already been created
				String intTable = intersectionTableJoin.getTable();
				if (ctx.isProcessIntersectionTable(intTable)){
					// build the create table and fkey constraints 
					// putting the DDL into ctx for later output as we are 
					// in the middle of rendering the create table DDL
					new CreateIntersectionTable(ctx, p).build();
				}
			}
		}
	}

	
	public void visitCompoundScalar(BeanPropertyCompound compound, BeanProperty p) {
        visitScalar(p);
    }


    public void visitCompound(BeanPropertyCompound p) {
        // do nothing
    }


    @Override
	public void visitEmbeddedScalar(BeanProperty p, BeanPropertyAssocOne<?> embedded) {

		visitScalar(p);
	}

	@Override
	public void visitOneImported(BeanPropertyAssocOne<?> p) {

		ImportedId importedId = p.getImportedId();

		TableJoinColumn[] columns = p.getTableJoin().columns();
		if (columns.length == 0){
			String msg = "No join columns for "+p.getFullBeanName();
			throw new RuntimeException(msg);
		}
		
		String uqConstraintName = "uq_"
			+ p.getBeanDescriptor().getBaseTable()
			+ "_"+columns[0].getLocalDbColumn();
		
		if (uqConstraintName.length() > ddl.getMaxConstraintNameLength()){
			uqConstraintName = uqConstraintName.substring(0, ddl.getMaxConstraintNameLength());
		}
		
		StringBuilder constraintExpr = new StringBuilder();
		constraintExpr.append("constraint ")
			.append(uqConstraintName)
			.append(" unique (");
		
		for (int i = 0; i < columns.length; i++) {
			
			String dbCol = columns[i].getLocalDbColumn();

			if (i > 0){
				constraintExpr.append(", ");
			}
			constraintExpr.append(dbCol);
			
			if (parent.wroteColumns.contains(dbCol)) {
				continue;
			}
			
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
			parent.wroteColumns.add(p.getDbColumn());
		}
		constraintExpr.append(")");
		
		if (p.isOneToOne()){
			if (ddl.isAddOneToOneUniqueContraint()){
				parent.writeConstraint( constraintExpr.toString());
			}
		}
		
	}

	@Override
	public void visitScalar(BeanProperty p) {

		if (parent.wroteColumns.contains(p.getDbColumn())) {
			return;
		}
		
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
		
		parent.wroteColumns.add(p.getDbColumn());
	}

	protected void writeIdentity() {
		String identity = ddl.getIdentity();
		if (identity != null && identity.length() > 0) {
			ctx.write(" ").write(identity);
		}
	}

	protected boolean isIdentity(BeanProperty p) {
		
		IdType idType = ctx.getDbPlatform().getDbIdentity().getIdType();
		if (!idType.equals(IdType.IDENTITY)){
			// this dbPlatform probably uses Sequences
			return false;
		}
		
		if (p.isId()) {
			int jdbcType = p.getScalarType().getJdbcType();
			if (jdbcType == Types.INTEGER || jdbcType == Types.BIGINT || jdbcType == Types.SMALLINT) {

				return true;
			}
		}
		return false;
	}


}