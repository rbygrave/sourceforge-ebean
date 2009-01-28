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
package org.avaje.ebean.server.deploy.generatedproperty;

import org.avaje.ebean.server.deploy.meta.DeployBeanProperty;

/**
 * Default implementation of GeneratedPropertyFactory.
 */
public class DefaultGeneratedPropertyFactory implements GeneratedPropertyFactory {

	CounterFactory counterFactory;

	InsertTimestampFactory insertFactory;

	UpdateTimestampFactory updateFactory;

	public DefaultGeneratedPropertyFactory() {
		counterFactory = new CounterFactory();
		insertFactory = new InsertTimestampFactory();
		updateFactory = new UpdateTimestampFactory();
	}
	
	public GeneratedProperty createCounter(DeployBeanProperty property) {
		
		return counterFactory.createCounter(property);
	}

	public GeneratedProperty createInsertTimestamp(DeployBeanProperty property) {
		
		return insertFactory.createInsertTimestamp(property);
	}

	public GeneratedProperty createUpdateTimestamp(DeployBeanProperty property) {
		
		return updateFactory.createUpdateTimestamp(property);
	}

}
