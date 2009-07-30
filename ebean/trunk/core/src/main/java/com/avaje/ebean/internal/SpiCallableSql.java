package com.avaje.ebean.internal;

import com.avaje.ebean.CallableSql;

public interface SpiCallableSql extends CallableSql {

	public BindParams getBindParams();
	
	public TransactionEventTable getTransactionEventTable();
}
