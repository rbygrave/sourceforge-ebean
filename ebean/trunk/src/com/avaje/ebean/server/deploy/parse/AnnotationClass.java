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

import java.lang.reflect.Constructor;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PersistenceException;

import com.avaje.ebean.annotation.NamedUpdate;
import com.avaje.ebean.annotation.NamedUpdates;
import com.avaje.ebean.annotation.UpdateMode;
import com.avaje.ebean.config.naming.TableName;
import com.avaje.ebean.server.deploy.DeployNamedQuery;
import com.avaje.ebean.server.deploy.DeployNamedUpdate;

/**
 * Read the class level deployment annotations.
 */
public class AnnotationClass extends AnnotationParser {

	public AnnotationClass(DeployBeanInfo<?> info) {
		super(info);
	}

	/**
	 * Read the class level deployment annotations.
	 */
	public void parse() {
		read(descriptor.getBeanType());
		setTableName();
	}

	/**
	 * Set the table name if it has not already been set.
	 */
	private void setTableName() {
		
		if (!descriptor.isEmbedded() && !descriptor.isMeta()) {

			// default the TableName using NamingConvention.
			TableName tableName = getTableName(descriptor.getBeanType());
			
            descriptor.setBaseTable(tableName.getQualifiedName());
		}
	}
	
	private void read(Class<?> cls) {

		Entity entity = cls.getAnnotation(Entity.class);
		if (entity != null){
			checkDefaultConstructor();
			if (entity.name().equals("")) {
				descriptor.setName(cls.getSimpleName());

			} else {
				descriptor.setName(entity.name());
			}
		}

		Embeddable embeddable = cls.getAnnotation(Embeddable.class);
		if (embeddable != null){
			descriptor.setEmbedded(true);
			descriptor.setName("Embeddable:"+cls.getSimpleName());
		}

		UpdateMode updateMode = cls.getAnnotation(UpdateMode.class);
		if (updateMode != null){
			descriptor.setUpdateChangesOnly(updateMode.updateChangesOnly());
		}

		NamedQueries namedQueries = cls.getAnnotation(NamedQueries.class);
		if (namedQueries != null){
			readNamedQueries(namedQueries);
		}
		NamedQuery namedQuery = cls.getAnnotation(NamedQuery.class);
		if (namedQuery != null){
			readNamedQuery(namedQuery);
		}

		NamedUpdates namedUpdates = cls.getAnnotation(NamedUpdates.class);
		if (namedUpdates != null){
			readNamedUpdates(namedUpdates);
		}

		NamedUpdate namedUpdate = cls.getAnnotation(NamedUpdate.class);
		if (namedUpdate != null){
			readNamedUpdate(namedUpdate);
		}
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
				String m = "No default constructor on "+beanType;
				throw new PersistenceException(m);
			}
		} catch (SecurityException e) {
			String m = "Error checking for default constructor on "+beanType;
			throw new PersistenceException(m, e);

		} catch (NoSuchMethodException e) {
			String m = "No default constructor on "+beanType;
			throw new PersistenceException(m);
		}
	}

}
