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
package com.avaje.ebean.server.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import com.avaje.ebean.NamingConvention;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.autofetch.DefaultAutoFetchManager;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.jmx.MLogControl;
import com.avaje.ebean.server.lib.resource.ResourceSource;
import com.avaje.ebean.server.lib.sql.DictionaryInfo;
import com.avaje.ebean.server.lib.util.FactoryHelper;
import com.avaje.ebean.server.naming.DefaultNamingConvention;
import com.avaje.ebean.server.persist.Binder;
import com.avaje.ebean.server.resource.ResourceControl;
import com.avaje.ebean.server.resource.ResourceManager;
import com.avaje.ebean.server.type.DefaultTypeManager;
import com.avaje.ebean.server.type.TypeManager;
import com.avaje.ebean.util.Message;

/**
 * Plugin level that defines database specific settings.
 */
public class PluginDbConfig {

	private static final Logger logger = Logger.getLogger(PluginDbConfig.class.getName());

	final DbSpecific dbSpecific;
		
	final String tableAliasPlaceHolder;

	final TypeManager typeManager;

	final NamingConvention namingConvention;

	final DictionaryInfo dictionaryInfo;
	
	final Binder binder;

	final ResultSetReader resultSetReader;

	final PluginProperties properties;

	final DataSource dataSource;

	final MLogControl logControl;

	final ResourceManager resourceManager;

	public PluginDbConfig(PluginProperties properties, DbSpecific dbSpecific) {

		this.properties = properties;
		this.dataSource = properties.getDataSource();
		this.dbSpecific = dbSpecific;
		
		this.resourceManager = ResourceControl.createResourceManager(properties);

		this.typeManager = new DefaultTypeManager(properties);
		this.logControl = new MLogControl(properties);
		this.dictionaryInfo = createDictionaryInfo(dataSource);
		this.namingConvention = createNamingConvention();

		this.binder = new Binder(properties, typeManager);
		this.resultSetReader = new ResultSetReader();

		this.tableAliasPlaceHolder =  properties.getProperty("tableAliasPlaceHolder", "${ta}");
	}
	
	public String getSql(String fileName) {
		return resourceManager.getSql(fileName);
	}

	public MLogControl getLogControl() {
		return logControl;
	}

	public DictionaryInfo getDictionaryInfo() {
		return dictionaryInfo;
	}

