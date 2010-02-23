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
     * The transaction logging level.
     * <p>
     * Defines which transactions will log.
     * </p>
     */
    public enum LogLevel {

        /**
         * No transaction logging.
         */
        NONE,

        /**
         * Transaction logging only for explicit transactions.
         */
        EXPLICIT,

        /**
         * Transaction logging for all types of transactions.
         */
        ALL,
    }
    
	/**
	 * Statement logging level.
	 * <p>
	 * For defining the amount of logging on queries, insert, update and delete
	 * statements.
	 * </p>
	 */
	public enum LogLevelStmt {

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
     * A log level for transaction begin, commit and rollback events.
     */
    public enum LogLevelTxnCommit {
        
        /**
         * Don't log any begin, commit and rollback events
         */
        NONE,
        
        /**
         * Log Commit and Rollback events.
         */
        DEBUG,
        
        /**
         * Log all begin, commit and rollback events.
         */
        VERBOSE

    }
    
	/**
	 * Defines if transactions share a single log file or each have their own
	 * transaction log file.
	 */
	public enum LogFileSharing {

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
	 * Set the overall transaction logging level.
	 */
	public void setLoggingLevel(LogLevel txLogLevel);
	
	/**
	 * Return the overall transaction logging level.
	 */
	public LogLevel getLoggingLevel();
	
	/**
	 * Set whether transactions share log files.
	 */
	public void setLogFileSharing(LogFileSharing txLogSharing);
	
	/**
	 * Return the log sharing mode for transactions.
	 */
	public LogFileSharing getLogFileSharing();
		
	/**
	 * Return the current log level for queries.
	 */
	public LogLevelStmt getLoggingLevelQuery();

	/**
	 * Set the log level for queries.
	 */
	public void setLoggingLevelQuery(LogLevelStmt sqlQueryLevel);

	/**
	 * Return the current log level for native sql queries.
	 */
	public LogLevelStmt getLoggingLevelSqlQuery();

	/**
	 * Set the log level for native sql queries.
	 */
	public void setLoggingLevelSqlQuery(LogLevelStmt sqlQueryLevel);

	/**
	 * The current log level for inserts updates and deletes.
	 */
	public LogLevelStmt getLoggingLevelIud();

	/**
	 * Set the log level for inserts updates and deletes.
	 */
	public void setLoggingLevelIud(LogLevelStmt updateLevel);

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