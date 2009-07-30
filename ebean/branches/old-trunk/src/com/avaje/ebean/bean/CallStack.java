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
package com.avaje.ebean.bean;

import java.io.Serializable;

/**
 * Represent the call stack (stack trace elements).
 * <p>
 * Used with a query to identify a CallStackQuery for AutoFetch automatic query
 * tuning.
 * </p>
 * <p>
 * This is used so that a single query called from different methods can be
 * tuned for each different call stack.
 * </p>
 * <p>
 * Note the call stack is trimmed to remove the common ebean internal elements.
 * </p>
 */
public final class CallStack implements Serializable {

	private static final long serialVersionUID = -8590644046907438579L;

	private final int hash;

	private final StackTraceElement[] callStack;

	public CallStack(StackTraceElement[] callStack) {
		this.callStack = callStack;
		int hc = 0;
		for (int i = 0; i < callStack.length; i++) {
			hc = 31 * hc + callStack[i].hashCode();
		}
		hash = hc;
	}

	/**
	 * Return the first element of the call stack.
	 */
	public StackTraceElement getFirstStackTraceElement() {
		return callStack[0];
	}

	/**
	 * Return the call stack.
	 */
	public StackTraceElement[] getCallStack() {
		return callStack;
	}

	/**
	 * Return the hash value.
	 */
	public int getHash() {
		return hash;
	}

	public String toString() {
		return hash + ":" + callStack[0];
	}

}
