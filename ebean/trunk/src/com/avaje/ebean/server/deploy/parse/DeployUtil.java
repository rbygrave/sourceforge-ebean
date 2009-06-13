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
package com.avaje.ebean.server.deploy.parse;

import java.lang.annotation.Annotation;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceException;

import com.avaje.ebean.annotation.SqlSelect;
import com.avaje.ebean.config.NamingConvention;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.server.deploy.DeploySqlSelect;
import com.avaje.ebean.server.deploy.DeploySqlSelectParser;
import com.avaje.ebean.server.deploy.DeploySqlSelectParser.Meta;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.type.ScalarType;
import com.avaje.ebean.server.type.ScalarTypeEnumStandard;
import com.avaje.ebean.server.type.TypeManager;
import com.avaje.ebean.server.validate.Validator;
import com.avaje.ebean.server.validate.ValidatorFactoryManager;

/**
 * Utility object to help processing deployment information.
 */
public class DeployUtil {

	private static final Logger logger = Logger.getLogger(DeployUtil.class.getName());

	/**
	 * Use a BackTick ` at the beginning and end of table or column names that
	 * you want to use quoted identifiers for. The backticks get converted to
	 * the appropriate characters from the ServerPlugin.
	 */
	private static final char BACK_TICK = '`';

	/**
	 * Assumes CLOB rather than LONGVARCHAR.
	 */
	private static final int dbCLOBType = Types.CLOB;

	/**
	 * Assumes BLOB rather than LONGVARBINARY. This should probably be
	 * configurable.
	 */
	private static final int dbBLOBType = Types.BLOB;

	private final NamingConvention namingConvention;

	private final TypeManager typeManager;
	
	private final DeploySqlSelectParser sqlSelectParser;
	
	private final ValidatorFactoryManager validatorFactoryManager;
		
	private final String manyToManyAlias;
	
	private final DatabasePlatform dbSpecific;
	
	public DeployUtil(DatabasePlatform dbSpecific, TypeManager typeMgr, NamingConvention nc) {

		this.dbSpecific = dbSpecific;
		this.typeManager = typeMgr;
		this.namingConvention = nc;
		this.sqlSelectParser = new DeploySqlSelectParser(namingConvention);

		// this alias is used for ManyToMany lazy loading queries
		this.manyToManyAlias = "zzzzzz";
		
		this.validatorFactoryManager = new ValidatorFactoryManager();
	}
	
	/**
	 * Return the table alias used for ManyToMany joins.
	 */
	public String getManyToManyAlias() {
		return manyToManyAlias;
	}
	
	public void createValidator(DeployBeanProperty prop, Annotation ann) {
		try {
			Validator validator = validatorFactoryManager.create(ann, prop.getPropertyType());
			if (validator != null){
				prop.addValidator(validator);
			}
		} catch (Exception e){
			String msg = "Error creating a validator on "+prop.getFullBeanName();
			logger.log(Level.SEVERE, msg, e);
		}
	}

	/**
	 * Parse and return the DeploySqlSelect.
	 */
	public DeploySqlSelect parseSqlSelect(DeployBeanDescriptor<?> deployDesc, SqlSelect sqlSelect) {
		
		
		Meta meta = DeploySqlSelectParser.createMeta(deployDesc, sqlSelect);
		
		return sqlSelectParser.parse(deployDesc, meta);
	}
	
	public ScalarType setEnumScalarType(Enumerated enumerated, DeployBeanProperty prop) {

		Class<?> enumType = prop.getPropertyType();
		if (!enumType.isEnum()) {
			throw new IllegalArgumentException("Not a Enum?");
		}
		ScalarType scalarType = typeManager.getScalarType(enumType);
		if (scalarType == null) {
			// see if it has a Mapping in avaje.properties
			scalarType = typeManager.createEnumScalarType(enumType);
			if (scalarType == null){
				// use JPA normal Enum type (without mapping)
				EnumType type = enumerated != null? enumerated.value(): null;
				scalarType = createEnumScalarTypePerSpec(enumType, type, prop.getDbType());
			}
			
			typeManager.add(scalarType);
		}
		prop.setScalarType(scalarType);
		prop.setDbType(scalarType.getJdbcType());
		return scalarType;
	}

