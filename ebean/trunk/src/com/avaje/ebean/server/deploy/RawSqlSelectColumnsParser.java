package com.avaje.ebean.server.deploy;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.config.naming.NamingConvention;

/**
 * Parses columnMapping (select clause) mapping columns to bean properties.
 */
public final class RawSqlSelectColumnsParser {

	private static Logger logger = Logger.getLogger(RawSqlSelectColumnsParser.class.getName());

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

	private final List<RawSqlColumnInfo> columns = new ArrayList<RawSqlColumnInfo>();

	private final BeanDescriptor<?> desc;

	private final NamingConvention namingConvention;

	private final RawSqlSelectBuilder parent;

	private final boolean debug;

	public RawSqlSelectColumnsParser(RawSqlSelectBuilder parent, String sqlSelect) {
		this.parent = parent;
		this.debug = parent.isDebug();
		this.namingConvention = parent.getNamingConvention();
		this.desc = parent.getBeanDescriptor();
		this.sqlSelect = sqlSelect;
		this.end = sqlSelect.length();
	}

	public List<RawSqlColumnInfo> parse() {
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

		
		BeanProperty prop = findProperty(colLabel);
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

			RawSqlColumnInfo info = new RawSqlColumnInfo(colName, colLabel, prop.getName(), prop.isScalar());
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

		String msg = "sql-select trimming quoted identifier from[" 
					+ columnLabel + "] to[" + result+ "]";
		logger.fine(msg);

		return result;
	}

	/**
	 * Find the property to match against the given resultSet column.
	 */
	private BeanProperty findProperty(String column) {

		searchColumn = column;
		int dotPos = searchColumn.indexOf(".");
		if (dotPos > -1) {
			searchColumn = searchColumn.substring(dotPos + 1);
		}

		searchColumn = removeQuotedIdentifierChars(searchColumn);

		BeanProperty matchingProp = desc.getBeanProperty(searchColumn);
		if (matchingProp != null) {
			matchDescription = "";
			return matchingProp;
		}

		// convert columnName using the namingConvention
		String propertyName = namingConvention.getPropertyFromColumn(desc.getBeanType(), searchColumn);
		matchingProp = desc.getBeanProperty(propertyName);
		if (matchingProp != null) {
			matchDescription = " ... using naming convention";
			return matchingProp;
		}

		matchDescription = " ... by linear search";

		// search all properties matching against the property db column
		BeanProperty[] propertiesBase = desc.propertiesBaseScalar();
		for (int i = 0; i < propertiesBase.length; i++) {
			BeanProperty prop = propertiesBase[i];
			if (isMatch(prop, searchColumn)) {
				return prop;
			}
		}

		BeanProperty[] propertiesId = desc.propertiesId();
		for (int i = 0; i < propertiesId.length; i++) {
			BeanProperty prop = propertiesId[i];
			if (isMatch(prop, searchColumn)) {
				return prop;
			}
		}

		BeanPropertyAssocOne<?>[] propertiesAssocOne = desc.propertiesOne();
		for (int i = 0; i < propertiesAssocOne.length; i++) {
			BeanProperty prop = propertiesAssocOne[i];
			if (isMatch(prop, searchColumn)) {
				return prop;
			}
		}
		
		return null;
	}

	private boolean isMatch(BeanProperty prop, String columnLabel) {
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
