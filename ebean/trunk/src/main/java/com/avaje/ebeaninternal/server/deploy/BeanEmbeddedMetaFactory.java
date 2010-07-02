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
package com.avaje.ebeaninternal.server.deploy;

import java.util.Map;

import javax.persistence.PersistenceException;

import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanPropertyAssocOne;

/**
 * Creates BeanProperties for Embedded beans that have deployment information
 * such as the actual DB column name and table alias.
 */
public class BeanEmbeddedMetaFactory {

	/**
	 * Create BeanProperties for embedded beans using the deployment specific DB column name and table alias. 
	 */
	public static BeanEmbeddedMeta create(BeanDescriptorMap owner, DeployBeanPropertyAssocOne<?> prop,
			BeanDescriptor<?> descriptor) {

		// we can get a BeanDescriptor for an Embedded bean
		// and know that it is NOT recursive, as Embedded beans are
		// only allow to hold simple scalar types...
		BeanDescriptor<?> targetDesc = owner.getBeanDescriptor(prop.getTargetType());
		if (targetDesc == null){
		    String msg = "Could not find BeanDescriptor for "+prop.getTargetType()
		        +". Perhaps the EmbeddedId class is not registered?";
		    throw new PersistenceException(msg);
		}

		// deployment override information (column names)
		Map<String, String> propColMap = prop.getDeployEmbedded().getPropertyColumnMap();

		BeanProperty[] sourceProperties = targetDesc.propertiesBaseScalar();

		BeanProperty[] embeddedProperties = new BeanProperty[sourceProperties.length];

		for (int i = 0; i < sourceProperties.length; i++) {

			String propertyName = sourceProperties[i].getName();
			String dbColumn = propColMap.get(propertyName);
			if (dbColumn == null) {
				// dbColumn not overridden so take original
				dbColumn = sourceProperties[i].getDbColumn();
			}

			BeanPropertyOverride overrides = new BeanPropertyOverride(dbColumn);
			embeddedProperties[i] = new BeanProperty(sourceProperties[i], overrides);
		}

		return new BeanEmbeddedMeta(embeddedProperties);
	}
}
