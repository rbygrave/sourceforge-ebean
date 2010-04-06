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
package com.avaje.ebeaninternal.server.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.ResourceFinder;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.dbplatform.GenericDatabasePlatform;

/**
 * Create a DatabasePlatform from the configuration.
 * <p>
 * Will used platform name or use the meta data from the JDBC driver to
 * determine the platform automatically.
 * </p>
 */
public class DatabasePlatformFactory {
	private static final String ATTR4ADAPTOR = "adaptorClass";
	private static final Logger logger = Logger.getLogger(DatabasePlatformFactory.class.getName());
	private String defaultClassName;
	private List<DbmsProfile> dbmsSet;

	/**
	 * Create the appropriate database specific platform.
	 */
	public DatabasePlatform create(ServerConfig serverConfig) {
		final String defaultCatalog = DatabasePlatform.class.getPackage().getName().replace('.', '/')
				+ "/dbms.xml";
		dbmsSet=new ArrayList<DbmsProfile>();
		String[] catalogs = GlobalProperties.get("ebean.dbplatform.registry", defaultCatalog).split(",");
		if (catalogs != null && catalogs.length > 0) {
			for (String cat : catalogs) {
				final String adjustedCatalog = cat.equalsIgnoreCase("default") ? defaultCatalog : cat;
				ResourceFinder in = new ResourceFinder(adjustedCatalog, GlobalProperties.getServletContext());
				if (in.exists()) {
					loadPlatforms(in.getInputSteram(), in.getSourcePath());
				} else {
					throw configError("Unable to find " + adjustedCatalog);
				}
			}
		} else {
			throw configError("No database platforms provided ebean.dbplatform.registry is blank");
		}
		try {

			if (serverConfig.getDatabasePlatformName() != null) {
				// choose based on dbName
				return byDatabaseName(serverConfig.getDatabasePlatformName());

			}
			if (serverConfig.getDataSourceConfig().isOffline()) {
				String m = "You must specify a DatabasePlatformName when you are offline";
				throw new PersistenceException(m);
			}
			// guess using meta data from driver
			return byDataSource(serverConfig.getDataSource());

		} catch (Exception ex) {
			logger.log(Level.WARNING, "??", ex);
			throw new PersistenceException(ex);
		}
	}

