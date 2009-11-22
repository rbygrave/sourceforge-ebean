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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Types;
import java.util.Iterator;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PersistenceException;
import javax.persistence.SequenceGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.avaje.ebean.annotation.CreatedTimestamp;
import com.avaje.ebean.annotation.Formula;
import com.avaje.ebean.annotation.UpdatedTimestamp;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.dbplatform.IdType;
import com.avaje.ebean.server.deploy.generatedproperty.GeneratedPropertyFactory;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssoc;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;
import com.avaje.ebean.server.idgen.UuidIdGenerator;
import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotNull;
import com.avaje.ebean.validation.Pattern;
import com.avaje.ebean.validation.Patterns;
import com.avaje.ebean.validation.ValidatorMeta;

/**
 * Read the field level deployment annotations.
 */
public class AnnotationFields extends AnnotationParser {

	/**
	 * By default we lazy load Lob properties.
	 */
	private FetchType defaultLobFetchType = FetchType.LAZY;
	
	private GeneratedPropertyFactory generatedPropFactory = new GeneratedPropertyFactory();

	public AnnotationFields(DeployBeanInfo<?> info) {
		super(info);
		
		if (GlobalProperties.getBoolean("ebean.lobEagerFetch", false)) {
			defaultLobFetchType = FetchType.EAGER;
		}
	}

