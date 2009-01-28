package com.avaje.ebean.server.deploy.id;

import java.sql.SQLException;

import com.avaje.ebean.MapBean;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssoc;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.persist.dml.GenerateDmlRequest;
import com.avaje.ebean.server.persist.dmlbind.BindableRequest;

/**
 * Imported concatenated id that is not embedded.
 */
public class ImportedIdMultiple implements ImportedId {
	
	final BeanPropertyAssoc owner;


	final ImportedIdSimple[] imported;
	
	public ImportedIdMultiple(BeanPropertyAssoc owner, ImportedIdSimple[] imported) {
		this.owner = owner;
		this.imported = imported;
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
	
	public void bind(BindableRequest request, Object bean, boolean bindNull) throws SQLException {
		
		for (int i = 0; i < imported.length; i++) {
			Object scalarValue = imported[i].foreignProperty.getValue(bean);
			request.bind(scalarValue, imported[i].foreignProperty, imported[i].localDbColumn, true);
		}
	}
	
	public void buildImport(MapBean mapBean, Object other){
				
		for (int i = 0; i < imported.length; i++) {
			Object scalarValue = imported[i].foreignProperty.getValue(other);
			mapBean.set(imported[i].localDbColumn, scalarValue);
		}		
	}
	
	/**
	 * Not supported for concatenated id.
	 */
	public BeanProperty findMatchImport(String matchDbColumn) {
		return null;
	}
}
