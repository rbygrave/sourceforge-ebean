package com.avaje.ebean.server.deploy;

import java.util.Set;

/**
 * Converts logical property names to database columns for the raw sql.
 * <p>
 * This is used in building the where and having clauses for SqlSelect queries.
 * </p>
 */
public final class DeployPropertyParserRawSql extends DeployParser {

	
	private final RawSqlSelect rawSqlSelect;
	
	public DeployPropertyParserRawSql(RawSqlSelect rawSqlSelect) {
		this.rawSqlSelect = rawSqlSelect;
	}

	/**
	 * Returns null for raw sql queries.
	 */
	public Set<String> getIncludes() {
		return null;
	}

	public String convertWord() {
	
	    RawSqlColumnInfo columnInfo = rawSqlSelect.getRawSqlColumnInfo(word);
		if (columnInfo == null){
			return word;
		} else {
		    return columnInfo.getName();
		}
	}

}
