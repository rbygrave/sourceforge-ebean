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
package com.avaje.ebean.common;

import java.io.Serializable;

/**
 * Represents a "origin" of an ORM object graph. This combines the call stack
 * and query plan hash.
 * <p>
 * The call stack is included so that the query can have different tuned fetches
 * for each unique call stack. For example, a query to fetch a customer could be
 * called by three different methods and each can be treated as a separate
 * origin point (and autoFetch can tune each one separately).
 * </p>
 */
public final class ObjectGraphOrigin implements Serializable {

	private static final long serialVersionUID = 410937765287968708L;

	/**
	 * The Original query hash (prior to autofetch tuning).
	 */
	private final int queryPlanHash;

	private final CallStack callStack;

	private final String key;

	private final String beanType;

	public ObjectGraphOrigin(int queryPlanHash, CallStack callStack, String beanType) {
		this.queryPlanHash = queryPlanHash;
		this.callStack = callStack;
		this.beanType = beanType;
		this.key = queryPlanHash + ":" + callStack.getHash();
	}

	/**
	 * The key includes the queryPlan hash and the callStack hash. This becomes
	 * the unique identifier for a query point.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * The type of bean the query is fetching.
	 */
	public String getBeanType() {
		return beanType;
	}

	/**
	 * Return the query plan hash calculated prior to any autofetch tuning.
	 */
	public int getQueryPlanHash() {
		return queryPlanHash;
	}
	
	/**
	 * The call stack involved.
	 */
	public CallStack getCallStack() {
		return callStack;
	}

	public String getFirstStackElement() {
		return callStack.getFirstStackTraceElement().toString();
	}

	public String toString() {
		return key + " " + beanType + " " + callStack.getFirstStackTraceElement();
	}

}
