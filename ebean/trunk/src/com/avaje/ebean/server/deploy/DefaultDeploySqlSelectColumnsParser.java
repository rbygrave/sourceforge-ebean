package com.avaje.ebean.server.deploy;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.config.NamingConvention;
import com.avaje.ebean.server.deploy.DeploySqlSelect.ColumnInfo;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;

/**
 * Parses columnMapping (select clause) mapping columns to bean properties.
 */
public final class DefaultDeploySqlSelectColumnsParser {

	private static Logger logger = Logger.getLogger(DefaultDeploySqlSelectColumnsParser.class.getName());

	/**
	 * Description of how the match was made.
	 */
	private String matchDescription;

	/**
	 * The actual column string used in search for a matching property.
	 * <p>
	 * This has table alias' and quoted identifiers removed.
	 * </p>
	 */
	private String searchColumn;

	private int columnIndex;

	private int pos;

	private final int end;

	private final String sqlSelect;

	private final List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

	private final DeployBeanDescriptor<?> deployDesc;

	private final NamingConvention namingConvention;

	private final DefaultDeploySqlSelectParser parent;

	private final boolean debug;

	public DefaultDeploySqlSelectColumnsParser(DefaultDeploySqlSelectParser parent, String sqlSelect) {
		this.parent = parent;
		this.debug = parent.debug;
		this.namingConvention = parent.namingConvention;
		this.deployDesc = parent.deployDesc;
		this.sqlSelect = sqlSelect;
		this.end = sqlSelect.length();
	}

	public List<ColumnInfo> parse() {
		while (pos <= end) {
			nextColumnInfo();
		}
		return columns;
	}

	private void nextColumnInfo() {
		int start = pos;
		nextComma();
		String colInfo = sqlSelect.substring(start, pos);
		pos++;
		colInfo = colInfo.trim();
		int secLastSpace = -1;
		int lastSpace = colInfo.lastIndexOf(' ');
		if (lastSpace > -1) {
			secLastSpace = colInfo.lastIndexOf(' ', lastSpace - 1);
		}
		String colName = null;
		String colLabel = null;
		if (lastSpace == -1) {
			// no column alias
			colName = colInfo;
			colLabel = colName;
		} else if (secLastSpace == -1) {
			// no 'as' keyword
			colName = colInfo.substring(0, lastSpace);
			colLabel = colInfo.substring(lastSpace + 1);
			if (colName.equals("")) {
				colName = colLabel;
			}
		} else {
			// check for as keyword
			String expectedAs = colInfo.substring(secLastSpace + 1, lastSpace);
			if (expectedAs.toLowerCase().equals("as")) {
				colName = colInfo.substring(0, secLastSpace);
				colLabel = colInfo.substring(lastSpace + 1);
			} else {
				String msg = "Error in " + parent.getErrName() + ". ";
				msg += "Expected \"AS\" keyword but got [" + expectedAs + "] in select clause ["
						+ colInfo + "]";
				throw new PersistenceException(msg);
			}
		}

		
		DeployBeanProperty prop = findProperty(colLabel);
		if (prop == null) {
			if (debug) {
				String msg = "ColumnMapping ... idx[" + columnIndex
						+ "] ERROR, no property found to match... column[" + colName + "] label[" + colLabel
						+ "] search[" + searchColumn + "]";
				parent.debug(msg);
			}
			String msg = "Error in " + parent.getErrName() + ". ";
			msg += "No matching bean property for column[" + colName + "] columnLabel[" + colLabel
					+ "] idx[" + columnIndex + "] using search[" + searchColumn + "] found?";
			logger.log(Level.SEVERE, msg);

		} else {
			
			
			String msg = null;
			if (debug || logger.isLoggable(Level.FINE)) {
				msg = "ColumnMapping ... idx[" + columnIndex + "] match column[" + colName
						+ "] label[" + colLabel + "] to property[" + prop + "]"
						+ matchDescription;
			}
			if (debug) {
				parent.debug(msg);
			}
			if (logger.isLoggable(Level.FINE)) {
				logger.fine(msg);
			}

			ColumnInfo info = new ColumnInfo(colName, colLabel, prop.getName(), prop.isScalar());
			columns.add(info);
			columnIndex++;

		}
	}

	private String removeQuotedIdentifierChars(String columnLabel) {

		char c = columnLabel.charAt(0);
		if (Character.isJavaIdentifierStart(c)) {
			return columnLabel;
		}

		// trim off first and last character
		String result = columnLabel.substring(1, columnLabel.length() - 1);

		logger.fine("sql-select trimming quoted identifier from[" + columnLabel + "] to[" + result
				+ "]");

		return result;
	}

	/**
	 * Find the property to match against the given resultSet column.
	 */
	private DeployBeanProperty findProperty(String column) {

		searchColumn = column;
		int dotPos = searchColumn.indexOf(".");
		if (dotPos > -1) {
			searchColumn = searchColumn.substring(dotPos + 1);
		}

		searchColumn = removeQuotedIdentifierChars(searchColumn);

		DeployBeanProperty matchingProp = deployDesc.getBeanProperty(searchColumn);
		if (matchingProp != null) {
			matchDescription = "";
			return matchingProp;
		}

		// convert columnName using the namingConvention
		String propertyName = namingConvention.getPropertyFromColumn(deployDesc.getBeanType(), searchColumn);
		matchingProp = deployDesc.getBeanProperty(propertyName);
		if (matchingProp != null) {
			matchDescription = " ... using naming convention";
			return matchingProp;
		}

		matchDescription = " ... by linear search";

		// search all properties matching against the property db column
		List<DeployBeanProperty> propertiesBase = deployDesc.propertiesBase();
		for (int i = 0; i < propertiesBase.size(); i++) {
			DeployBeanProperty prop = propertiesBase.get(i);
			if (isMatch(prop, searchColumn)) {
				return prop;
			}
		}

		List<DeployBeanProperty> propertiesId = deployDesc.propertiesId();
		for (int i = 0; i < propertiesId.size(); i++) {
			DeployBeanProperty prop = propertiesId.get(i);
			if (isMatch(prop, searchColumn)) {
				return prop;
			}
		}

		List<DeployBeanPropertyAssocOne<?>> propertiesAssocOne = deployDesc.propertiesAssocOne();
		for (int i = 0; i < propertiesAssocOne.size(); i++) {
			DeployBeanProperty prop = propertiesAssocOne.get(i);
			if (isMatch(prop, searchColumn)) {
				return prop;
			}
		}
		
		return null;
	}

	private boolean isMatch(DeployBeanProperty prop, String columnLabel) {
		if (columnLabel.equalsIgnoreCase(prop.getDbColumn())) {
			return true;
		}
		if (columnLabel.equalsIgnoreCase(prop.getName())) {
			return true;
		}
		return false;
	}

	private int nextComma() {
		boolean inQuote = false;
		while (pos < end) {
			char c = sqlSelect.charAt(pos);
			if (c == '\'') {
				inQuote = !inQuote;
			} else if (!inQuote && c == ',') {
				return pos;
			}
			pos++;
		}
		return pos;
	}
}
