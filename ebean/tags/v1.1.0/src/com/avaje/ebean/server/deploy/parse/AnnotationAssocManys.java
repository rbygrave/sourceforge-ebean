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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;

import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PersistenceException;

import com.avaje.ebean.annotation.Where;
import com.avaje.ebean.server.deploy.BeanTable;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;
import com.avaje.ebean.server.lib.sql.TableInfo;

/**
 * Read the deployment annotation for Assoc Many beans.
 */
public class AnnotationAssocManys extends AnnotationParser {

	/**
	 * Create with the DeployInfo.
	 */
	public AnnotationAssocManys(DeployBeanInfo info) {
		super(info);
	}

	/**
	 * Parse the annotations.
	 */
	public void parse() {
		Iterator<DeployBeanProperty> it = descriptor.propertiesAll();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();
			if (prop instanceof DeployBeanPropertyAssocMany) {
				read((DeployBeanPropertyAssocMany) prop);
			}
		}
	}

	private void read(DeployBeanPropertyAssocMany prop) {

		OneToMany oneToMany = (OneToMany) get(prop, OneToMany.class);
		if (oneToMany != null) {
			readToOne(oneToMany, prop);
		}
		ManyToMany manyToMany = (ManyToMany) get(prop, ManyToMany.class);
		if (manyToMany != null) {
			readToMany(manyToMany, prop);
		}

		OrderBy orderBy = (OrderBy) get(prop, OrderBy.class);
		if (orderBy != null) {
			prop.setFetchOrderBy(orderBy.value());
		}

		MapKey mapKey = (MapKey) get(prop, MapKey.class);
		if (mapKey != null) {
			prop.setMapKey(mapKey.name());
		}

		Where where = (Where) get(prop, Where.class);
		if (where != null) {
			prop.setExtraWhere(where.clause());
		}

		TableInfo baseTableInfo = info.getBaseTableInfo();
		if (baseTableInfo == null){
			// do not try to define joins manually as they will 
			// likely fail for this database schema as the base
			// table has not been found.

		} else {
			// check for manually defined joins
			JoinColumn joinColumn = (JoinColumn) get(prop, JoinColumn.class);
			if (joinColumn != null) {
				JoinDefineManualInfo defineJoin = new JoinDefineManualInfo(descriptor, prop);
				defineJoin.add(true, joinColumn);
				util.define(defineJoin);
			}
	
			JoinColumns joinColumns = (JoinColumns) get(prop, JoinColumns.class);
			if (joinColumns != null) {
				JoinDefineManualInfo defineJoin = new JoinDefineManualInfo(descriptor, prop);
				defineJoin.add(true, joinColumns);
				util.define(defineJoin);
			}
	
			JoinTable joinTable = (JoinTable) get(prop, JoinTable.class);
			if (joinTable != null) {
				if (prop.isManyToMany()){
					// expected this 
					readJoinTable(joinTable, prop);
					
				} else {
					// OneToMany in theory 
					JoinDefineManualInfo defineJoin = new JoinDefineManualInfo(descriptor, prop);
					defineJoin.add(true, joinTable, joinTable.joinColumns());
					util.define(defineJoin);					
				}
			}
		}
	}

	/**
	 * Define the joins for a ManyToMany relationship.
	 * <p>
	 * This includes joins to the intersection table and from the intersection table
	 * to the other side of the ManyToMany.
	 * </p>
	 */
	private void readJoinTable(JoinTable joinTable, DeployBeanPropertyAssocMany prop) {
		
		String intTableName = joinTable.name();
		// set the intersection table
		DeployTableJoin intJoin = new DeployTableJoin();
		intJoin.setTable(intTableName);

		// add the source to intersection join columns
		intJoin.addJoinColumn(true, joinTable.joinColumns());
		intJoin.setUndefinedColumnIfRequired(info.getUtil(), info.getBaseTableInfo().getName());

		// set the intersection to dest table join columns
		DeployTableJoin destJoin = prop.getTableJoin();
		destJoin.addJoinColumn(false, joinTable.inverseJoinColumns());
		destJoin.setUndefinedColumnIfRequired(info.getUtil(), destJoin.getTable());

		// set table alias etc for the join to intersection
		info.setManyIntersectionAlias(prop, intJoin);

		// set the intersection alias to the destJoin
		String intAlias = intJoin.getForeignTableAlias();
		destJoin.setLocalTableAlias(intAlias);

		// reverse join from dest back to intersection
		DeployTableJoin inverseDest = destJoin.createInverse();
		inverseDest.setTable(intTableName);
		// try to make sure we don't get a tableAlias clash
		inverseDest.setLocalTableAlias(prop.getBeanTable().getBaseTableAlias());

		// zzzzzz is typically the ManyToManyAlias
		inverseDest.setForeignTableAlias(info.getUtil().getManyToManyAlias());

		prop.setIntersectionTableJoin(intJoin);
		prop.setInverseJoin(inverseDest);
	}
	
    
    private String errorMsgMissingBeanTable(Class<?> type, String from) {
    	return "Error with association to ["+type+"] from ["+from+"]. Is "+type+" registered?";
    }
    
	private void readToMany(ManyToMany propAnn, DeployBeanPropertyAssocMany manyProp) {

		manyProp.setMappedBy(propAnn.mappedBy());
		setCascadeTypes(propAnn.cascade(), manyProp.getCascadeInfo());

		Class<?> targetType = propAnn.targetEntity();
		if (targetType.equals(void.class)) {
			targetType = determineTargetType(manyProp);
		}

		// find the other many table (not intersection)
		BeanTable assoc = util.getBeanTable(targetType);
		if (assoc == null) {
        	String msg = errorMsgMissingBeanTable(targetType, manyProp.getFullBeanName());
        	throw new RuntimeException(msg);
		}
		
		manyProp.setTargetType(targetType);
		manyProp.setManyToMany(true);		
		manyProp.setBeanTable(assoc);
		info.setManyJoinAlias(manyProp, manyProp.getTableJoin());
	}

	private void readToOne(OneToMany propAnn, DeployBeanPropertyAssocMany manyProp) {

		manyProp.setMappedBy(propAnn.mappedBy());
		setCascadeTypes(propAnn.cascade(), manyProp.getCascadeInfo());

		Class<?> targetType = propAnn.targetEntity();
		if (targetType.equals(void.class)) {
			targetType = determineTargetType(manyProp);
		}

		BeanTable assoc = util.getBeanTable(targetType);
		if (assoc == null) {
        	String msg = errorMsgMissingBeanTable(targetType, manyProp.getFullBeanName());
        	throw new RuntimeException(msg);
		}
		
		manyProp.setTargetType(targetType);		
		manyProp.setBeanTable(assoc);
		info.setManyJoinAlias(manyProp, manyProp.getTableJoin());
	}

	/**
	 * Determine the type of the List,Set or Map. Not been set explicitly so
	 * determine this from ParameterizedType.
	 */
	private Class<?> determineTargetType(DeployBeanProperty prop) {
		Field field = prop.getField();
		if (field == null) {
			String msg = "property " + prop.getFullBeanName() + " has no field to find targetType?";
			throw new PersistenceException(msg);
		}
		Type genType = field.getGenericType();
		if (genType instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) genType;

			Type[] typeArgs = ptype.getActualTypeArguments();
			if (typeArgs.length == 1) {
				// probably a Set or List
				return (Class<?>) typeArgs[0];
			}
			if (typeArgs.length == 2) {
				// this is probably a Map
				return (Class<?>) typeArgs[1];
			}
		}
		String msg = "property " + prop.getFullBeanName() + " has no generics targetType defined?";
		throw new PersistenceException(msg);
	}
}
