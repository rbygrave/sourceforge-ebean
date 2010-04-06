package com.avaje.ebean.config.dbplatform;

import javax.sql.DataSource;

import com.avaje.ebean.BackgroundExecutor;

/**
 * Contract for a bridge between EBean and a specific DBMS.
 * @since 2.6, 2010-Apr
 * @author Paul Mendelson
 * @version $Revision$, $Date$
 *
 */
public interface DatabasePlatform {

	/**
	 * Return the name of the DatabasePlatform.
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * Return the DbEncrypt handler for this DB platform.
	 */
	public abstract DbEncrypt getDbEncrypt();

	/**
	 * Return a DB Sequence based IdGenerator.
	 * 
	 * @param be
	 *            the BackgroundExecutor that can be used to load the sequence
	 *            if desired
	 * @param ds
	 *            the DataSource
	 * @param seqName
	 *            the name of the sequence
	 * @param batchSize
	 *            the number of sequences that should be loaded
	 */
	public IdGenerator createSequenceIdGenerator(BackgroundExecutor be, DataSource ds, 
			String seqName, int batchSize);

/**
	 * Set the DbEncrypt handler for this DB platform.
	 */
	public abstract void setDbEncrypt(DbEncrypt dbEncrypt);

	/**
	 * Return the mapping of JDBC to DB types.
	 *
	 * @return the db type map
	 */
	public abstract DbTypeMap getDbTypeMap();

	/**
	 * Return the DDL syntax for this platform.
	 *
	 * @return the db ddl syntax
	 */
	public abstract DbDdlSyntax getDbDdlSyntax();

	/**
	 * Return the close quote for quoted identifiers.
	 *
	 * @return the close quote
	 */
	public abstract String getCloseQuote();

	/**
	 * Return the open quote for quoted identifiers.
	 *
	 * @return the open quote
	 */
	public abstract String getOpenQuote();

	/**
	 * Return the JDBC type used to store booleans.
	 *
	 * @return the boolean db type
	 */
	public abstract int getBooleanDbType();

	/**
	 * Return the data type that should be used for Blob.
	 * <p>
	 * This is typically Types.BLOB but for Postgres is Types.LONGVARBINARY for
	 * example.
	 * </p>
	 */
	public abstract int getBlobDbType();

	/**
	 * Return true if empty strings should be treated as null.
	 *
	 * @return true, if checks if is treat empty strings as null
	 */
	public abstract boolean isTreatEmptyStringsAsNull();

	/**
	 * Return the DB identity/sequence features for this platform.
	 *
	 * @return the db identity
	 */
	public abstract DbIdentity getDbIdentity();

	/**
	 * Return the SqlLimiter used to apply additional sql around
	 * a query to limit its results.
	 * <p>
	 * Basically add the clauses for limit/offset, rownum, row_number().
	 * </p>
	 *
	 * @return the sql limiter
	 */
	public abstract SqlLimiter getSqlLimiter();

	/**
	 * Convert backticks to the platform specific open quote and close quote
	 *
	 * <p>Specific plugins may implement this method to cater for
	 * platform specific naming rules.
	 * </p>
	 *
	 * @param dbName the db name
	 *
	 * @return the string
	 */
	public abstract String convertQuotedIdentifiers(String dbName);

}