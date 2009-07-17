package com.avaje.ebean.server.deploy.id;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.core.InternString;
import com.avaje.ebean.server.deploy.BeanFkeyProperty;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssoc;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.deploy.IntersectionRow;
import com.avaje.ebean.server.persist.dml.GenerateDmlRequest;
import com.avaje.ebean.server.persist.dmlbind.BindableRequest;
import com.avaje.ebean.util.ValueUtil;

/**
 * Single scalar imported id.
 */
public class ImportedIdSimple implements ImportedId {

	protected final BeanPropertyAssoc<?> owner;

	protected final String localDbColumn;

	protected final String logicalName;

	protected final BeanProperty foreignProperty;

	public ImportedIdSimple(BeanPropertyAssoc<?> owner, String localDbColumn, BeanProperty foreignProperty) {
		this.owner = owner;
		this.localDbColumn = InternString.intern(localDbColumn);
		this.foreignProperty = foreignProperty;
		this.logicalName = InternString.intern(owner.getName()+"."+foreignProperty.getName());
	}
	
	public void addFkeys(String name) {
		BeanFkeyProperty fkey = new BeanFkeyProperty(null, name+"."+foreignProperty.getName(), localDbColumn);
		owner.getBeanDescriptor().add(fkey);
	}

	public boolean isScalar(){
		return true;
	}

	public String getLogicalName() {
		return logicalName;
	}

	public String getDbColumn(){
		return localDbColumn;
	}

	public void buildImport(IntersectionRow row, Object other){

		Object value = foreignProperty.getValue(other);
		if (value == null){
			String msg = "Foreign Key value null?";
			throw new PersistenceException(msg);
		}

		row.put(localDbColumn, value);
	}

	public void sqlAppend(DbSqlContext ctx) {
		ctx.appendColumn(localDbColumn);
	}


	public void dmlAppend(GenerateDmlRequest request) {
		request.appendColumn(localDbColumn);
	}

	public void dmlWhere(GenerateDmlRequest request, Object bean){

		Object value = null;
		if (bean != null){
			value = foreignProperty.getValue(bean);
		}
		if (value == null){
			request.appendColumnIsNull(localDbColumn);
		} else {
			request.appendColumn(localDbColumn);
		}
	}

	public boolean hasChanged(Object bean, Object oldValues) {
		Object id = foreignProperty.getValue(bean);

		if (oldValues != null){
			Object oldId = foreignProperty.getValue(oldValues);
			return !ValueUtil.areEqual(id, oldId);
		}

		return true;
	}

	public void bind(BindableRequest request, Object bean, boolean bindNull) throws SQLException {

		Object value = null;
		if (bean != null){
			value = foreignProperty.getValue(bean);
		}
		request.bind(value, foreignProperty, localDbColumn, bindNull);
	}

	public BeanProperty findMatchImport(String matchDbColumn) {

		if (matchDbColumn.equals(localDbColumn)) {
			return foreignProperty;
		}
		return null;
	}
}
