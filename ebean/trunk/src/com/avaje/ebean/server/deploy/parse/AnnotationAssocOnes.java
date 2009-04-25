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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import com.avaje.ebean.annotation.EmbeddedColumns;
import com.avaje.ebean.annotation.Where;
import com.avaje.ebean.server.deploy.BeanTable;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
import com.avaje.ebean.server.lib.sql.TableInfo;
import com.avaje.ebean.server.lib.util.StringHelper;

/**
 * Read the deployment annotations for Associated One beans.
 */
public class AnnotationAssocOnes extends AnnotationParser {
    
	/**
	 * Create with the deploy Info.
	 */
    public AnnotationAssocOnes(DeployBeanInfo<?> info) {
        super(info);
    }
    
    /**
     * Parse the annotation.
     */
    public void parse() {
    	
    	Iterator<DeployBeanProperty> it = descriptor.propertiesAll();
    	while (it.hasNext()) {
    		DeployBeanProperty prop = (DeployBeanProperty) it.next();
			if (prop instanceof DeployBeanPropertyAssocOne){
				readAssocOne((DeployBeanPropertyAssocOne<?>)prop); 
			}
		}
    }


    private void readAssocOne(DeployBeanPropertyAssocOne<?> prop) {
        
        ManyToOne manyToOne = (ManyToOne) get(prop, ManyToOne.class);
        if (manyToOne != null) {
            readManyToOne(manyToOne, prop);
        }
        OneToOne oneToOne = (OneToOne) get(prop, OneToOne.class);
        if (oneToOne != null) {
            readOneToOne(oneToOne, prop);
        }
        Embedded embedded = (Embedded) get(prop, Embedded.class);
        if (embedded != null) {
            readEmbedded(embedded, prop);
        }
        EmbeddedId emId = (EmbeddedId)get(prop, EmbeddedId.class);
        if (emId != null){
        	prop.setEmbedded(true);
        	prop.setId(true);
        	prop.setNullable(false);
        }
        Column column = (Column)get(prop, Column.class);
        if (column != null){
        	// have this in for AssocOnes used on
        	// Sql based beans...
        	prop.setDbColumn(column.name());
        }
        
        // May as well check for Id. Makes sense to me.
        Id id = (Id)get(prop, Id.class);
        if (id != null){
        	prop.setEmbedded(true);
        	prop.setId(true);
        	prop.setNullable(false);
        }
        
		Where where = (Where) get(prop, Where.class);
		if (where != null) {
			// not expecting this to be used on assoc one properties
			prop.setExtraWhere(where.clause());
		}
		
        // if it is none of these then it is a transient property
        // unless Xml deployment makes it persistent
        
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
	        	defineJoin.add(false, joinColumn);
	        	util.define(defineJoin);
	        } 
	
	        JoinColumns joinColumns = (JoinColumns) get(prop, JoinColumns.class);
	        if (joinColumns != null) {
	        	JoinDefineManualInfo defineJoin = new JoinDefineManualInfo(descriptor, prop);
	        	defineJoin.add(false, joinColumns);
	        	util.define(defineJoin);
	        }
	        
	        JoinTable joinTable = (JoinTable) get(prop, JoinTable.class);
	        if (joinTable != null) {
	        	JoinDefineManualInfo defineJoin = new JoinDefineManualInfo(descriptor, prop);
	        	defineJoin.add(false, joinTable, joinTable.joinColumns());
	        	util.define(defineJoin);
	        }
		}
    }
    
    private String errorMsgMissingBeanTable(Class<?> type, String from) {
    	return "Error with association to ["+type+"] from ["+from+"]. Is "+type+" registered?";
    }
    
    private void readManyToOne(ManyToOne propAnn, DeployBeanProperty prop) {
        
    	DeployBeanPropertyAssocOne<?> beanProp = (DeployBeanPropertyAssocOne<?>) prop;

        setCascadeTypes(propAnn.cascade(), beanProp.getCascadeInfo());

        BeanTable assoc = util.getBeanTable(beanProp.getPropertyType());
        if (assoc == null){
        	String msg = errorMsgMissingBeanTable(beanProp.getPropertyType(), prop.getFullBeanName());
        	throw new RuntimeException(msg);
        }
        
        beanProp.setBeanTable(assoc);
        info.setBeanJoinAlias(beanProp, propAnn.optional());
    }

    private void readOneToOne(OneToOne propAnn, DeployBeanPropertyAssocOne<?> prop) {
        
    	prop.setOneToOne(true);
        setCascadeTypes(propAnn.cascade(), prop.getCascadeInfo());

        BeanTable assoc = util.getBeanTable(prop.getPropertyType());
        if (assoc == null){
        	String msg = errorMsgMissingBeanTable(prop.getPropertyType(), prop.getFullBeanName());
        	throw new RuntimeException(msg);
        }

        prop.setBeanTable(assoc);
        info.setBeanJoinAlias(prop, propAnn.optional());
    }
    
    private void readEmbedded(Embedded propAnn, DeployBeanPropertyAssocOne<?> prop) {

    	prop.setEmbedded(true);

    	EmbeddedColumns columns = (EmbeddedColumns) get(prop, EmbeddedColumns.class);
    	if (columns != null){
    		    		
    		// convert into a Map
    		String propColumns = columns.columns();
    		Map<String,String> propMap = StringHelper.delimitedToMap(propColumns, ",", "=");
    		
    		prop.getDeployEmbedded().putAll(propMap);
    	}
    	
    	AttributeOverrides attrOverrides = (AttributeOverrides)get(prop, AttributeOverrides.class);
    	if (attrOverrides != null){
    		HashMap<String,String> propMap = new HashMap<String,String>();
    		AttributeOverride[] aoArray = attrOverrides.value();
    		for (int i = 0; i < aoArray.length; i++) {
    			String propName = aoArray[i].name();
    			String columnName = aoArray[i].column().name();
    			
    			propMap.put(propName, columnName);
			}
    		
    		prop.getDeployEmbedded().putAll(propMap);
    	}

    }
      
}
