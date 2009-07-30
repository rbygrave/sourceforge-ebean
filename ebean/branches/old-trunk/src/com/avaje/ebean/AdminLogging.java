package com.avaje.ebean;

/**
 * Administrative control over transaction logging at runtime.
 * <p>
 * Enables an administrator to change the amount of transaction
 * logging that occurs while the application is running.
 * </p>
 */
public interface AdminLogging {

	/**
	 * Statement logging level.
	 * <p>
	 * For defining the amount of logging on queries, insert, update and delete
	 * statements.
	 * </p>
	 */
	public enum StmtLogLevel {

		/**
		 * No logging.
		 */
		NONE,

		/**
		 * Log only a summary level.
		 */
		SUMMARY,

		/**
		 * Log summary and additionally the binding variables.
		 */
		BINDING,

		/**
		 * Log all including the binding variables and generated SQL/DML.
		 */
		SQL
	}

	/**
	 * The transaction logging level.
	 * <p>
	 * Defines which transactions will log.
	 * </p>
	 */
	public enum TxLogLevel {

		/**
		 * No transaction logging.
		 */
		NONE,

		/**
		 * Have transaction logging only for explicit transactions.
		 */
		EXPLICIT,

		/**
		 * Have transaction logging for all types of transactions.
		 */
		ALL,
	}

	/**
	 * Defines if transactions share a single log file or each have their own
	 * transaction log file.
	 */
	public enum TxLogSharing {

		/**
		 * Every transaction has its own log file.
		 */
		NONE,

		/**
		 * Explicit transactions each have their own transaction log file and
		 * implicit transaction all share a common log file.
		 */
		EXPLICIT,

		/**
		 * All transactions share the same log file.
		 */
		ALL,
	}

	/**
	 * Set the transaction logging level.
	 */
	public void setTransactionLogLevel(TxLogLevel txLogLevel);
	
	/**
	 * Return the current transaction logging.
	 */
	public TxLogLevel getTransactionLogLevel();
	
	/**
	 * Set whether transactions share log files.
	 */
	public void setTransactionLogSharing(TxLogSharing txLogSharing);
	
	/**
	 * Return the log sharing mode for transactions.
	 */
	public TxLogSharing getTransactionLogSharing();
		
	/**
	 * Return the current log level for queries.
	 */
	public StmtLogLevel getQueryLevel();

	/**
	 * Set the log level for queries.
	 */
	public void setQueryLevel(StmtLogLevel sqlQueryLevel);

	/**
	 * Return the current log level for native sql queries.
	 */
	public StmtLogLevel getSqlQueryLevel();

	/**
	 * Set the log level for native sql queries.
	 */
	public void setSqlQueryLevel(StmtLogLevel sqlQueryLevel);

	/**
	 * The current log level for inserts updates and deletes.
	 */
	public StmtLogLevel getIudLevel();

	/**
	 * Set the log level for inserts updates and deletes.
	 */
	public void setIudLevel(StmtLogLevel updateLevel);

	/**
	 * Returns true if generated sql is logged to the console.
	 */
	public boolean isDebugGeneratedSql();

	/**
	 * Set to true to Log generated sql to the console.
	 */
	public void setDebugGeneratedSql(boolean debugSql);

	/**
	 * Return true if lazy loading should be debugged.
	 */
	public boolean isDebugLazyLoad();

	/**
	 * Set the debugging on lazy loading.
	 */
	public void setDebugLazyLoad(boolean debugLazyLoad);

}