	public PluginProperties getProperties() {
		return properties;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public String getTableAliasPlaceHolder() {
		return tableAliasPlaceHolder;
	}

	public DbSpecific getDbSpecific() {
		return dbSpecific;
	}
	
	public NamingConvention getNamingConvention() {
		return namingConvention;
	}

	public TypeManager getTypeManager() {
		return typeManager;
	}

	public Binder getBinder() {
		return binder;
	}

	public ResultSetReader getResultSetReader() {
		return resultSetReader;
	}

	/**
	 * Create or get the NamingConvention to use.
	 */
	protected NamingConvention createNamingConvention() {

		NamingConvention nc = properties.getServerConfiguration().getNamingConvention();
		if (nc != null){
			// the NamingConvention was set of the ServerConfiguration so use that
			return nc;
		}
		
		String c = properties.getProperty("namingconvention", null);
		if (c != null) {
			// create a specified constructor
			try {
				Class<?> cls = Class.forName(c);
				// see if it has a constructor that takes PluginProperties
				Constructor<?> constructor = FactoryHelper.findConstructor(cls, PluginProperties.class);
				if (constructor != null){
					return (NamingConvention)constructor.newInstance(properties);
				} else {
					// use the default constructor
					return (NamingConvention)cls.newInstance();
				}

			} catch (Exception ex) {
				logger.log(Level.SEVERE, null, ex);
			}
		}
		// use the default NamingConvention
		return new DefaultNamingConvention(properties);
	}

	/**
	 * Return ResourceSource which provides access to xml and sql deployment
	 * resources.
	 */
	public ResourceSource getResourceSource() {
		return resourceManager.getResourceSource();
	}

	/**
	 * Return the file name of the dictionary file.
	 */
	protected File getDictionaryFile() {

		String dictFileName = properties.getServerName();
		if (dictFileName == null) {
			dictFileName = "primarydatabase";
		}
		dictFileName = ".ebean."+dictFileName+".dictionary";

		File dictDir = resourceManager.getDictionaryDirectory();

		if (!dictDir.exists()) {
			// automatically create the directory if it does not exist.
			// this is probably a fairly reasonable thing to do
			if (!dictDir.mkdirs()) {
				String m = "Unable to create directory [" + dictDir + "] for dictionary file ["
						+ dictFileName + "]";
				throw new PersistenceException(m);
			}
		}

		return new File(dictDir, dictFileName);
	}

	
	/**
	 * Return the file name of the autoFetch meta data.
	 */
	protected File getAutoFetchFile() {

		String fileName = properties.getServerName();
		if (fileName == null) {
			fileName = "primarydatabase";
		}
		fileName = ".ebean."+fileName+".autofetch";

		File dir = resourceManager.getAutofetchDirectory();

		if (!dir.exists()) {
			// automatically create the directory if it does not exist.
			// this is probably a fairly reasonable thing to do
			if (!dir.mkdirs()) {
				String m = "Unable to create directory [" + dir + "] for autofetch file ["+ fileName + "]";
				throw new PersistenceException(m);
			}
		}

		return new File(dir, fileName);
	}
	
	/**
	 * Create the DictionaryInfo and set its DataSource. Note the DictionaryInfo
	 * can be deserialized from a file for performance reasons.
	 */
	protected DictionaryInfo createDictionaryInfo(DataSource ds) {

		// find the full name of the dictionary file
		// Note this file is written to so needs write
		// permissions on the directory.
		File dictFile = getDictionaryFile();

		DictionaryInfo dictInfo = null;

		// set this to false to force the dictionary to be populated
		// by querying the database meta data (Maybe very slow)
		String readDict = properties.getProperty("dictionary.readfromfile", "true");
		if (readDict.equalsIgnoreCase("true")) {
			// try to deserialize the DictionaryInfo from a file
			dictInfo = deserializeDictionary(dictFile);
		}

		if (dictInfo == null) {
			// not deserialized from file so create as empty
			// It will be populated automatically by querying the
			// database meta data
			dictInfo = new DictionaryInfo();
		}

		// set the data source
		dictInfo.setDataSource(ds);

		String writeDict = properties.getProperty("dictionary.writetofile", "true");
		if (writeDict.equalsIgnoreCase("true")) {
			dictInfo.setSerializeToFile(dictFile.getAbsolutePath());
		}
		return dictInfo;
	}

	protected DictionaryInfo deserializeDictionary(File dictFile) {
		try {
			if (!dictFile.exists()) {
				return null;
			}
			FileInputStream fi = new FileInputStream(dictFile);
			ObjectInputStream ois = new ObjectInputStream(fi);
			DictionaryInfo dictInfo = (DictionaryInfo) ois.readObject();

			logger.info(Message.msg("plugin.dictionary", dictFile.getAbsolutePath()));
			return dictInfo;

		} catch (Exception ex) {
			logger.log(Level.SEVERE, Message.msg("plugin.dictionary.error"), ex);
			return null;
		}
	}
	
	public AutoFetchManager createAutoFetchManager(InternalEbeanServer server){
		
		AutoFetchManager manager = createAutoFetchManager();
		manager.setOwner(server);
		
		return manager;
	}
	
	protected AutoFetchManager createAutoFetchManager() {

		File autoFetchFile = getAutoFetchFile();

		AutoFetchManager autoFetchManager = null;

		// set this to false to force the dictionary to be populated
		// by querying the database meta data (Maybe very slow)
		boolean readFile = properties.getPropertyBoolean("autofetch.readfromfile", true);
		if (readFile) {
			// try to deserialize the DictionaryInfo from a file
			autoFetchManager = deserializeAutoFetch(autoFetchFile);
		}

		if (autoFetchManager == null) {
			// not deserialized from file so create as empty
			// It will be populated automatically by querying the
			// database meta data
			autoFetchManager = new DefaultAutoFetchManager(autoFetchFile.getAbsolutePath());
		}
		
		return autoFetchManager;
	}
	
	protected AutoFetchManager deserializeAutoFetch(File autoFetchFile) {
		try {
			
			if (!autoFetchFile.exists()) {
				return null;
			}
			FileInputStream fi = new FileInputStream(autoFetchFile);
			ObjectInputStream ois = new ObjectInputStream(fi);
			AutoFetchManager profListener = (AutoFetchManager) ois.readObject();
			
			logger.info("AutoFetch deserialized from file ["+autoFetchFile.getAbsolutePath()+"]");
			
			return profListener;

		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Error loading autofetch file "+autoFetchFile.getAbsolutePath(), ex);
			return null;
		}
	}
}
