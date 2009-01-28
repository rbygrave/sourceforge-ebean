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
package org.avaje.ebean.server.persist.dml;

import java.util.ArrayList;
import java.util.List;

import org.avaje.ebean.server.deploy.BeanDescriptor;
import org.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import org.avaje.ebean.server.persist.dmlbind.Bindable;
import org.avaje.ebean.server.persist.dmlbind.BindableId;
import org.avaje.ebean.server.persist.dmlbind.BindableList;
import org.avaje.ebean.server.persist.dmlbind.BindableUnidirectional;
import org.avaje.ebean.server.persist.dmlbind.FactoryAssocOnes;
import org.avaje.ebean.server.persist.dmlbind.FactoryBaseProperties;
import org.avaje.ebean.server.persist.dmlbind.FactoryEmbedded;
import org.avaje.ebean.server.persist.dmlbind.FactoryId;
import org.avaje.ebean.server.persist.dmlbind.FactoryVersion;
import org.avaje.ebean.server.plugin.PluginDbConfig;

/**
 * Factory for creating InsertMeta UpdateMeta and DeleteMeta.
 */
public class MetaFactory implements Modes {

	private final FactoryVersion versionFact = new FactoryVersion();

	private final FactoryBaseProperties baseFact = new FactoryBaseProperties();

	private final FactoryEmbedded embeddedFact = new FactoryEmbedded();

	private final FactoryAssocOnes assocOneFact = new FactoryAssocOnes();

	private final FactoryId idFact = new FactoryId();

	/**
	 * Include Lobs in the base statement. Generally true. Oracle9 used to
	 * require a separate statement for Clobs and Blobs.
	 */
	private static final boolean includeLobs = true;

	private final PluginDbConfig dbConfig;

	public MetaFactory(PluginDbConfig dbConfig) {
		this.dbConfig = dbConfig;
	}

	/**
	 * Create the UpdateMeta for the given bean type.
	 */
	public UpdateMeta createUpdate(BeanDescriptor desc) {

		List<Bindable> setList = new ArrayList<Bindable>();

		baseFact.create(setList, desc, MODE_UPDATE, includeLobs);
		embeddedFact.create(setList, desc, MODE_UPDATE, includeLobs);
		assocOneFact.create(setList, desc);

		Bindable id = idFact.createId(desc);

		Bindable ver = versionFact.create(desc);

		
		List<Bindable> allList = new ArrayList<Bindable>();

		baseFact.create(allList, desc, MODE_WHERE, false);
		embeddedFact.create(allList, desc, MODE_WHERE, false);
		assocOneFact.create(allList, desc);
		
		
		Bindable setBindable = new BindableList(setList);
		Bindable allBindable = new BindableList(allList);

		return new UpdateMeta(desc, setBindable, id, ver, allBindable);
	}

	/**
	 * Create the DeleteMeta for the given bean type.
	 */
	public DeleteMeta createDelete(BeanDescriptor desc) {

		Bindable id = idFact.createId(desc);

		Bindable ver = versionFact.create(desc);

		List<Bindable> allList = new ArrayList<Bindable>();
		
		baseFact.create(allList, desc, MODE_WHERE, false);
		embeddedFact.create(allList, desc, MODE_WHERE, false);
		assocOneFact.create(allList, desc);

		Bindable allBindable = new BindableList(allList);
		
		return new DeleteMeta(desc, id, ver, allBindable);
	}

	/**
	 * Create the InsertMeta for the given bean type.
	 */
	public InsertMeta createInsert(BeanDescriptor desc) {

		BindableId id = idFact.createId(desc);

		List<Bindable> allList = new ArrayList<Bindable>();

		baseFact.create(allList, desc, MODE_INSERT, includeLobs);
		embeddedFact.create(allList, desc, MODE_INSERT, includeLobs);
		assocOneFact.create(allList, desc);

		Bindable allBindable = new BindableList(allList);
		
		BeanPropertyAssocOne unidirectional = desc.getUnidirectional();
		
		Bindable shadowFkey;
		if (unidirectional == null){
			shadowFkey = null;
		} else {
			shadowFkey = new BindableUnidirectional(desc, unidirectional);
		}
		
		return new InsertMeta(dbConfig, desc, shadowFkey, id, allBindable);
	}
}
