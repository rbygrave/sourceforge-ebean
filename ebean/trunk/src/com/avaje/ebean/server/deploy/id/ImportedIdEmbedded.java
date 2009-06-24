package com.avaje.ebean.server.deploy.id;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.deploy.BeanFkeyProperty;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssoc;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.deploy.IntersectionRow;
import com.avaje.ebean.server.persist.dml.GenerateDmlRequest;
import com.avaje.ebean.server.persist.dmlbind.BindableRequest;
import com.avaje.ebean.util.ValueUtil;

/**
 * Imported Embedded id.
 */
public class ImportedIdEmbedded implements ImportedId {
	
	final BeanPropertyAssoc<?> owner;

	final BeanPropertyAssocOne<?> foreignAssocOne;

	final ImportedIdSimple[] imported;
	
	public ImportedIdEmbedded(BeanPropertyAssoc<?> owner, BeanPropertyAssocOne<?> foreignAssocOne, ImportedIdSimple[] imported) {
		this.owner = owner;
		this.foreignAssocOne = foreignAssocOne;
		this.imported = imported;
	}
	
	public void addFkeys(String name) {
		
		BeanProperty[] embeddedProps = foreignAssocOne.getProperties();
		
		for (int i = 0; i < imported.length; i++) {
			String n = name+"."+foreignAssocOne.getName()+"."+embeddedProps[i].getName();
			BeanFkeyProperty fkey = new BeanFkeyProperty(null, n, imported[i].localDbColumn);
			owner.getBeanDescriptor().add(fkey);
		}
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
	
	public boolean hasChanged(Object bean, Object oldValues) {
		Object id = foreignAssocOne.getValue(bean);
		Object oldId = foreignAssocOne.getValue(oldValues);
		
		return !ValueUtil.areEqual(id, oldId);
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

	public void buildImport(IntersectionRow row, Object other){
		
		Object embeddedId = foreignAssocOne.getValue(other);
		if (embeddedId == null){
			String msg = "Foreign Key value null?";
			throw new PersistenceException(msg);
		}
		
		for (int i = 0; i < imported.length; i++) {
			Object scalarValue = imported[i].foreignProperty.getValue(embeddedId);
			row.put(imported[i].localDbColumn, scalarValue);
		}
				
	}
	
	/**
	 * Not supported for embedded id.
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