	private ScalarType createEnumScalarTypePerSpec(Class<?> enumType, EnumType type, int dbType) {

		if (type == null) {
			// default as per spec is ORDINAL
			return new ScalarTypeEnumStandard.OrdinalEnum(enumType);
		
		} else if (type == EnumType.ORDINAL) {	
			return new ScalarTypeEnumStandard.OrdinalEnum(enumType);
		
		} else {	
			return new ScalarTypeEnumStandard.StringEnum(enumType);
		}
	}
	

	
	/**
	 * Returns the table name for a given Class using the naming convention.
	 */
	public String getTableNameFromClass(Class<?> beanType) {
		return namingConvention.getTableNameFromClass(beanType);
	}

	/**
	 * Return the DB column name for a given property name.
	 */
	public String getDbColumn(Class<?> beanClass, String propName, String dbColumn) {
		if (isNullString(dbColumn)) {
			dbColumn = namingConvention.getColumnFromProperty(beanClass, propName);
		}
		dbColumn = convertQuotedIdentifiers(dbColumn);
		return dbColumn;
	}

	/**
	 * Find the ScalarType for this property.
	 * <p>
	 * This determines if there is a conversion required from the logical (bean)
	 * type to a DB (jdbc) type. This is the case for java.util.Date etc.
	 * </p>
	 */
	public void setScalarType(DeployBeanProperty property) {


		if (property.getScalarType() != null){
			// already has a ScalarType assigned.
			// this will be an Enum type...
			return;
		}
		
		ScalarType scalarType = getScalarType(property);
		if (scalarType != null){
			// set the jdbc type this maps to
			property.setDbType(scalarType.getJdbcType());
			property.setScalarType(scalarType);
		}
	}

	private ScalarType getScalarType(DeployBeanProperty property) {
		
		// Note that Temporal types already have dbType 
		// set via annotations
		Class<?> propType = property.getPropertyType();
		ScalarType scalarType = typeManager.getScalarType(propType, property.getDbType());
		if (scalarType != null) {
			return scalarType;
		}
		
		String msg = property.getFullBeanName()+" has no ScalarType - type[" + propType.getName() + "]";
		if (!property.isTransient()){
			throw new PersistenceException(msg);
		
		} else {
			// this is ok...
			logger.finest("... transient property "+msg);
			return null;
		} 
	}

	/**
	 * This property is marked as a Lob object.
	 */
	public void setLobType(DeployBeanProperty prop) {

		// is String or byte[] ? used to determine if its
		// a CLOB or BLOB
		Class<?> type = prop.getPropertyType();

		// this also sets the lob flag on DeployBeanProperty
		if (isClobType(type)) {
			prop.setDbType(dbCLOBType);
		} else {
			prop.setDbType(dbBLOBType);
		}
	}

	public boolean isClobType(Class<?> type){
		if (type.equals(String.class)){
			return true;
		}
		return false;
	}

	/**
	 * Convert backticks to the appropriate open quote and close quote for this
	 * plugin.
	 */
	public String convertQuotedIdentifiers(String dbName) {

		if (dbName.charAt(0) == BACK_TICK) {
			if (dbName.charAt(dbName.length() - 1) == BACK_TICK) {

				String quotedName = dbSpecific.getOpenQuote();
				quotedName += dbName.substring(1, dbName.length() - 1);
				quotedName += dbSpecific.getCloseQuote();

				return quotedName;

			} else {
				logger.log(Level.SEVERE, "Missing backquote on [" + dbName + "]");
			}
		}

		return dbName;
	}

	private boolean isNullString(String s) {
		if (s == null || s.trim().length() == 0) {
			return true;
		}
		return false;
	}

	/**
	 * Get a table alias without any checking with the availability in the
	 * aliasList.
	 */
	public String getPotentialAlias(String tableOrProperty) {

		int usPos = tableOrProperty.lastIndexOf("_");
		if (usPos > -1 && usPos < tableOrProperty.length() - 1) {
			tableOrProperty = tableOrProperty.substring(usPos + 1);
		}
		
		// search for the first valid letter
		for (int i = 0; i < tableOrProperty.length(); i++) {
			char ch = Character.toLowerCase(tableOrProperty.charAt(i));
			if (Character.isLetter(ch)){
				return String.valueOf(ch);
			}
		}
		
		// not expected really
		return "z";
	}
}
