package com.avaje.ebean.config.dbplatform;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;

/**
 * Used to support DB specific syntax for DDL generation.
 */
public class DbDdlSyntax {

	boolean renderIndexForFkey = true;
	
	int maxConstraintNameLength= 32;
	
	int columnNameWidth = 25;
	
	String dropTableCascade;
	
	String newLine = "\r\n";
	
	String identity = "auto_increment";
	
	String pkPrefix = "pk_";
	
	String disableReferentialIntegrity;
	String enableReferentialIntegrity;
	
	String foreignKeySuffix;
	
	/**
	 * Return the primary key name for a given bean descriptor.
	 */
	public String getPrimaryKeyName(BeanDescriptor<?> descriptor) {
		
		if (pkPrefix != null){
			return pkPrefix + descriptor.getBaseTable();
		}
		return null;
	}

	/**
	 * Return the identity clause for DB's that have identities.
	 */
	public String getIdentity() {
		return identity;
	}

	/**
	 * Set the identity clause.
	 */
	public void setIdentity(String identity) {
		this.identity = identity;
	}

	/**
	 * Return the width for padding whitespace after column names.
	 */
	public int getColumnNameWidth() {
		return columnNameWidth;
	}

	/**
	 * Set the amount of padding to write after the column name.
	 */
	public void setColumnNameWidth(int columnNameWidth) {
		this.columnNameWidth = columnNameWidth;
	}

	/**
	 * Return the new line character.
	 */
	public String getNewLine() {
		return newLine;
	}

	/**
	 * Set the new line character.
	 */
	public void setNewLine(String newLine) {
		this.newLine = newLine;
	}

	/**
	 * Return the prefix used in naming primary keys.
	 */
	public String getPkPrefix() {
		return pkPrefix;
	}

	/**
	 * Set the prefix used in naming primary keys.
	 */
	public void setPkPrefix(String pkPrefix) {
		this.pkPrefix = pkPrefix;
	}

	/**
	 * Return the DB specific command to disable referential integrity
	 */
	public String getDisableReferentialIntegrity() {
		return disableReferentialIntegrity;
	}

	/**
	 * Set the DB specific command to disable referential integrity
	 */
	public void setDisableReferentialIntegrity(String disableReferentialIntegrity) {
		this.disableReferentialIntegrity = disableReferentialIntegrity;
	}

	/**
	 * Return the DB specific command to enable referential integrity
	 */
	public String getEnableReferentialIntegrity() {
		return enableReferentialIntegrity;
	}

	/**
	 * Set the DB specific command to enable referential integrity
	 */
	public void setEnableReferentialIntegrity(String enableReferentialIntegrity) {
		this.enableReferentialIntegrity = enableReferentialIntegrity;
	}

	/**
	 * Return true if indexes should be created for the foreign keys.
	 */
	public boolean isRenderIndexForFkey() {
		return renderIndexForFkey;
	}

	/**
	 * Set whether indexes should be created for the foreign keys.
	 */
	public void setRenderIndexForFkey(boolean renderIndexForFkey) {
		this.renderIndexForFkey = renderIndexForFkey;
	}

	/**
	 * Return the cascade option for the drop table command.
	 */
	public String getDropTableCascade() {
		return dropTableCascade;
	}

	/**
	 * Set the cascade option for the drop table command.
	 */
	public void setDropTableCascade(String dropTableCascade) {
		this.dropTableCascade = dropTableCascade;
	}

	/**
	 * Return the foreign key suffix.
	 */
	public String getForeignKeySuffix() {
		return foreignKeySuffix;
	}

	/**
	 * Set the foreign key suffix.
	 */
	public void setForeignKeySuffix(String foreignKeySuffix) {
		this.foreignKeySuffix = foreignKeySuffix;
	}

	/**
	 * Return the maximum length that constraint names can be for this database.
	 */
	public int getMaxConstraintNameLength() {
		return maxConstraintNameLength;
	}

	/**
	 * Set the maximum length that constraint names can be for this database.
	 */
	public void setMaxConstraintNameLength(int maxFkeyLength) {
		this.maxConstraintNameLength = maxFkeyLength;
	}

	public String getIndexName(BeanPropertyAssocOne<?> p, int ixCount){
		
		StringBuilder buffer = new StringBuilder();
		buffer.append("ix_");
		buffer.append(p.getBeanDescriptor().getBaseTable());
		buffer.append("_");
		buffer.append(p.getName());

		addSuffix(buffer, ixCount);

		return buffer.toString();
	}
	
	public String getForeignKeyName(BeanPropertyAssocOne<?> p, int fkCount) {
		
		StringBuilder buffer = new StringBuilder();
		buffer.append("fk_");
		buffer.append(p.getBeanDescriptor().getBaseTable());
		buffer.append("_");
		buffer.append(p.getName());

		addSuffix(buffer, fkCount);

		return buffer.toString();
	}
	
	/**
	 * Adds the suffix.
	 *
	 * @param buffer the buffer
	 * @param count the count
	 */
	protected void addSuffix(StringBuilder buffer, int count){
		final String suffixNr = Integer.toString(count);
		final int suffixLen = suffixNr.length()+ 1;

		if (buffer.length() + suffixLen > maxConstraintNameLength){
			buffer.setLength(maxConstraintNameLength-suffixLen);
		}
		buffer.append("_");
		buffer.append(suffixNr);
	}
}
