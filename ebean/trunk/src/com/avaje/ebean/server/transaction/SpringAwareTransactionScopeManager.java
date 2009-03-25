package com.avaje.ebean.server.transaction;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;

import com.avaje.ebean.server.core.ServerTransaction;

/**
 * A Spring aware TransactionScopeManager.
 * <p>
 * Will look for Spring transactions and use them if they exist.
 * </p>
 */
public class SpringAwareTransactionScopeManager extends TransactionScopeManager {
	
	final DataSource dataSource;
	
	public SpringAwareTransactionScopeManager(TransactionManager transactionManager){
		super(transactionManager);
		this.dataSource = transactionManager.getDataSource();
	}

	public void commit() {
		DefaultTransactionThreadLocal.commit(serverName);
	}

	public void end() {
		DefaultTransactionThreadLocal.end(serverName);
	}

	public ServerTransaction get() {
		
		// first look in Ebean's on ThreadLocal
		ServerTransaction t = DefaultTransactionThreadLocal.get(serverName);
		if (t != null){
			// Got an already wrapped a Spring connection ... 
			// or using a Ebean started transaction 
			return t;
		}
		
		// look for a Spring managed connection/transaction
		Connection c = DataSourceUtils.getConnection(dataSource);
		if (c != null){
			// "wrap" the Spring started connection/transaction
			// and put it into Ebean's ThreadLocal 
			t = transactionManager.wrapExternalConnection(c);
			DefaultTransactionThreadLocal.set(serverName, t);
		}
		
		return t;
	}

	public void replace(ServerTransaction trans) {
		DefaultTransactionThreadLocal.replace(serverName, trans);
	}

	public void rollback() {
		DefaultTransactionThreadLocal.rollback(serverName);
	}

	public void set(ServerTransaction trans) {
		DefaultTransactionThreadLocal.set(serverName, trans);
	}

}