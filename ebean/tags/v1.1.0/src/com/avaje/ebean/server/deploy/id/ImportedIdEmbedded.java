package com.avaje.ebean.server.deploy.id;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import com.avaje.ebean.MapBean;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssoc;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.persist.dml.GenerateDmlRequest;
import com.avaje.ebean.server.persist.dmlbind.BindableRequest;

/**
 * Imported Embedded id.
 */
public class ImportedIdEmbedded implements ImportedId {
	
	final BeanPropertyAssoc owner;

	final BeanPropertyAssocOne foreignAssocOne;

	final ImportedIdSimple[] imported;
	
	public ImportedIdEmbedded(BeanPropertyAssoc owner, BeanPropertyAssocOne foreignAssocOne, ImportedIdSimple[] imported) {
		this.owner = owner;
		this.foreignAssocOne = foreignAssocOne;
		this.imported = imported;
	}
	
	public boolean isScalar(){
		return false;
	}
	
	public String getLogicalName() {
		return owner.getName()+"."+foreignAssocOne.getName();
	}

	
	public String getDbColumn(){
		return null;
	}

	public void sqlAppend(DbSqlContext ctx) {
		ctx.setUseColumnAlias(true);
		for (int i = 0; i < imported.length; i++) {
			ctx.appendColumn(imported[i].localDbColumn);			
		}
		ctx.setUseColumnAlias(false);
	}
	
	public void dmlAppend(GenerateDmlRequest request) {
		
		for (int i = 0; i < imported.length; i++) {
			request.appendColumn(imported[i].localDbColumn);	
		}
	}

	public void dmlWhere(GenerateDmlRequest request, Object bean){

		Object embeddedId = null;
		if (bean != null) {
			embeddedId = foreignAssocOne.getValue(bean);
		}
		
		if (embeddedId == null){
			for (int i = 0; i < imported.length; i++) {
				request.appendColumnIsNull(imported[i].localDbColumn);	
			}
		} else {
		
			for (int i = 0; i < imported.length; i++) {
				Object value = imported[i].foreignProperty.getValue(embeddedId);
				if (value == null){
					request.appendColumnIsNull(imported[i].localDbColumn);	
				} else {
					request.appendColumn(imported[i].localDbColumn);	
				}
			}
		}
	}
	
	public void bind(BindableRequest request, Object bean, boolean bindNull) throws SQLException {

		Object embeddedId = foreignAssocOne.getValue(bean);
		
		if (embeddedId == null){
			for (int i = 0; i < imported.length; i++) {
				request.bind(null, imported[i].foreignProperty, imported[i].localDbColumn, true);
			}
			
		} else {
		
			for (int i = 0; i < imported.length; i++) {
				Object scalarValue = imported[i].foreignProperty.getValue(embeddedId);
				request.bind(scalarValue, imported[i].foreignProperty, imported[i].localDbColumn, true);
			}
		}
	}

	public void buildImport(MapBean mapBean, Object other){
		
		Object embeddedId = foreignAssocOne.getValue(other);
		if (embeddedId == null){
			String msg = "Foreign Key value null?";
			throw new PersistenceException(msg);
		}
		
		for (int i = 0; i < imported.length; i++) {
			Object scalarValue = imported[i].foreignProperty.getValue(embeddedId);
			mapBean.set(imported[i].localDbColumn, scalarValue);
		}
				
	}
	
	/**
	 * Not supported for embedded id.
	 */
	public BeanProperty findMatchImport(String matchDbColumn) {
		return null;
	}

}
