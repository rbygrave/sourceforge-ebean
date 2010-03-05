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
package com.avaje.ebean.server.persist.dml;

import java.util.ArrayList;
import java.util.List;

import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.dbplatform.DbEncrypt;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.persist.dmlbind.Bindable;
import com.avaje.ebean.server.persist.dmlbind.BindableId;
import com.avaje.ebean.server.persist.dmlbind.BindableList;
import com.avaje.ebean.server.persist.dmlbind.BindableUnidirectional;
import com.avaje.ebean.server.persist.dmlbind.FactoryAssocOnes;
import com.avaje.ebean.server.persist.dmlbind.FactoryBaseProperties;
import com.avaje.ebean.server.persist.dmlbind.FactoryEmbedded;
import com.avaje.ebean.server.persist.dmlbind.FactoryId;
import com.avaje.ebean.server.persist.dmlbind.FactoryVersion;

/**
 * Factory for creating InsertMeta UpdateMeta and DeleteMeta.
 */
public class MetaFactory {

    private final FactoryBaseProperties baseFact;
    private final FactoryEmbedded embeddedFact;
	private final FactoryVersion versionFact = new FactoryVersion();
	private final FactoryAssocOnes assocOneFact = new FactoryAssocOnes();

	private final FactoryId idFact = new FactoryId();

	/**
	 * Include Lobs in the base statement. Generally true. Oracle9 used to
	 * require a separate statement for Clobs and Blobs.
	 */
	private static final boolean includeLobs = true;

	private final DatabasePlatform dbPlatform;
	
	private final boolean emptyStringAsNull;
	
	public MetaFactory(DatabasePlatform dbPlatform) {
		this.dbPlatform = dbPlatform;
		this.emptyStringAsNull = dbPlatform.isTreatEmptyStringsAsNull();
		
		// to bind encryption data before or after the encryption key
		DbEncrypt dbEncrypt = dbPlatform.getDbEncrypt();
		boolean bindEncryptDataFirst = dbEncrypt == null ? true : dbEncrypt.isBindEncryptDataFirst();
		
		this.baseFact = new FactoryBaseProperties(bindEncryptDataFirst);
		this.embeddedFact = new FactoryEmbedded(bindEncryptDataFirst);
	}

	/**
	 * Create the UpdateMeta for the given bean type.
	 */
	public UpdateMeta createUpdate(BeanDescriptor<?> desc) {

		List<Bindable> setList = new ArrayList<Bindable>();

		baseFact.create(setList, desc, DmlMode.UPDATE, includeLobs);
		embeddedFact.create(setList, desc, DmlMode.UPDATE, includeLobs);
		assocOneFact.create(setList, desc, DmlMode.UPDATE);

		Bindable id = idFact.createId(desc);

		Bindable ver = versionFact.create(desc);

		
		List<Bindable> allList = new ArrayList<Bindable>();

		baseFact.create(allList, desc, DmlMode.WHERE, false);
		embeddedFact.create(allList, desc, DmlMode.WHERE, false);
		assocOneFact.create(allList, desc, DmlMode.WHERE);
		
		
		Bindable setBindable = new BindableList(setList);
		Bindable allBindable = new BindableList(allList);

		return new UpdateMeta(emptyStringAsNull, desc, setBindable, id, ver, allBindable);
	}

	/**
	 * Create the DeleteMeta for the given bean type.
	 */
	public DeleteMeta createDelete(BeanDescriptor<?> desc) {

		Bindable id = idFact.createId(desc);

		Bindable ver = versionFact.create(desc);

		List<Bindable> allList = new ArrayList<Bindable>();
		
		baseFact.create(allList, desc, DmlMode.WHERE, false);
		embeddedFact.create(allList, desc, DmlMode.WHERE, false);
		assocOneFact.create(allList, desc, DmlMode.WHERE);

		Bindable allBindable = new BindableList(allList);
		
		return new DeleteMeta(emptyStringAsNull, desc, id, ver, allBindable);
	}

	/**
	 * Create the InsertMeta for the given bean type.
	 */
	public InsertMeta createInsert(BeanDescriptor<?> desc) {

		BindableId id = idFact.createId(desc);

		List<Bindable> allList = new ArrayList<Bindable>();

		baseFact.create(allList, desc, DmlMode.INSERT, includeLobs);
		embeddedFact.create(allList, desc, DmlMode.INSERT, includeLobs);
		assocOneFact.create(allList, desc, DmlMode.INSERT);

		Bindable allBindable = new BindableList(allList);
		
		BeanPropertyAssocOne<?> unidirectional = desc.getUnidirectional();
		
		Bindable shadowFkey;
		if (unidirectional == null){
			shadowFkey = null;
		} else {
			shadowFkey = new BindableUnidirectional(desc, unidirectional);
		}
		
		return new InsertMeta(dbPlatform, desc, shadowFkey, id, allBindable);
	}
}
