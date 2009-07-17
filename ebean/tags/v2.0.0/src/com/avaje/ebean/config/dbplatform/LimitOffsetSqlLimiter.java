package com.avaje.ebean.config.dbplatform;

/**
 * Adds LIMIT OFFSET clauses to a SQL query.
 */
public class LimitOffsetSqlLimiter implements SqlLimiter {


	/**
	 * LIMIT keyword.
	 */
	private static final String LIMIT = "limit";

	/**
	 * OFFSET keyword.
	 */
	private static final String OFFSET = "offset";
	
	
	public SqlLimitResponse limit(SqlLimitRequest request) {
		
		StringBuilder sb = new StringBuilder(512);
		sb.append("select ");
		if (request.isDistinct()){
			sb.append("distinct ");
		}
		
		sb.append(request.getDbSql());
		
		int firstRow = request.getFirstRow();
		int lastRow = request.getMaxRows();
		if (lastRow > 0) {
			lastRow = lastRow + firstRow + 1;
		}

		sb.append(" ").append(NEW_LINE).append(LIMIT).append(" ");
		if (lastRow > 0) {
			sb.append(lastRow);
			if (firstRow > 0) {
				sb.append(" ").append(OFFSET).append(" ");
			}
		}
		if (firstRow > 0) {
			sb.append(firstRow);
		}
		
		return new SqlLimitResponse(sb.toString(), false);
	}

	
}
