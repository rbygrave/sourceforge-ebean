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
package com.avaje.ebean.server.persist.dmlbind;

import java.util.List;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.persist.dml.Modes;

/**
 * A factory that builds Bindable for BeanPropertyAssocOne properties.
 */
public class FactoryAssocOnes {
	
	public FactoryAssocOnes() {
	}
	
	/**
	 * Add foreign key columns from associated one beans.
	 */
	public List<Bindable> create(List<Bindable> list, BeanDescriptor desc, int mode) {

		BeanPropertyAssocOne[] ones = desc.propertiesOneImported();

		for (int i = 0; i < ones.length; i++) {
			if (ones[i].isImportedPrimaryKey()){
				// excluded as already part of the primary key
				
			//} else if (ones[i].isOneToOneExported()) {
				// excluded as its the 'non-owning' side of OneToOne

			} else {
				switch (mode) {
				case Modes.MODE_INSERT:
					if (!ones[i].isInsertable()) {
						continue;
					}
					break;
				case Modes.MODE_UPDATE:
					if (!ones[i].isUpdateable()) {
						continue;
					}
					break;
				}
				Bindable item = new BindableAssocOne(ones[i]);
				list.add(item);
			}
		}

		return list;
	}
}
