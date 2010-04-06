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
package com.avaje.ebeaninternal.net;

import java.util.HashMap;

import com.avaje.ebean.Transaction;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.net.CommandProcessor;

/**
 * The context by which a command is executed.
 * <p>
 * Access to the CommandProcessor plus can access session information such as
 * user transactions.
 * </p>
 */
public class CommandContext {

	SpiEbeanServer server;

	CommandProcessor processor;

	TransactionMap transMap;

	public CommandContext() {
		transMap = new TransactionMap();
	}

	public TransactionMap getTransMap() {
		return transMap;
	}

	/**
	 * Get the appropriate server to run the command.
	 */
	public SpiEbeanServer getServer() {
		return server;
	}

	/**
	 * Set the server to run the command against.
	 */
	public void setServer(SpiEbeanServer server) {
		this.server = server;
	}

	/**
	 * Set the commandProcessor.
	 */
	public void setProcessor(CommandProcessor processor) {
		this.processor = processor;
	}

	/**
	 * Return the commandProcessor.
	 */
	public CommandProcessor getProcessor() {
		return processor;
	}

	/**
	 * Map of the user transactions.
	 * <p>
	 * The transactionId is the client transactionId.
	 * Created by the client and sent with the command.
	 * </p>
	 */
	public static class TransactionMap {

		HashMap<String,Transaction> transMap = new HashMap<String, Transaction>();

		/**
		 * Get a transaction using the client transactionId.
		 */
		public Transaction get(String tid) {
			return (Transaction) transMap.get(tid);
		}

		/**
		 * Set a transaction using the client transactionId.
		 */
		public void put(String tid, Transaction t) {
			transMap.put(tid, t);
		}

		/**
		 * Remove a transaction using the client transactionId.
		 */
		public Transaction remove(String tid) {
			return (Transaction) transMap.remove(tid);
		}
	}
}
