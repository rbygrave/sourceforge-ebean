/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package org.avaje.ebean.server.deploy.meta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.avaje.ebean.server.deploy.generatedproperty.GeneratedProperty;
import org.avaje.ebean.server.lib.sql.ColumnInfo;
import org.avaje.ebean.server.reflect.BeanReflectGetter;
import org.avaje.ebean.server.reflect.BeanReflectSetter;
import org.avaje.ebean.server.type.ScalarType;
import org.avaje.ebean.server.validate.Validator;

/**
 * Description of a property of a bean. Includes its deployment information such
 * as database column mapping information.
 */
public class DeployBeanProperty {

	/**
	 * Advanced bean deployment. To exclude this property from update where
	 * clause.
	 */
	public static final String EXCLUDE_FROM_UPDATE_WHERE = "EXCLUDE_FROM_UPDATE_WHERE";

	/**
	 * Advanced bean deployment. To exclude this property from delete where
	 * clause.
	 */
	public static final String EXCLUDE_FROM_DELETE_WHERE = "EXCLUDE_FROM_DELETE_WHERE";

	/**
	 * Advanced bean deployment. To exclude this property from insert.
	 */
	public static final String EXCLUDE_FROM_INSERT = "EXCLUDE_FROM_INSERT";

	/**
	 * Advanced bean deployment. To exclude this property from update set
	 * clause.
	 */
	public static final String EXCLUDE_FROM_UPDATE = "EXCLUDE_FROM_UPDATE";

	/**
	 * Flag to mark this at part of the unique id.
	 */
	boolean id;

	/**
	 * Flag to mark the property as embedded. This could be on
	 * BeanPropertyAssocOne rather than here. Put it here for checking Id type
	 * (embedded or not).
	 */
	boolean embedded;

	/**
	 * Flag indicating if this the version property.
	 */
	boolean versionColumn;

	/**
	 * Set if this property is nullable.
	 */
	boolean nullable = true;

	boolean isTransient;

	/**
	 * Is this property include in database resultSet.
	 */
	boolean dbRead;

	/**
	 * Is this property mapped to the BASE table.
	 */
	boolean dbWrite;

	/**
	 * Set to true if this property is based on a secondary table.
	 */
	boolean secondaryTable;

	/**
	 * The type that owns this property.
	 */
	Class<?> owningType;

	/**
	 * True if the property is a Clob, Blob LongVarchar or LongVarbinary.
	 */
	boolean lob;

	/**
	 * The logical bean property name.
	 */
	String name;

	/**
	 * The reflected field.
	 */
	Field field;

	/**
	 * The bean type.
	 */
	Class<?> propertyType;

	/**
	 * Set for Non-JDBC types to provide logical to db type conversion.
	 */
	ScalarType scalarType;

	/**
	 * The database column. This can include quoted identifiers.
	 */
	String dbColumn;

	String sqlFormulaSelect;
	String sqlFormulaJoin;

	/**
	 * The jdbc data type this maps to.
	 */
	int dbType;

	/**
	 * The database table alias.
	 */
	String dbTableAlias;

	/**
	 * The default value to insert if null.
	 */
	Object defaultValue;

	/**
	 * Extra deployment parameters.
	 */
	HashMap<String, String> extraAttributeMap = new HashMap<String, String>();

	/**
	 * The method used to read the property.
	 */
	Method readMethod;

	/**
	 * The method used to write the property.
	 */
	Method writeMethod;

	BeanReflectGetter getter;

	BeanReflectSetter setter;

	/**
	 * Generator for insert or update timestamp etc.
	 */
	GeneratedProperty generatedProperty;

	List<Validator> validators = new ArrayList<Validator>();

	int dbColumnSize;
	int dbColumnDigits;
	int dbColumnPrecision;

	final DeployBeanDescriptor desc;

	public DeployBeanProperty(DeployBeanDescriptor desc) {
		this.desc = desc;
	}

	/**
	 * Return true is this is a simple scalar property.
	 */
	public boolean isScalar() {
		return true;
	}

	public String getFullBeanName() {
		return desc.getFullName() + "." + name;
	}

	public void readColumnInfo(ColumnInfo info) {
		if (nullable) {
			nullable = info.isNullable();
		} else {
			// do not remove if already set
		}
		this.dbColumnSize = info.getColumnSize();
		this.dbColumnDigits = info.getDecimalDigits();
		this.dbColumnPrecision = info.getNumberPrecisionRadix();
	}

	/**
	 * Return the Db Column maximum size as per DB dictionary.
	 */
	public int getDbColumnSize() {
		return dbColumnSize;
	}

