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

import java.util.Iterator;

import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import com.avaje.ebean.annotation.Where;
import com.avaje.ebean.server.deploy.BeanDescriptorFactory;
import com.avaje.ebean.server.deploy.BeanTable;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;
import com.avaje.ebean.server.deploy.meta.DeployTableJoinColumn;

/**
 * Read the deployment annotation for Assoc Many beans.
 */
public class AnnotationAssocManys extends AnnotationParser {

	final BeanDescriptorFactory factory;
	
	/**
	 * Create with the DeployInfo.
	 */
	public AnnotationAssocManys(DeployBeanInfo<?> info, BeanDescriptorFactory factory) {
		super(info);
		this.factory = factory;
	}

	/**
	 * Parse the annotations.
	 */
	public void parse() {
		Iterator<DeployBeanProperty> it = descriptor.propertiesAll();
		while (it.hasNext()) {
			DeployBeanProperty prop = it.next();
			if (prop instanceof DeployBeanPropertyAssocMany) {
				read((DeployBeanPropertyAssocMany<?>) prop);
			}
		}
	}

	private void read(DeployBeanPropertyAssocMany<?> prop) {

		OneToMany oneToMany = get(prop, OneToMany.class);
		if (oneToMany != null) {
			readToOne(oneToMany, prop);
		}
		ManyToMany manyToMany = get(prop, ManyToMany.class);
		if (manyToMany != null) {
			readToMany(manyToMany, prop);
		}

		OrderBy orderBy = get(prop, OrderBy.class);
		if (orderBy != null) {
			prop.setFetchOrderBy(orderBy.value());
		}

		MapKey mapKey = get(prop, MapKey.class);
		if (mapKey != null) {
			prop.setMapKey(mapKey.name());
		}

		Where where = get(prop, Where.class);
		if (where != null) {
			prop.setExtraWhere(where.clause());
		}

		// check for manually defined joins
		BeanTable beanTable = prop.getBeanTable();
		JoinColumn joinColumn = get(prop, JoinColumn.class);
		if (joinColumn != null) {
			prop.getTableJoin().addJoinColumn(true, joinColumn, beanTable);
		}

		JoinColumns joinColumns = get(prop, JoinColumns.class);
		if (joinColumns != null) {
			prop.getTableJoin().addJoinColumn(true, joinColumns.value(), beanTable);
		}

		JoinTable joinTable = get(prop, JoinTable.class);
		if (joinTable != null) {
			if (prop.isManyToMany()){
				// expected this 
				readJoinTable(joinTable, prop);
				
			} else {
				// OneToMany in theory 
				prop.getTableJoin().addJoinColumn(true, joinTable.joinColumns(), beanTable);
			}
		}
			
		if (!prop.getTableJoin().hasJoinColumns() && beanTable != null){
			// checked mappedBy
			String propName =  (null != prop.getMappedBy() ? prop.getMappedBy() : prop.getName() );
			
			// use naming convention to define join
			String fkeyPrefix = factory.getNamingConvention().getColumnFromProperty(descriptor.getBeanType(), propName);
			DeployTableJoinColumn join = beanTable.createJoinColumn(fkeyPrefix);
			if (join != null){
				prop.getTableJoin().addJoinColumn(join);
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
	private void readJoinTable(JoinTable joinTable, DeployBeanPropertyAssocMany<?> prop) {
		
		String intTableName = joinTable.name();
		// set the intersection table
		DeployTableJoin intJoin = new DeployTableJoin();
		intJoin.setTable(intTableName);

		// add the source to intersection join columns
		intJoin.addJoinColumn(true, joinTable.joinColumns(), prop.getBeanTable());

		// set the intersection to dest table join columns
		DeployTableJoin destJoin = prop.getTableJoin();
		destJoin.addJoinColumn(false, joinTable.inverseJoinColumns(), prop.getBeanTable());

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
    
	private void readToMany(ManyToMany propAnn, DeployBeanPropertyAssocMany<?> manyProp) {

		manyProp.setMappedBy(propAnn.mappedBy());
		setCascadeTypes(propAnn.cascade(), manyProp.getCascadeInfo());

		Class<?> targetType = propAnn.targetEntity();
		if (targetType.equals(void.class)) {
			// via reflection of generics type
			targetType = manyProp.getTargetType();
		} else {
			manyProp.setTargetType(targetType);
		}

		// find the other many table (not intersection)
		BeanTable assoc = factory.getBeanTable(targetType);
		if (assoc == null) {
        	String msg = errorMsgMissingBeanTable(targetType, manyProp.getFullBeanName());
        	throw new RuntimeException(msg);
		}
		
		manyProp.setManyToMany(true);		
		manyProp.setBeanTable(assoc);
		info.setManyJoinAlias(manyProp, manyProp.getTableJoin());
	}

	private void readToOne(OneToMany propAnn, DeployBeanPropertyAssocMany<?> manyProp) {

		manyProp.setMappedBy(propAnn.mappedBy());
		setCascadeTypes(propAnn.cascade(), manyProp.getCascadeInfo());

		Class<?> targetType = propAnn.targetEntity();
		if (targetType.equals(void.class)) {
			// via reflection of generics type
			targetType = manyProp.getTargetType();			
		} else {
			manyProp.setTargetType(targetType);
		}

		BeanTable assoc = factory.getBeanTable(targetType);
		if (assoc == null) {
        	String msg = errorMsgMissingBeanTable(targetType, manyProp.getFullBeanName());
        	throw new RuntimeException(msg);
		}
		
		manyProp.setBeanTable(assoc);
		info.setManyJoinAlias(manyProp, manyProp.getTableJoin());
	}

}
