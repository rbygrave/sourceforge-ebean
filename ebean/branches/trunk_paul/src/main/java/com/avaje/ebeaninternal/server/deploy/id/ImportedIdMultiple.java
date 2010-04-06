package com.avaje.ebeaninternal.server.deploy.id;

import java.sql.SQLException;

import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssoc;
import com.avaje.ebeaninternal.server.deploy.DbSqlContext;
import com.avaje.ebeaninternal.server.deploy.IntersectionRow;
import com.avaje.ebeaninternal.server.persist.dml.GenerateDmlRequest;
import com.avaje.ebeaninternal.server.persist.dmlbind.BindableRequest;
import com.avaje.ebeaninternal.util.ValueUtil;

/**
 * Imported concatenated id that is not embedded.
 */
public class ImportedIdMultiple implements ImportedId {
	
	final BeanPropertyAssoc<?> owner;

	final ImportedIdSimple[] imported;
	
	public ImportedIdMultiple(BeanPropertyAssoc<?> owner, ImportedIdSimple[] imported) {
		this.owner = owner;
		this.imported = imported;
	}

	public void addFkeys(String name) {

		// not supporting addFkeys for ImportedIdMultiple
	}
	
	public String getLogicalName() {
		return null;
	}
	
	public boolean isScalar(){
		return false;
	}
	
	public String getDbColumn(){
		return null;
	}


	public void sqlAppend(DbSqlContext ctx) {
		for (int i = 0; i < imported.length; i++) {
			ctx.appendColumn(imported[i].localDbColumn);			
		}
	}
	
	public void dmlAppend(GenerateDmlRequest request) {
		
		for (int i = 0; i < imported.length; i++) {
			request.appendColumn(imported[i].localDbColumn);	
		}
	}

	public void dmlWhere(GenerateDmlRequest request, Object bean){
		if (bean == null){
			for (int i = 0; i < imported.length; i++) {
				request.appendColumnIsNull(imported[i].localDbColumn);	
			}			
		} else {
			for (int i = 0; i < imported.length; i++) {
				Object value = imported[i].foreignProperty.getValue(bean);
				if (value == null){
					request.appendColumnIsNull(imported[i].localDbColumn);
				} else {
					request.appendColumn(imported[i].localDbColumn);
				}
			}
		}
	}
	
	public boolean hasChanged(Object bean, Object oldValues) {
		
		for (int i = 0; i < imported.length; i++) {
			Object id = imported[i].foreignProperty.getValue(bean);
			Object oldId = imported[i].foreignProperty.getValue(oldValues);
			if (!ValueUtil.areEqual(id, oldId)) {
				return true;
			}
		}
		return false;
	}

	
	public void bind(BindableRequest request, Object bean, boolean bindNull) throws SQLException {
		
		for (int i = 0; i < imported.length; i++) {
			Object scalarValue = imported[i].foreignProperty.getValue(bean);
			request.bind(scalarValue, imported[i].foreignProperty, imported[i].localDbColumn, true);
		}
	}
	
	public void buildImport(IntersectionRow row, Object other){
				
		for (int i = 0; i < imported.length; i++) {
			Object scalarValue = imported[i].foreignProperty.getValue(other);
			row.put(imported[i].localDbColumn, scalarValue);
		}		
	}
	
	/**
	 * Not supported for concatenated id.
	 */
	public BeanProperty findMatchImport(String matchDbColumn) {
		
		BeanProperty p = null;
		for (int i = 0; i < imported.length; i++) {
			p = imported[i].findMatchImport(matchDbColumn);
			if (p != null){
				return p;
			}
		}
		
		return p;
	}
}
