package com.avaje.ebean.server.ddl;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.TableJoinColumn;

public class CreateIntersectionTable {

	StringBuilder sb = new StringBuilder();
	
	final DdlGenContext ctx;
	
	final BeanPropertyAssocMany<?> manyProp;
	final TableJoin intersectionTableJoin;
	final TableJoin tableJoin;
	
	final String NEW_LINE = "\n";
	
	public CreateIntersectionTable(DdlGenContext ctx, BeanPropertyAssocMany<?> manyProp) {
		this.ctx = ctx;
		this.manyProp = manyProp;
		this.intersectionTableJoin = manyProp.getIntersectionTableJoin();
		this.tableJoin = manyProp.getTableJoin();
	}
	
	
	
	public String build() {
		
		BeanDescriptor<?> localDesc = manyProp.getBeanDescriptor();
		BeanDescriptor<?> targetDesc = manyProp.getTargetDescriptor();
		
		sb.append("create table ");
		sb.append(intersectionTableJoin.getTable());
		sb.append(" (").append(NEW_LINE);
		
		StringBuilder pkeySb = new StringBuilder();
		
		TableJoinColumn[] columns = intersectionTableJoin.columns();
		for (int i = 0; i < columns.length; i++) {
			if (i  > 0){
				pkeySb.append(",");
			}
			pkeySb.append(columns[i].getForeignDbColumn());
			writeColumn(columns[i].getForeignDbColumn());
			
			BeanProperty p = localDesc.getIdBinder().findBeanProperty(columns[i].getLocalDbColumn());
			if (p == null){
				throw new RuntimeException("Could not find id property for "+columns[i].getLocalDbColumn());
			}
			
			String columnDefn = ctx.getColumnDefn(p);
			sb.append(columnDefn);
			sb.append("  ,").append(NEW_LINE);
		}
		
		TableJoinColumn[] otherColumns = tableJoin.columns();
		for (int i = 0; i < otherColumns.length; i++) {
			
			pkeySb.append(",");
			pkeySb.append(otherColumns[i].getLocalDbColumn());
			
			writeColumn(otherColumns[i].getLocalDbColumn());
			
			BeanProperty p = targetDesc.getIdBinder().findBeanProperty(otherColumns[i].getForeignDbColumn());
			if (p == null){
				throw new RuntimeException("Could not find id property for "+otherColumns[i].getForeignDbColumn());
			}
			
			String columnDefn = ctx.getColumnDefn(p);
			sb.append(columnDefn);
			
			sb.append("  ,").append(NEW_LINE);
		}
		
		sb.append("constraint pk_").append(intersectionTableJoin.getTable());
		sb.append(" primary key (").append(pkeySb.toString());
		sb.append("))").append(NEW_LINE).append(";").append(NEW_LINE);
		
		return sb.toString();
	}
	
	private void writeColumn(String columnName) {
		sb.append("  ").append(ctx.pad(columnName, 40)).append(" ");
	}
}
