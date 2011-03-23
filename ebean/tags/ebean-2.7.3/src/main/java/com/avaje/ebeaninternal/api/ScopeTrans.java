/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebeaninternal.api;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;

import com.avaje.ebean.TxScope;

/**
 * Used internally to handle the scoping of transactions for methods.
 */
public class ScopeTrans implements Thread.UncaughtExceptionHandler {

	private static final int OPCODE_ATHROW = 191;
	//private static final int OPCODE_ATHROW = com.avaje.ebean.enhance.asm.Opcodes.ATHROW;
	
	private final SpiTransactionScopeManager scopeMgr;

	/**
	 * The suspended transaction (can be null).
	 */
	private final SpiTransaction suspendedTransaction;
	/**
	 * The transaction in scope (can be null).
	 */
	private final SpiTransaction transaction;

	/**
	 * If true by default rollback on Checked exceptions.
	 */
	private final boolean rollbackOnChecked;

	/**
	 * True if the transaction was created and hence should be committed
	 * on finally if it hasn't already been rolled back.
	 */
	private final boolean created;

	/**
	 * Explicit set of Exceptions that DO NOT cause a rollback to occur.
	 */
	private final ArrayList<Class<? extends Throwable>> noRollbackFor;

	/**
	 * Explicit set of Exceptions that DO cause a rollback to occur.
	 */
	private final ArrayList<Class<? extends Throwable>> rollbackFor;


	private final UncaughtExceptionHandler originalUncaughtHandler;

	/**
	 * Flag set when a rollback has occurred.
	 */
	private boolean rolledBack;
	
	
	public ScopeTrans(boolean rollbackOnChecked, boolean created, SpiTransaction transaction, TxScope txScope,
			SpiTransaction suspendedTransaction,  SpiTransactionScopeManager scopeMgr) {

		this.rollbackOnChecked = rollbackOnChecked;
		this.created = created;
		this.transaction = transaction;
		this.suspendedTransaction = suspendedTransaction;
		this.scopeMgr = scopeMgr;
		
		this.noRollbackFor = txScope.getNoRollbackFor();
		this.rollbackFor = txScope.getRollbackFor();
		
		Thread t = Thread.currentThread();
		originalUncaughtHandler = t.getUncaughtExceptionHandler();
		
		t.setUncaughtExceptionHandler(this);
	}
	
	/**
	 * Called when the Thread catches any uncaught exception.
	 * For example, an unexpected NullPointerException or Error.
	 */
	public void uncaughtException(Thread thread, Throwable e) {
		
		// rollback transaction if required
		caughtThrowable(e);
		
		// reinstate suspended transaction and
		// original uncaughtExceptionHandler if required
		onFinally();
		
		if (originalUncaughtHandler != null){
			originalUncaughtHandler.uncaughtException(thread, e);
		}
	}
	
	/**
	 * Returned via RETURN or expected Exception from the method.
	 * @param returnOrThrowable the return value or Throwable
	 * @param opCode indicates 
	 */
	public void onExit(Object returnOrThrowable, int opCode) {
		
		if (opCode == OPCODE_ATHROW){
			// exited with a Throwable
			caughtThrowable((Throwable)returnOrThrowable);
		}
		onFinally();
	}
	
	
	/**
	 * Commit if the transaction exists and has not already been rolled back.
	 * Also reinstate the suspended transaction if there was one.
	 */
	public void onFinally() {
		try {
			if (originalUncaughtHandler != null){
				Thread.currentThread().setUncaughtExceptionHandler(originalUncaughtHandler);
			}
			
			if (!rolledBack && created) {
				transaction.commit();
			}
		
		} finally {
			if (suspendedTransaction != null){
				// put the previously suspended transaction 
				// back onto the ThreadLocal or equivalent
				scopeMgr.replace(suspendedTransaction);
			}
		}
	}

	/**
	 * An Error was caught and this ALWAYS causes a rollback to occur.
	 * Returns the error and this should be thrown by the calling code.
	 */
	public Error caughtError(Error e) {
		rollback(e);
		return e;
	}
	
	/**
	 * An Exception was caught and may or may not cause a rollback to occur.
	 * Returns the exception and this should be thrown by the calling code.
	 */
	public <T extends Throwable> T caughtThrowable(T e) {
		
		if (isRollbackThrowable(e)) {
			rollback(e);
		}
		return e;
	}

	private void rollback(Throwable e) {
		if (transaction != null && transaction.isActive()) {
			// transaction is null for NOT_SUPPORTED and sometimes SUPPORTS
			// and Inactive (already rolled back) if nested REQUIRED
			transaction.rollback(e);
		}
		rolledBack = true;
	}

	/**
	 * Return true if this throwable should cause a rollback to occur.
	 */
	private boolean isRollbackThrowable(Throwable e) {

		if (e instanceof Error){
			return true;
		}
		
		if (noRollbackFor != null){
			for (int i = 0; i < noRollbackFor.size(); i++) {
				if (noRollbackFor.get(i).equals(e.getClass())) {
					
					// explicit no rollback for this one
					return false;
				}
			}
		}

		if (rollbackFor != null){
			for (int i = 0; i < rollbackFor.size(); i++) {
				if (rollbackFor.get(i).equals(e.getClass())) {
					// explicit rollback for this one
					return true;
				}
			}
		}
		
		
		if (e instanceof RuntimeException) {
			return true;
			
		} else {
			// checked exceptions...
			// EJB defaults this to false which is not intuitive IMO
			// Ebean makes this configurable (default to true)
			return rollbackOnChecked;
		}
	}
	

}
