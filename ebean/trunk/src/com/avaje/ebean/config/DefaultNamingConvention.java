/**
 * Imilia Interactive Mobile Applications GmbH
 * Copyright (c) 2009 - all rights reserved
 *
 * Created on: Jun 29, 2009
 * Created by: emcgreal
 */

package com.avaje.ebean.config;

import java.util.logging.Logger;

import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.BeanTable;

/**
 * The Class DefaultNamingConvention.
 *
 * @author emcgreal
 */
public abstract class DefaultNamingConvention implements NamingConvention {

	private static final Logger logger = Logger.getLogger(DefaultNamingConvention.class.getName());

	/** The database platform. */
	protected DatabasePlatform databasePlatform;

	/** The max fkey length. */
	protected int maxFkeyLength;

	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.NamingConvention#getForeignKeyName(com.avaje.ebean.server.deploy.BeanPropertyAssocOne)
	 */
	public String getForeignKeyName(BeanPropertyAssocOne<?> p, int fkCount) {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("fk_");
		buffer.append(p.getBeanDescriptor().getBaseTable());
		buffer.append("_");
		buffer.append(p.getName());

		addSuffix(buffer, fkCount);

		return buffer.toString();
	}


	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.NamingConvention#getIndexName(com.avaje.ebean.server.deploy.BeanPropertyAssocOne)
	 */
	public String getIndexName(BeanPropertyAssocOne<?> p, int ixCount){
		final StringBuffer buffer = new StringBuffer();
		buffer.append("ix_");
		buffer.append(p.getBeanDescriptor().getBaseTable());
		buffer.append("_");
		buffer.append(p.getName());

		addSuffix(buffer, ixCount);

		return buffer.toString();
	}

	public String getM2MJoinTableName(BeanTable lhsTable, BeanTable rhsTable){
		StringBuffer buffer = new StringBuffer();
		buffer.append(lhsTable.getBaseTable());
		buffer.append("_");
		buffer.append(rhsTable.getBaseTable());

		// FIXME - maxFKeyLength is used as the max table name length.
		if (buffer.length() > maxFkeyLength){
			buffer.setLength(maxFkeyLength);
		}

		return buffer.toString();
	}


	protected void addSuffix(StringBuffer buffer, int count){
		final String suffixNr = Integer.toString(count);
		final int suffixLen = suffixNr.length()+ 1;

		if (buffer.length() + suffixLen > maxFkeyLength){
			buffer.setLength(maxFkeyLength-suffixLen);
		}
		buffer.append("_");
		buffer.append(suffixNr);
	}


	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.NamingConvention#setDatabasePlatform(com.avaje.ebean.config.dbplatform.DatabasePlatform)
	 */
	public void setDatabasePlatform(DatabasePlatform databasePlatform) {
		this.databasePlatform = databasePlatform;
		maxFkeyLength =  databasePlatform.getDbDdlSyntax().getMaxFkeyLength();

		logger.info("Setting maxFkeyLength to: " + maxFkeyLength);
	}
}