	/**
	 * Return the Db Column max digits as per DB dictionary.
	 */
	public int getDbColumnDigits() {
		return dbColumnDigits;
	}

	/**
	 * Return the Db Column precision as per DB dictionary.
	 */
	public int getDbColumnPrecision() {
		return dbColumnPrecision;
	}

	/**
	 * Add a validator to this property.
	 */
	public void addValidator(Validator validator) {
		validators.add(validator);
	}

	/**
	 * Return true if the property contains a validator of a given type.
	 * <p>
	 * Used to detect if a validator has already been assigned when trying to
	 * automatically add validators such as Length and NotNull.
	 * </p>
	 */
	public boolean containsValidatorType(Class<?> type) {

		Iterator<Validator> it = validators.iterator();
		while (it.hasNext()) {
			Validator validator = (Validator) it.next();
			if (validator.getClass().equals(type)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the validators for this property.
	 */
	public Validator[] getValidators() {
		return validators.toArray(new Validator[validators.size()]);
	}

	/**
	 * Return the scalarType. This returns null for native JDBC types, otherwise
	 * it is used to convert between logical types and jdbc types.
	 */
	public ScalarType getScalarType() {
		return scalarType;
	}

	public void setScalarType(ScalarType scalarType) {
		this.scalarType = scalarType;
	}

	public BeanReflectGetter getGetter() {
		return getter;
	}

	public BeanReflectSetter getSetter() {
		return setter;
	}

	/**
	 * Return the getter method.
	 */
	public Method getReadMethod() {
		return readMethod;
	}

	/**
	 * Return the setter method.
	 */
	public Method getWriteMethod() {
		return writeMethod;
	}

	/**
	 * Set to the owning type form a Inheritance heirarchy.
	 */
	public void setOwningType(Class<?> owningType) {
		this.owningType = owningType;
	}

	public Class<?> getOwningType() {
		return owningType;
	}

	/**
	 * Return true if this is local to this type - aka not from a super type.
	 */
	public boolean isLocal() {
		return owningType == null || owningType.equals(desc.getBeanType());
	}

	/**
	 * Set the getter used to read the property value from a bean.
	 */
	public void setGetter(BeanReflectGetter getter) {
		this.getter = getter;
	}

	/**
	 * Set the setter used to set the property value to a bean.
	 */
	public void setSetter(BeanReflectSetter setter) {
		this.setter = setter;
	}

	/**
	 * Return the name of the property.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the property.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the bean Field associated with this property.
	 */
	public Field getField() {
		return field;
	}

	/**
	 * Set the bean Field associated with this property.
	 */
	public void setField(Field field) {
		this.field = field;
	}

	/**
	 * Return true if this is a generated property like update timestamp and
	 * create timestamp.
	 */
	public boolean isGenerated() {
		return generatedProperty != null;
	}

	/**
	 * Return the GeneratedValue. Used to generate update timestamp etc.
	 */
	public GeneratedProperty getGeneratedProperty() {
		return generatedProperty;
	}

	/**
	 * Set the GeneratedValue. Used to generate update timestamp etc.
	 */
	public void setGeneratedProperty(GeneratedProperty generatedValue) {
		this.generatedProperty = generatedValue;
	}

	/**
	 * Return true if this property is mandatory.
	 */
	public boolean isNullable() {
		return nullable;
	}

	/**
	 * Set the not nullable of this property.
	 */
	public void setNullable(boolean isNullable) {
		this.nullable = isNullable;
	}

	/**
	 * Return true if this is a version column used for concurrency checking.
	 */
	public boolean isVersionColumn() {
		return versionColumn;
	}

	/**
	 * Set if this is a version column used for concurrency checking.
	 */
	public void setVersionColumn(boolean isVersionColumn) {
		this.versionColumn = isVersionColumn;
	}

	/**
	 * Return the full deployment name of the property including a table alias
	 * if required.
	 * <p>
	 * This is derived information. Done for ease of use.
	 * </p>
	 */
	public String getDbFullName() {

		String dbFullName = getDbColumn();
		if (getDbTableAlias() != null) {
			dbFullName = getDbTableAlias() + "." + dbFullName;
		}

		return dbFullName;
	}

	/**
	 * Return the formula this property is based on.
	 */
	public String getSqlFormulaSelect() {
		return sqlFormulaSelect;
	}

	public String getSqlFormulaJoin() {
		return sqlFormulaJoin;
	}

	/**
	 * The property is based on a formula.
	 */
	public void setSqlFormula(String formulaSelect, String formulaJoin) {
		this.sqlFormulaSelect = formulaSelect;
		this.sqlFormulaJoin = formulaJoin.equals("") ? null : formulaJoin;
		this.dbRead = true;
		this.dbWrite = false;
	}

	/**
	 * The database column name this is mapped to.
	 */
	public String getDbColumn() {
		return dbColumn;
	}

	/**
	 * Set the database column name this is mapped to.
	 */
	public void setDbColumn(String dbColumn) {
		this.dbColumn = dbColumn;
	}

	/**
	 * Return the database jdbc data type this is mapped to.
	 */
	public int getDbType() {
		return dbType;
	}

	/**
	 * Set the database jdbc data type this is mapped to.
	 */
	public void setDbType(int dbType) {
		this.dbType = dbType;
		this.lob = isLobType(dbType);
	}

	/**
	 * Return true if this is mapped to a Clob Blob LongVarchar or
	 * LongVarbinary.
	 */
	public boolean isLob() {
		return lob;
	}

	private boolean isLobType(int type) {
		switch (type) {
		case Types.CLOB:
			return true;
		case Types.BLOB:
			return true;
		case Types.LONGVARBINARY:
			return true;
		case Types.LONGVARCHAR:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Return true if this property is based on a secondary table.
	 */
	public boolean isSecondaryTable() {
		return secondaryTable;
	}

	/**
	 * Return true if this property is included in persisting. This is always
	 * false for List and Bean types regardless.
	 */
	public boolean isDbWrite() {
		return dbWrite;
	}

	/**
	 * Set to true if this property is included in persisting.
	 */
	public void setDbWrite(boolean isDBWrite) {
		this.dbWrite = isDBWrite;
	}

	/**
	 * Set to true if this property is included in persisting.
	 */
	public void setSecondaryTable() {
		this.secondaryTable = true;
	}

	/**
	 * Return true if this property is included in database queries.
	 */
	public boolean isDbRead() {
		return dbRead;
	}

	/**
	 * Set to true if this property is included in database queries.
	 */
	public void setDbRead(boolean isDBRead) {
		this.dbRead = isDBRead;
	}

	/**
	 * Return true if the property is transient.
	 */
	public boolean isTransient() {
		return isTransient;
	}

	/**
	 * Mark the property explicitly as a transient property.
	 */
	public void setTransient(boolean isTransient) {
		this.isTransient = isTransient;
	}

	/**
	 * Set the bean read method.
	 * <p>
	 * NB: That a BeanReflectGetter is used to actually perform the getting of
	 * property values from a bean. This is due to performance considerations.
	 * </p>
	 */
	public void setReadMethod(Method readMethod) {
		this.readMethod = readMethod;
	}

	/**
	 * Set the bean write method.
	 * <p>
	 * NB: That a BeanReflectSetter is used to actually perform the setting of
	 * property values to a bean. This is due to performance considerations.
	 * </p>
	 */
	public void setWriteMethod(Method writeMethod) {
		this.writeMethod = writeMethod;
	}

	/**
	 * Return the property type.
	 */
	public Class<?> getPropertyType() {
		return propertyType;
	}

	/**
	 * Set the property type.
	 */
	public void setPropertyType(Class<?> propertyType) {
		this.propertyType = propertyType;
	}

	/**
	 * Return true if this is included in the unique id.
	 */
	public boolean isId() {
		return id;
	}

	/**
	 * Set to true if this is included in the unique id.
	 */
	public void setId(boolean id) {
		this.id = id;
	}

	/**
	 * Return true if this is an Embedded property. In this case it shares the
	 * table and pk of its owner object.
	 */
	public boolean isEmbedded() {
		return embedded;
	}

	/**
	 * Set to true if this is an embedded property.
	 */
	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public Map<String, String> getExtraAttributeMap() {
		return extraAttributeMap;
	}

	/**
	 * Return an extra attribute set on this property.
	 */
	public String getExtraAttribute(String key) {
		return (String) extraAttributeMap.get(key);
	}

	/**
	 * Set an extra attribute set on this property.
	 */
	public void setExtraAttribute(String key, String value) {
		extraAttributeMap.put(key, value);
	}

	/**
	 * Return the default value.
	 */
	public Object getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Set the default value. Inserted if the value is null.
	 */
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Return the database table alias.
	 */
	public String getDbTableAlias() {
		return dbTableAlias;
	}

	/**
	 * Set the database table alias.
	 */
	public void setDbTableAlias(String dbTableAlias) {
		this.dbTableAlias = dbTableAlias;
	}

	public String toString() {
		return desc.getFullName() + "." + name;
	}

}
