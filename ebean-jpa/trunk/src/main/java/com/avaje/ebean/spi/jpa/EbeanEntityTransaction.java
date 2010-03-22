package com.avaje.ebean.spi.jpa;

import javax.persistence.EntityTransaction;

import com.avaje.ebean.Transaction;

public class EbeanEntityTransaction implements EntityTransaction {
	private Transaction m_transaction;

	public EbeanEntityTransaction(Transaction t) {
		m_transaction=t;
	}

	public void begin() {
	}

	public void commit() {
		m_transaction.commit();
	}

	public boolean getRollbackOnly() {
		return false;
	}

	public boolean isActive() {
		return m_transaction.isActive();
	}

	public void rollback() {
		m_transaction.rollback();
	}

	public void setRollbackOnly() {
		// TODO Auto-generated method stub

	}

}
