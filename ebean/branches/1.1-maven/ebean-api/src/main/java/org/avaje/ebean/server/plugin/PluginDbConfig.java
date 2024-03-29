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
package org.avaje.ebean.server.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.avaje.ebean.server.autofetch.AutoFetchManager;
import org.avaje.ebean.server.autofetch.DefaultAutoFetchManager;
import org.avaje.ebean.server.core.InternalEbeanServer;
import org.avaje.ebean.server.deploy.IdentityGeneration;
import org.avaje.ebean.server.jmx.MLogControl;
import org.avaje.ebean.server.lib.resource.ResourceSource;
import org.avaje.ebean.server.lib.sql.DictionaryInfo;
import org.avaje.ebean.server.naming.NamingConvention;
import org.avaje.ebean.server.persist.Binder;
import org.avaje.ebean.server.resource.ResourceControl;
import org.avaje.ebean.server.resource.ResourceManager;
import org.avaje.ebean.server.type.DefaultTypeManager;
import org.avaje.ebean.server.type.TypeManager;
import org.avaje.ebean.util.Message;
import org.avaje.lib.log.LogFactory;

/**
 * Plugin level that defines database specific settings.
 */
public class PluginDbConfig {

	protected static final Logger logger = LogFactory.get(PluginDbConfig.class);

	protected String rowNumberWindowAlias;
	
	final String tableAliasPlaceHolder;
	
	/**
	 * The open quote used by quoted identifiers.
	 */
	protected String openQuote;

	/**
	 * The close quote used by quoted identifiers.
	 */
	protected String closeQuote;

	/**
	 * The technique used for limiting the result set. If null this will use use
	 * the rset.absolute() method to skip fetched rows.
	 */
	protected ResultSetLimit resultSetLimit;

	/**
	 * Whether this jdbc driver supports returning generated keys for inserts.
	 */
	protected boolean supportsGetGeneratedKeys;

	/**
	 * Should default to true for databases that don't support IDENTITY
	 * auto increment or sequences. IdGenerator MUST be used.
	 */
	protected char identityGeneration;

	/**
	 * Should default to true for all databases that don't support IDENTITY or
	 * auto increment. e.g. Oracle.
	 */
	protected boolean supportsSequences;

	final TypeManager typeManager;

	final NamingConvention namingConvention;

	final DictionaryInfo dictionaryInfo;
	
	final Binder binder;

	final ResultSetReader resultSetReader;

	final PluginProperties properties;

	final DataSource dataSource;

	final MLogControl logControl;

	final ResourceManager resourceManager;

	public PluginDbConfig(PluginProperties properties) {

		this.properties = properties;
		this.dataSource = properties.getDataSource();

		this.resourceManager = ResourceControl.createResourceManager(properties);

		this.typeManager = new DefaultTypeManager(properties);
		this.logControl = new MLogControl(properties);
		this.dictionaryInfo = createDictionaryInfo(dataSource);
		this.namingConvention = createNamingConvention();

		this.binder = new Binder(properties, typeManager);
		resultSetReader = new ResultSetReader();

		String rl = properties.getProperty("resultSetLimit", null);
		if (rl != null) {
			resultSetLimit = ResultSetLimit.parse(rl);
		} else {
			resultSetLimit = ResultSetLimit.JdbcRowNavigation;
		}

		tableAliasPlaceHolder =  properties.getProperty("tableAliasPlaceHolder", "${ta}");
		rowNumberWindowAlias = properties.getProperty("rowNumberWindowAlias", "as limitresult");	
		closeQuote = properties.getProperty("closequote", "\"");
		openQuote = properties.getProperty("openquote", "\"");
		
		supportsGetGeneratedKeys = properties.getPropertyBoolean("supportsGetGeneratedKeys", false);
		supportsSequences = properties.getPropertyBoolean("supportsSequences", false);
		

		String ia = properties.getProperty("identityGeneration", "auto");
		identityGeneration = IdentityGeneration.parse(ia);
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

	public String getCloseQuote() {
		return closeQuote;
	}

	public String getRowNumberWindowAlias() {
		return rowNumberWindowAlias;
	}
	
	public String getTableAliasPlaceHolder() {
		return tableAliasPlaceHolder;
	}
	
	public char getIdentityGeneration() {
		return identityGeneration;
	}

	public String getOpenQuote() {
		return openQuote;
	}

	public boolean useJdbcResultSetLimit() {
		return ResultSetLimit.JdbcRowNavigation.equals(resultSetLimit);
	}
	
	public ResultSetLimit getResultSetLimit() {
		return resultSetLimit;
	}

	public boolean isSupportsGetGeneratedKeys() {
		return supportsGetGeneratedKeys;
	}

	public boolean isSupportsSequences() {
		return supportsSequences;
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
	 * Determines the IdentityGeneration used based on the support for
	 * getGeneratedKeys and sequences. Refer to IdentityGeneration.
	 */
	public char getDefaultIdentityGeneration() {
		if (identityGeneration != IdentityGeneration.AUTO) {
			return identityGeneration;
		}

		if (!supportsGetGeneratedKeys) {
			return IdentityGeneration.ID_GENERATOR;
		}
		if (supportsSequences) {
			return IdentityGeneration.DB_SEQUENCE;
		} else {
			return IdentityGeneration.DB_IDENTITY;
		}
	}

	/**
	 * Create the nameConverter to use.
	 */
	protected NamingConvention createNamingConvention() {

		String c = properties.getProperty("namingconvention", null);
		if (c != null) {
			try {
				Class<?> cls = Class.forName(c);
				NamingConvention nc = (NamingConvention) cls.newInstance();
				return nc;

			} catch (Exception ex) {
				logger.log(Level.SEVERE, null, ex);
			}
		}
		return new NamingConvention(properties);
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

		// autoFetch file in same directory as dictionary
		File dictDir = resourceManager.getDictionaryDirectory();

		if (!dictDir.exists()) {
			// automatically create the directory if it does not exist.
			// this is probably a fairly reasonable thing to do
			if (!dictDir.mkdirs()) {
				String m = "Unable to create directory [" + dictDir + "] for autofetch file ["
						+ fileName + "]";
				throw new PersistenceException(m);
			}
		}

		return new File(dictDir, fileName);
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
			// File dictFile = new File(dictFileName);
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
