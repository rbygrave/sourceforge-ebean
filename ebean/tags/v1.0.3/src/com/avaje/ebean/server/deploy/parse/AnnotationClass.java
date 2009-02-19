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
import java.lang.reflect.Constructor;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.avaje.ebean.annotation.NamedUpdate;
import com.avaje.ebean.annotation.NamedUpdates;
import com.avaje.ebean.annotation.Sql;
import com.avaje.ebean.annotation.SqlSelect;
import com.avaje.ebean.server.deploy.DeployNamedQuery;
import com.avaje.ebean.server.deploy.DeployNamedUpdate;
import com.avaje.ebean.server.deploy.DeploySqlSelect;

/**
 * Read the class level deployment annotations.
 */
public class AnnotationClass extends AnnotationParser {

	public AnnotationClass(DeployBeanInfo info) {
		super(info);
	}

	/**
	 * Read the class level deployment annotations.
	 */
	public void parse() {

		read(descriptor.getBeanType());
		
		if (descriptor.getBaseTable() == null){
			// search the inheritance heirarchy ...
			Table table = findInheritedTable(descriptor.getBeanType().getSuperclass());
			if (table != null){
				info.setTable(table.catalog(), table.schema(), table.name(), null);
			}
		}
	}
	
	/**
	 * Search the inheritance heirarchy for Table annotation.
	 */
	private Table findInheritedTable(Class<?> cls) {
		if (cls.equals(Object.class)){
			return null;
		} 
		Table table = cls.getAnnotation(Table.class);
		if (table != null){
			return table;
		}
		return findInheritedTable(cls.getSuperclass());
	}

	public void readSqlAnnotations() {
		Class<?> cls = descriptor.getBeanType();
		Annotation[] anns = cls.getAnnotations();
		for (int i = 0; i < anns.length; i++) {

			if (anns[i] instanceof Sql) {
				Sql ann = (Sql) anns[i];
				setSql(ann);
			}
			if (anns[i] instanceof SqlSelect) {
				SqlSelect ann = (SqlSelect) anns[i];
				setSqlSelect(ann);
			}
		}
	}

	private void read(Class<?> cls) {

		Annotation[] anns = cls.getAnnotations();
		for (int i = 0; i < anns.length; i++) {
			if (anns[i] instanceof Entity) {
				Entity entity = (Entity) anns[i];

				checkDefaultConstructor();

				if (entity.name().equals("")) {
					descriptor.setName(getShortName(cls));

				} else {
					descriptor.setName(entity.name());
				}
			}
			if (anns[i] instanceof Embeddable) {
				descriptor.setEmbedded(true);
				descriptor.setName("Embeddable:"+getShortName(cls));
			}
			if (anns[i] instanceof Table) {
				Table ann = (Table) anns[i];
				info.setTable(ann.catalog(), ann.schema(), ann.name(), null);
			}
			if (anns[i] instanceof NamedQueries) {
				readNamedQueries((NamedQueries) anns[i]);
			}
			if (anns[i] instanceof NamedQuery) {
				readNamedQuery((NamedQuery) anns[i]);
			}
			if (anns[i] instanceof NamedUpdates) {
				readNamedUpdates((NamedUpdates) anns[i]);
			}
			if (anns[i] instanceof NamedUpdate) {
				readNamedUpdate((NamedUpdate) anns[i]);
			}
		}
	}

	private String getShortName(Class<?> cls) {
		String defaultShortName = cls.getName();
		int dp = defaultShortName.lastIndexOf('.');
		if (dp > -1) {
			defaultShortName = defaultShortName.substring(dp + 1);
		}
		return defaultShortName;
	}
	
	private void setSql(Sql sql) {
		SqlSelect[] select = sql.select();
		for (int i = 0; i < select.length; i++) {
			setSqlSelect(select[i]);
		}
	}

	private void setSqlSelect(SqlSelect sqlSelect) {

		DeploySqlSelect parsedSql = util.parseSqlSelect(descriptor, sqlSelect);

		DeployNamedQuery namedQuery = new DeployNamedQuery(sqlSelect.name(), sqlSelect.query(),
				null, parsedSql);
		descriptor.add(namedQuery);
	}

	private void readNamedQueries(NamedQueries namedQueries) {
		NamedQuery[] queries = namedQueries.value();
		for (int i = 0; i < queries.length; i++) {
			readNamedQuery(queries[i]);
		}
	}

	private void readNamedQuery(NamedQuery namedQuery) {
		DeployNamedQuery q = new DeployNamedQuery(namedQuery);
		descriptor.add(q);
	}

	private void readNamedUpdates(NamedUpdates updates) {
		NamedUpdate[] updateArray = updates.value();
		for (int i = 0; i < updateArray.length; i++) {
			readNamedUpdate(updateArray[i]);
		}
	}

	private void readNamedUpdate(NamedUpdate update) {
		DeployNamedUpdate upd = new DeployNamedUpdate(update);
		descriptor.add(upd);
	}

	/**
	 * Check to see if the Entity bean has a default constructor.
	 * <p>
	 * If it does not then it is expected that this entity bean has an
	 * associated BeanFinder.
	 * </p>
	 */
	private void checkDefaultConstructor() {

		Class<?> beanType = descriptor.getBeanType();

		Constructor<?> defaultConstructor;
		try {
			defaultConstructor = beanType.getConstructor((Class[]) null);
			if (defaultConstructor == null) {
				descriptor.setDefaultConstructor(false);
			}
		} catch (SecurityException e) {
			// hmmm, not sure about this one...
			// throw new PersistenceException(e);
			descriptor.setDefaultConstructor(false);

		} catch (NoSuchMethodException e) {
			descriptor.setDefaultConstructor(false);
		}
	}

}
