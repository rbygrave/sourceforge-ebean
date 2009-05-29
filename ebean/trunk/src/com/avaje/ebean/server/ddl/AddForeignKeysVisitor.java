package com.avaje.ebean.server.ddl;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.TableJoinColumn;

/**
 * Used to generate the foreign key DDL and related indexes.
 */
public class AddForeignKeysVisitor implements BeanVisitor {

	final DdlGenContext ctx;
	
	final FkeyPropertyVisitor pv;
	
	public AddForeignKeysVisitor(DdlGenContext ctx) {
		this.ctx = ctx;
		this.pv = new FkeyPropertyVisitor(this, ctx);
	}

	protected String getForeignKeyName(BeanPropertyAssocOne<?> p) {

		return "fk_"+p.getBeanDescriptor().getBaseTable()+"_"+p.getName();
	}
	
	protected String getIndexName(BeanPropertyAssocOne<?> p) {

		return "idx_"+p.getBeanDescriptor().getBaseTable()+"_"+p.getName();
	}

	public void visitBean(BeanDescriptor<?> descriptor) {
	}

	public void visitBeanEnd(BeanDescriptor<?> descriptor) {
	}

	public void visitBegin() {
	}

	public void visitEnd() {
	}

	public PropertyVisitor visitProperty(BeanProperty p) {
		return pv;
	}

	
	public static class FkeyPropertyVisitor extends BaseTablePropertyVisitor {

		final DdlGenContext ctx;

		final AddForeignKeysVisitor parent;
		
		public FkeyPropertyVisitor(AddForeignKeysVisitor parent, DdlGenContext ctx) {
			this.parent = parent;
			this.ctx = ctx;
		}
		
		@Override
		public void visitEmbeddedScalar(BeanProperty p, BeanPropertyAssocOne<?> embedded) {
			// not interested
		}

		@Override
		public void visitOneImported(BeanPropertyAssocOne<?> p) {
			
			// alter table {basetable} add foreign key (...) references {} (...) on delete restrict on update restrict;
			// Alter table o_address add Foreign Key (country_code) references o_country (code) on delete  restrict on update  restrict;
						
			String baseTable = p.getBeanDescriptor().getBaseTable();
			
			TableJoin tableJoin = p.getTableJoin();
			
			TableJoinColumn[] columns = tableJoin.columns();
			
			String fkName = parent.getForeignKeyName(p);
			
			ctx.write("alter table ").write(baseTable).write(" add ");
			if (fkName != null) {
				ctx.write("constraint ").write(fkName).write(" ");
			}
			ctx.write("foreign key (");
			for (int i = 0; i < columns.length; i++) {
				if (i > 0){
					ctx.write(",");
				}
				ctx.write(columns[i].getLocalDbColumn());
			}
			ctx.write(")");
			
			ctx.write(" references ");
			ctx.write(tableJoin.getTable());
			ctx.write(" (");
			for (int i = 0; i < columns.length; i++) {
				if (i > 0){
					ctx.write(",");
				}
				ctx.write(columns[i].getForeignDbColumn());
			}
			ctx.write(")");
			
			String fkeySuffix = ctx.getDdlSyntax().getForeignKeySuffix();
			if (fkeySuffix != null){
				ctx.write(" ").write(fkeySuffix);				
			}
			ctx.write(";").writeNewLine();
			
			if (ctx.getDdlSyntax().isRenderIndexForFkey()){

				//create index idx_fk_o_address_ctry on o_address(country_code);
				ctx.write("create index ");
				
				String idxName = parent.getIndexName(p);
				if (idxName != null){
					ctx.write(idxName);
				}

				ctx.write(" on ").write(baseTable).write(" (");
				for (int i = 0; i < columns.length; i++) {
					if (i > 0){
						ctx.write(",");
					}
					ctx.write(columns[i].getLocalDbColumn());
				}
				ctx.write(");").writeNewLine();
			}			
		}

		@Override
		public void visitScalar(BeanProperty p) {
			// not interested
		}
	}

}
