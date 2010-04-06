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
package com.avaje.ebeaninternal.server.deploy.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptorMap;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyCompoundRoot;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyCompoundScalar;
import com.avaje.ebeaninternal.server.type.CtCompoundProperty;
import com.avaje.ebeaninternal.server.type.CtCompoundType;
import com.avaje.ebeaninternal.server.type.CtCompoundTypeScalarList;
import com.avaje.ebeaninternal.server.type.ScalarType;


/**
 * Property mapped to a joined bean.
 */
public class DeployBeanPropertyCompound extends DeployBeanProperty {

    CtCompoundType<?> compoundType;

	DeployBeanEmbedded deployEmbedded;
	
	/**
	 * Create the property.
	 */
	public DeployBeanPropertyCompound(DeployBeanDescriptor<?> desc, Class<?> targetType, CtCompoundType<?> compoundType) {
		super(desc, targetType, null);
		this.compoundType = compoundType;
	}

	public BeanPropertyCompoundRoot getFlatProperties(BeanDescriptorMap owner, BeanDescriptor<?> descriptor) {
	    
	    // get a 'flat' list of all the scalar types, their relative property names
	    // and also set their matching dbColumn 
	    
	    // represents the root property
	    BeanPropertyCompoundRoot rootProperty = new BeanPropertyCompoundRoot(this);
	    
	    // Walk the tree of a compound type collecting the 
	    // scalar types and non-scalar properties
	    CtCompoundTypeScalarList ctMeta = new CtCompoundTypeScalarList();
	    
	    compoundType.accumulateScalarTypes(null, ctMeta);
	    
	    List<BeanProperty> beanPropertyList = new ArrayList<BeanProperty>();
	    
	    
	    // for each of the scalar types inside a compound value object
	    // build a BeanPropertyCompoundScalar with appropriate deployment
	    // information. 
	    
	    for (Entry<String, ScalarType<?>> entry : ctMeta.entries()) {

	        String relativePropertyName = entry.getKey();
	        ScalarType<?> scalarType = entry.getValue();
	        
	        CtCompoundProperty ctProp = ctMeta.getCompoundType(relativePropertyName);
	        
	        
	        String dbColumn = relativePropertyName.replace(".", "_");
	        dbColumn = getDbColumn(relativePropertyName, dbColumn);
	        
	        DeployBeanProperty deploy = new DeployBeanProperty(null, scalarType.getType(), scalarType);
	        deploy.setScalarType(scalarType);
	        deploy.setDbColumn(dbColumn);
	        deploy.setName(relativePropertyName);
	        deploy.setDbInsertable(true);
	        deploy.setDbUpdateable(true);
	        deploy.setDbRead(true);
	        
	        BeanPropertyCompoundScalar bp = new BeanPropertyCompoundScalar(rootProperty, deploy, ctProp);
	        beanPropertyList.add(bp);
	        
	        rootProperty.register(bp);
	    }
	    
	    rootProperty.setNonScalarProperties(ctMeta.getNonScalarProperties());
	    return rootProperty;
	}
	
	private String getDbColumn(String propName, String defaultDbColumn){
	    if (deployEmbedded == null){
	        return defaultDbColumn;
	    }
	    String dbColumn = deployEmbedded.getPropertyColumnMap().get(propName);
	    return dbColumn == null ? defaultDbColumn : dbColumn;
	}
	
	/**
	 * Return the deploy information specifically for the deployment
	 * of Embedded beans.
	 */
	public DeployBeanEmbedded getDeployEmbedded() {
		// deployment should be single threaded 
		if (deployEmbedded == null){
			deployEmbedded = new DeployBeanEmbedded();
		}
		return deployEmbedded;
	}

    public CtCompoundType<?> getCompoundType() {
        return compoundType;
    }

    public void setCompoundType(CtCompoundType<?> compoundType) {
        this.compoundType = compoundType;
    }
}