	/**
	 * Read the field level deployment annotations.
	 */
	public void parse() {

		Iterator<DeployBeanProperty> it = descriptor.propertiesAll();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();
			if (prop instanceof DeployBeanPropertyAssoc<?>) {
				readAssocOne(prop);
			} else {
				readField(prop);
			}

			readValidations(prop);
		}
	}

	/**
	 * Read the Id marker annotations on EmbeddedId properties.
	 */
	private void readAssocOne(DeployBeanProperty prop) {

		Id id = get(prop, Id.class);
		if (id != null) {
			prop.setId(true);
			prop.setNullable(false);
		}

		EmbeddedId embeddedId = get(prop, EmbeddedId.class);
		if (embeddedId != null) {
			prop.setId(true);
			prop.setNullable(false);
			prop.setEmbedded(true);
		}
		
	}
	
	private void readField(DeployBeanProperty prop) {

		// all Enums will have a ScalarType assigned...
		boolean isEnum = prop.getPropertyType().isEnum();
		Enumerated enumerated = get(prop, Enumerated.class);
		if (isEnum || enumerated != null) {
			util.setEnumScalarType(enumerated, prop);
		}

		// its persistent and assumed to be on the base table
		// rather than on a secondary table
		prop.setDbRead(true);
		prop.setDbInsertable(true);
		prop.setDbUpdateable(true);

		Column column = get(prop, Column.class);
		if (column != null) {
			readColumn(column, prop);
		} 
		if (prop.getDbColumn() == null){
			// No @Column annotation or @Column.name() not set
			// Use the NamingConvention to set the DB column name
			String dbColumn = namingConvention.getColumnFromProperty(beanType, prop.getName());
			prop.setDbColumn(dbColumn);
		}

		GeneratedValue gen = get(prop, GeneratedValue.class);
		if (gen != null) {
			readGenValue(gen, prop);
		}

		Id id = (Id) get(prop, Id.class);
		if (id != null) {
			readId(id, prop);
		}

		// determine the JDBC type using Lob/Temporal
		// otherwise based on the property Class
		Lob lob = get(prop, Lob.class);
		Temporal temporal = get(prop, Temporal.class);
		if (temporal != null) {
			readTemporal(temporal, prop);

		} else if (lob != null) {
			util.setLobType(prop);
		}

		Formula formula = get(prop, Formula.class);
		if (formula != null) {
			prop.setSqlFormula(formula.select(), formula.join());
		}

		Version version = get(prop, Version.class);
		if (version != null) {
			// explicitly specify a version column
			prop.setVersionColumn(true);
			generatedPropFactory.setVersion(prop);
		}

		Basic basic = get(prop, Basic.class);
		if (basic != null) {
			prop.setFetchType(basic.fetch());
			if (!basic.optional()) {
				prop.setNullable(false);
			}
		} else if (prop.isLob()){
			// use the default Lob fetchType
			prop.setFetchType(defaultLobFetchType);
		}
		
		CreatedTimestamp ct = get(prop, CreatedTimestamp.class);
		if (ct != null) {
			generatedPropFactory.setInsertTimestamp(prop);
		}

		UpdatedTimestamp ut = get(prop, UpdatedTimestamp.class);
		if (ut != null) {
			generatedPropFactory.setUpdateTimestamp(prop);
		}

		NotNull notNull = get(prop, NotNull.class);
		if (notNull != null) {
			// explicitly specify a version column
			prop.setNullable(false);
		}

		Length length = get(prop, Length.class);
		if (length != null) {
			if (length.max() < Integer.MAX_VALUE){
				// explicitly specify a version column
				prop.setDbLength(length.max());
			}
		}
		
		// Want to process last so we can use with @Formula 
		Transient t = get(prop, Transient.class);
		if (t != null) {
			// it is not a persistent property.
			prop.setDbRead(false);
			prop.setDbInsertable(false);
			prop.setDbUpdateable(false);
			prop.setTransient(true);
		}
	}

	private void readId(Id id, DeployBeanProperty prop) {

		prop.setId(true);
		prop.setNullable(false);

		if (prop.getPropertyType().equals(UUID.class)){
			// An Id of type UUID
			if (descriptor.getIdGeneratorName() == null){
				// Without a generator explicitly specified
				// so will use the default one AUTO_UUID
				descriptor.setIdGeneratorName(UuidIdGenerator.AUTO_UUID);
				descriptor.setIdType(IdType.GENERATOR);
			}
		}
	}

	private void readGenValue(GeneratedValue gen, DeployBeanProperty prop) {

		String genName = gen.generator();

		SequenceGenerator sequenceGenerator = find(prop, SequenceGenerator.class);
		if (sequenceGenerator != null) {
			if (sequenceGenerator.name().equals(genName)) {
				genName = sequenceGenerator.sequenceName();
			}
		}

		GenerationType strategy = gen.strategy();

		if (strategy == GenerationType.IDENTITY) {
			descriptor.setIdType(IdType.IDENTITY);

		} else if (strategy == GenerationType.SEQUENCE) {
			descriptor.setIdType(IdType.SEQUENCE);
			if (genName != null && genName.length() > 0) {
				descriptor.setIdGeneratorName(genName);
			}

		} else if (strategy == GenerationType.AUTO) {
			if (prop.getPropertyType().equals(UUID.class)){
				descriptor.setIdGeneratorName(UuidIdGenerator.AUTO_UUID);
				descriptor.setIdType(IdType.GENERATOR);

			} else {
				// use DatabasePlatform defaults
			}
		}
	}

	private void readTemporal(Temporal temporal, DeployBeanProperty prop) {

		TemporalType type = temporal.value();
		if (type.equals(TemporalType.DATE)) {
			prop.setDbType(Types.DATE);

		} else if (type.equals(TemporalType.TIMESTAMP)) {
			prop.setDbType(Types.TIMESTAMP);

		} else if (type.equals(TemporalType.TIME)) {
			prop.setDbType(Types.TIME);

		} else {
			throw new PersistenceException("Unhandled type " + type);
		}
	}

	private void readColumn(Column columnAnn, DeployBeanProperty prop) {

		if (!isEmpty(columnAnn.name())){
			String dbColumn = databasePlatform.convertQuotedIdentifiers(columnAnn.name());
			prop.setDbColumn(dbColumn);
		}

		prop.setDbInsertable(columnAnn.insertable());
		prop.setDbUpdateable(columnAnn.updatable());
		prop.setNullable(columnAnn.nullable());
		prop.setUnique(columnAnn.unique());
		if (columnAnn.precision() > 0){
			prop.setDbLength(columnAnn.precision());
		} else if (columnAnn.length() != 255){
			// set default 255 on DbTypeMap
			prop.setDbLength(columnAnn.length());
		}
		prop.setDbScale(columnAnn.scale());
		prop.setDbColumnDefn(columnAnn.columnDefinition());

		String baseTable = descriptor.getBaseTable();
		String tableName = columnAnn.table();
		if (tableName.equals("") || tableName.equalsIgnoreCase(baseTable)) {
			// its a base table property...
		} else {
			// its on a secondary table...
			DeployTableJoin tableJoin = info.getTableJoin(tableName);
			tableJoin.addProperty(prop);
		}
	}



	private void readValidations(DeployBeanProperty prop) {

		Field field = prop.getField();
		if (field != null) {
			Annotation[] fieldAnnotations = field.getAnnotations();
			for (int i = 0; i < fieldAnnotations.length; i++) {
				readValidations(prop, fieldAnnotations[i]);
			}
		}

		Method readMethod = prop.getReadMethod();
		if (readMethod != null) {
			Annotation[] methAnnotations = readMethod.getAnnotations();
			for (int i = 0; i < methAnnotations.length; i++) {
				readValidations(prop, methAnnotations[i]);
			}
		}
	}

	private void readValidations(DeployBeanProperty prop, Annotation ann) {
		Class<?> type = ann.annotationType();
		if (type.equals(Patterns.class)){
			// treating this as a special case for now...
			Patterns patterns = (Patterns)ann;
			Pattern[] patternsArray = patterns.patterns();
			for (int i = 0; i < patternsArray.length; i++) {
				util.createValidator(prop, patternsArray[i]);
			}

		} else {

			ValidatorMeta meta = type.getAnnotation(ValidatorMeta.class);
			if (meta != null) {
				util.createValidator(prop, ann);
			}
		}
	}
}
