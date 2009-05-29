package com.avaje.ebean.server.ddl;

import com.avaje.ebean.server.deploy.BeanDescriptor;

public class DdlSyntax {

	boolean renderIndexForFkey = true;
	boolean renderPrimaryKeyName = true;
	
	int columnNameWidth = 25;
	
	String dropTableCascade;
	
	String newLine = "\n";
	
	String identity = "auto_increment";
	
	String pkPrefix = "pk_";
	
	String disableReferentialIntegrity;
	String enableReferentialIntegrity;
	
	public String getPrimaryKeyName(BeanDescriptor<?> descriptor) {
		
		if (pkPrefix != null){
			return pkPrefix + descriptor.getBaseTable();
		}
		return null;
	}

	public String getIdentity() {
		return identity;
	}


	public void setIdentity(String identity) {
		this.identity = identity;
	}


	public int getColumnNameWidth() {
		return columnNameWidth;
	}


	public void setColumnNameWidth(int columnNameWidth) {
		this.columnNameWidth = columnNameWidth;
	}


	public String getNewLine() {
		return newLine;
	}


	public void setNewLine(String newLine) {
		this.newLine = newLine;
	}

	public String getPkPrefix() {
		return pkPrefix;
	}

	public void setPkPrefix(String pkPrefix) {
		this.pkPrefix = pkPrefix;
	}

	public String getDisableReferentialIntegrity() {
		return disableReferentialIntegrity;
	}

	public void setDisableReferentialIntegrity(String disableReferentialIntegrity) {
		this.disableReferentialIntegrity = disableReferentialIntegrity;
	}

	public String getEnableReferentialIntegrity() {
		return enableReferentialIntegrity;
	}

	public void setEnableReferentialIntegrity(String enableReferentialIntegrity) {
		this.enableReferentialIntegrity = enableReferentialIntegrity;
	}

	public boolean isRenderPrimaryKeyName() {
		return renderPrimaryKeyName;
	}

	public void setRenderPrimaryKeyName(boolean renderPrimaryKeyName) {
		this.renderPrimaryKeyName = renderPrimaryKeyName;
	}

	public boolean isRenderIndexForFkey() {
		return renderIndexForFkey;
	}

	public void setRenderIndexForFkey(boolean renderIndexForFkey) {
		this.renderIndexForFkey = renderIndexForFkey;
	}

	public String getDropTableCascade() {
		return dropTableCascade;
	}

	public void setDropTableCascade(String dropTableCascade) {
		this.dropTableCascade = dropTableCascade;
	}

	
	
}