	private void loadPlatforms(InputStream in, String sourceLabel) {
		int oldSize=dbmsSet.size();
		final Document parser = createXmlParser(in);
		if (parser == null) {
			throw configError("Error parsing " + sourceLabel);
		}
		Element root = parser.getDocumentElement();
		if (defaultClassName == null || defaultClassName.length() <= 0) {
			defaultClassName = root.getAttribute(ATTR4ADAPTOR);
		}
		for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n instanceof Element) {
				if (n.getNodeName().equalsIgnoreCase("dbmsProduct")) {
					dbmsSet.add(new DbmsProfile((Element) n));
				} else {
					logger.info("Unexpected xml node: " + n.getNodeName());
				}
			}
		}
		logger.info("Loaded " + (dbmsSet.size()-oldSize) + " platforms from " + sourceLabel
				+(oldSize>0?" platform entries="+dbmsSet.size():""));
	}

	private PersistenceException configError(String msg) {
		logger.warning(msg);
		return new PersistenceException(msg);
	}

	/**
	 * Lookup the platform by name.
	 */
	private DatabasePlatform byDatabaseName(String dbName) throws SQLException {
		dbName = dbName.toLowerCase();
		String dbClazz = null;
		for (Iterator<DbmsProfile> i = dbmsSet.iterator(); i.hasNext() && dbClazz == null;) {
			DbmsProfile dbms = i.next();
			dbClazz = dbms.getAdaptor4EBeanDbmsCode(dbName);
		}
		if (dbClazz == null) {
			dbClazz = defaultClassName;
		}
		DatabasePlatform r = createPlatformObject(dbClazz);
		if (r != null) {
			return r;
		} else {
			throw new RuntimeException("database platform " + dbName + " is not known?");
			// return new DatabasePlatform();
		}
	}

	/**
	 * Use JDBC DatabaseMetaData to determine the platform.
	 */
	private GenericDatabasePlatform byDataSource(DataSource dataSource) {

		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			DatabaseMetaData metaData = conn.getMetaData();

			return byDatabaseMeta(metaData);

		} catch (SQLException ex) {
			throw new PersistenceException(ex);

		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException ex) {
				logger.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Find the platform by the metaData.getDatabaseProductName().
	 */
	private GenericDatabasePlatform byDatabaseMeta(DatabaseMetaData metaData) throws SQLException {

		String dbProductName = metaData.getDatabaseProductName();
		dbProductName = dbProductName.toLowerCase();

		int majorVersion = metaData.getDatabaseMajorVersion();
		String dbClazz = null;
		for (Iterator<DbmsProfile> i = dbmsSet.iterator(); i.hasNext() && dbClazz == null;) {
			DbmsProfile dbms = i.next();
			dbClazz = dbms.getAdaptor4JdbcProduct(dbProductName, majorVersion);
		}
		if (dbClazz == null) {
			dbClazz = defaultClassName;
		}
		GenericDatabasePlatform r = createPlatformObject(dbClazz);
		if (r == null)
			r = new GenericDatabasePlatform();
		return r;
	}

	private GenericDatabasePlatform createPlatformObject(String dbClazz) {
		if (dbClazz != null && dbClazz.length() > 1) {
			try {
				return (GenericDatabasePlatform) Class.forName(dbClazz).newInstance();
			} catch (InstantiationException e) {
				throw configError("Instantiate " + dbClazz);
			} catch (IllegalAccessException e) {
				throw configError("Instantiate " + dbClazz);
			} catch (ClassNotFoundException e) {
				throw configError("Instantiate " + dbClazz);
			}
		} else {
			return null;
		}
	}

	public static Document createXmlParser(InputStream inbuff) {
		// Step 1: create a DocumentBuilderFactory and configure it
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		// Optional: set various configuration options
		dbf.setValidating(false /* validation */);
		dbf.setIgnoringComments(true /* ignoreComments */);
		dbf.setIgnoringElementContentWhitespace(true /* ignoreWhitespace */);
		dbf.setCoalescing(true /* putCDATAIntoText */);

		// The opposite of creating entity ref nodes is expanding them inline
		dbf.setExpandEntityReferences(true /* !createEntityRefs */);

		// Step 2: create a DocumentBuilder that satisfies the constraints
		// specified by the DocumentBuilderFactory
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			logger.log(Level.WARNING, "Constructing parser", pce);
			return null;
		}

		// Set an ErrorHandler before parsing
		db.setErrorHandler(new XmlErrorHandler());

		// Step 3: parse the input file
		Document doc = null;
		try {
			doc = db.parse(inbuff);
		} catch (SAXException se) {
			logger.log(Level.WARNING, "parseing ", se);
			return null;
		} catch (IOException ioe) {
			logger.log(Level.WARNING, "parseing ", ioe);
			return null;
		}
		return doc;
	} // createXmlParser()

	/** Error handler to report errors and warnings */
	protected static class XmlErrorHandler implements ErrorHandler {
		// ~ Constructors
		// --------------------------------------------------------

		/** Error handler output goes here */
		XmlErrorHandler() {
		}

		// ~ Methods
		// -------------------------------------------------------------

		/**
		 * Returns a string describing parse exception details
		 */
		private String getParseExceptionInfo(SAXParseException spe) {
			String system_id = spe.getSystemId();
			if (system_id == null) {
				system_id = "null";
			}
			String info = "URI=" + system_id + " Line=" + spe.getLineNumber() + ": " + spe.getMessage();
			return info;
		}

		// The following methods are standard SAX ErrorHandler methods.
		// See SAX documentation for more info.
		public void warning(SAXParseException spe) throws SAXException {
			logger.log(Level.WARNING, "Warning: " + getParseExceptionInfo(spe), spe);
		}

		public void error(SAXParseException spe) throws SAXException {
			String message = "Error: " + getParseExceptionInfo(spe);
			throw new SAXException(message);
		}

		public void fatalError(SAXParseException spe) throws SAXException {
			String message = "Fatal Error: " + getParseExceptionInfo(spe);
			throw new SAXException(message);
		}
	} // end XmlErrorHandler class

	private class DbmsProfile {

		private String mAdaptorClassName;
		private List<String> mDbNameSet;
		private List<String> mEBeanNameSet;
		private List<DbmsVersionProfile> mVersionSet;

		public DbmsProfile(Element xml) {
			mAdaptorClassName = xml.getAttribute(ATTR4ADAPTOR);
			mDbNameSet = new ArrayList<String>();
			mEBeanNameSet = new ArrayList<String>();
			mVersionSet = new ArrayList<DbmsVersionProfile>();
			for (Node n = xml.getFirstChild(); n != null; n = n.getNextSibling()) {
				if (n instanceof Element) {
					if (n.getNodeName().equalsIgnoreCase("dbNamePrefix")) {
						mDbNameSet.add(((Element) n).getTextContent().trim());
					} else if (n.getNodeName().equalsIgnoreCase("eBeanName")) {
						mEBeanNameSet.add(((Element) n).getTextContent().trim());
					} else if (n.getNodeName().equalsIgnoreCase("dbmsVersion")) {
						mVersionSet.add(new DbmsVersionProfile((Element) n));
					}
				}
			}
		}

		public String getAdaptor4JdbcProduct(String dbProductName, int majorVersion) {
			for (String tst : mDbNameSet) {
				if (dbProductName.indexOf(tst) > -1) {
					for (DbmsVersionProfile ver : mVersionSet) {
						logger.fine(dbProductName + ": " + majorVersion + " vs " + ver.maxVersion);
						if (ver.minVersion <= majorVersion && ver.maxVersion >= majorVersion) {
							return ver.getClassName();
						}
					}
					return mAdaptorClassName;
				}
			}
			return null;
		}

		public String getClassName() {
			return mAdaptorClassName;
		}

		public String getAdaptor4EBeanDbmsCode(String dbName) {
			for (String tst : mEBeanNameSet) {
				if (tst.equalsIgnoreCase(dbName)) {
					return getClassName();
				}
			}
			for (DbmsVersionProfile ver : mVersionSet) {
				for (String tst : ver.mEBeanNameSet) {
					if (tst.equalsIgnoreCase(dbName)) {
						return ver.getClassName();
					}
				}
			}
			return null;
		}

	}

	private class DbmsVersionProfile {

		private String mAdaptorClassName;
		private List<String> mEBeanNameSet;
		private int minVersion;
		private int maxVersion;

		public DbmsVersionProfile(Element xml) {
			mAdaptorClassName = xml.getAttribute(ATTR4ADAPTOR);
			maxVersion = getIntAttribute(xml, "maxVersion", Integer.MAX_VALUE);
			minVersion = getIntAttribute(xml, "minVersion", Integer.MIN_VALUE);
			final NodeList ebeanNameList = xml.getElementsByTagName("eBeanName");
			mEBeanNameSet = new ArrayList<String>();
			for (int x = 0; x < ebeanNameList.getLength(); x++) {
				mEBeanNameSet.add(((Element) ebeanNameList.item(x)).getTextContent());
			}
		}

		public String getAdaptor4JdbcProduct(String dbProductName, int majorVersion) {
			for (String tst : mEBeanNameSet) {
				return mAdaptorClassName;
			}
			return null;
		}

		public String getClassName() {
			return mAdaptorClassName;
		}

		public boolean supportsEbeanName(String dbName) {
			for (String tst : mEBeanNameSet) {
				if (tst.equalsIgnoreCase(dbName)) {
					return true;
				}
			}
			return false;
		}

	}

	public int getIntAttribute(Element xml, String attribName, int defaultValue) {
		String txt = xml.getAttribute(attribName);
		if (txt != null && txt.length() > 0) {
			try {
				return Integer.parseInt(txt);
			} catch (NumberFormatException e) {
				logger.warning("Unable to parse " + attribName + "=" + txt);
			}
		}
		return defaultValue;
	}
}
