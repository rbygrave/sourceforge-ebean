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

import javax.persistence.Column;
import javax.persistence.Enumerated;
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

import com.avaje.ebean.annotation.Formula;
import com.avaje.ebean.server.deploy.IdentityGeneration;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssoc;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;
import com.avaje.ebean.server.idgen.IdGeneratorManager;
import com.avaje.ebean.validation.Pattern;
import com.avaje.ebean.validation.Patterns;
import com.avaje.ebean.validation.ValidatorMeta;

/**
 * Read the field level deployment annotations.
 */
public class AnnotationFields extends AnnotationParser {

	public AnnotationFields(DeployBeanInfo info) {
		super(info);
	}

	/**
	 * Read the field level deployment annotations.
	 */
	public void parse() {

		Iterator<DeployBeanProperty> it = descriptor.propertiesAll();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();
			if (prop instanceof DeployBeanPropertyAssoc) {

			} else {
				readField(prop);
			}

			readValidations(prop);
		}
	}

	private void readField(DeployBeanProperty prop) {

		// all Enums will have a ScalarType assigned...
		boolean isEnum = prop.getPropertyType().isEnum();
		Enumerated enumerated = (Enumerated) get(prop, Enumerated.class);
		if (isEnum || enumerated != null) {
			util.setEnumScalarType(enumerated, prop);
		}

		Transient t = (Transient) get(prop, Transient.class);
		if (t != null) {
			// it is not a persistent property.
			prop.setDbRead(false);
			prop.setDbWrite(false);
			prop.setTransient(true);
			return;
		}

		// its persistent and assumed to be on the base table
		// rather than on a secondary table
		prop.setDbRead(true);
		prop.setDbWrite(true);
		prop.setDbTableAlias(descriptor.getBaseTableAlias());

		Column column = (Column) get(prop, Column.class);
		if (column != null) {
			readColumn(column, prop);

		} else {
			// set column name using Naming Convention
			setDbColumn(prop, null);
		}

		GeneratedValue gen = (GeneratedValue) get(prop, GeneratedValue.class);
		if (gen != null) {
			readGenValue(gen, prop);
		}
		
		Id id = (Id) get(prop, Id.class);
		if (id != null) {
			readId(id, prop);
		}

		// determine the JDBC type using Lob/Temporal
		// otherwise based on the property Class
		Lob lob = (Lob) get(prop, Lob.class);
		Temporal temporal = (Temporal) get(prop, Temporal.class);
		if (temporal != null) {
			readTemporal(temporal, prop);

		} else if (lob != null) {
			util.setLobType(prop);
		}

		Formula formula = (Formula) get(prop, Formula.class);
		if (formula != null) {
			prop.setSqlFormula(formula.select(), formula.join());
		}

		Version version = (Version) get(prop, Version.class);
		if (version != null) {
			// explicitly specify a version column
			prop.setVersionColumn(true);
		}
		
		// Could add an annotation for GeneratedProperty
		// but just going to use GeneratedPropertySettings
		// to do the job for now.

		// Using naming conventions to determine if this field
		// is a GeneratedProperty such as 'Insert Timestamp'
		util.setGeneratedProperty(prop);
	}

	private void readId(Id id, DeployBeanProperty prop) {
		
		prop.setId(true);
		prop.setNullable(false);
		
		if (prop.getPropertyType().equals(UUID.class)){
			// An Id of type UUID
			if (descriptor.getIdGeneratorName() == null){
				// Without a generator explicitly specified
				// so will use the default one AUTO_UUID
				descriptor.setIdGeneratorName(IdGeneratorManager.AUTO_UUID);
				descriptor.setIdentityGeneration(IdentityGeneration.ID_GENERATOR);
			}
		}
	}

	private void readGenValue(GeneratedValue gen, DeployBeanProperty prop) {

		String genName = gen.generator();
		
		SequenceGenerator sequenceGenerator = (SequenceGenerator) find(prop, SequenceGenerator.class);
		if (sequenceGenerator != null) {
			if (sequenceGenerator.name().equals(genName)) {
				genName = sequenceGenerator.sequenceName();
			}
		}
		
		GenerationType strategy = gen.strategy();

		if (strategy == GenerationType.IDENTITY) {
			descriptor.setIdentityGeneration(IdentityGeneration.DB_IDENTITY);

		} else if (strategy == GenerationType.SEQUENCE) {
			descriptor.setIdentityGeneration(IdentityGeneration.DB_SEQUENCE);
			if (genName != null && genName.length() > 0) {
				descriptor.setIdGeneratorName(genName);
			}

		} else if (strategy == GenerationType.AUTO) {
			if (prop.getPropertyType().equals(UUID.class)){
				descriptor.setIdGeneratorName(IdGeneratorManager.AUTO_UUID);
				descriptor.setIdentityGeneration(IdentityGeneration.ID_GENERATOR);
				
			} else {
				
				descriptor.setIdentityGeneration(IdentityGeneration.ID_GENERATOR);
				if (genName != null && genName.length() > 0) {
					descriptor.setIdGeneratorName(genName);
				}
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

		setDbColumn(prop, columnAnn.name());

		if (!columnAnn.nullable()) {
			prop.setNullable(false);
		}

		String baseTable = descriptor.getBaseTable();
		String tableName = columnAnn.table();
		if (tableName.equals("") || tableName.equalsIgnoreCase(baseTable)) {
			// its a base table property...
		} else {
			// its on a secondary table...
			DeployTableJoin tableJoin = info.getTableJoin(tableName, null);
			tableJoin.addProperty(prop);
		}
	}

	private void setDbColumn(DeployBeanProperty prop, String dbColumn) {
		dbColumn = util.getDbColumn(prop.getName(), dbColumn);

		prop.setDbColumn(dbColumn);
		prop.setDbTableAlias(descriptor.getBaseTableAlias());
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
