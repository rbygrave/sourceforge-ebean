package com.avaje.ebean.spring;

import org.springframework.jdbc.datasource.ConnectionHolder;

import com.avaje.ebean.server.transaction.ExternalJdbcTransaction;
import com.avaje.ebean.server.transaction.TransactionManager;

public class SpringJdbcTransaction extends ExternalJdbcTransaction {

	final ConnectionHolder holder;
	
	public SpringJdbcTransaction(ConnectionHolder holder, TransactionManager manager) {
		super("s"+holder.hashCode(), true, holder.getConnection(), manager);
		this.holder = holder;
	}

	@Override
	public boolean isActive() {
		return holder.isSynchronizedWithTransaction();
	}

	public ConnectionHolder getConnectionHolder() {
		return holder;
	}
	
	
	
}
