/**
 * Copyright (C) 2009 Authors
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
package com.avaje.ebean.internal;

import java.sql.SQLException;
import java.util.Set;

import com.avaje.ebean.server.core.ConcurrencyMode;
import com.avaje.ebean.server.persist.dml.DmlHandler;
import com.avaje.ebean.server.persist.dmlbind.Bindable;

/**
 * A plan for executing bean updates for a given set of changed properties.
 * <p>
 * This is a cachable plan with the purpose of being being able to skip some
 * phases of the update bean processing.
 * </p>
 * <p>
 * The plans are cached by the BeanDescriptors.
 * </>
 * 
 * @author rbygrave
 */
public interface SpiUpdatePlan {

	/**
	 * Bind given the request and bean. The bean could be the oldValues bean
	 * when binding a update or delete where clause with ALL concurrency mode.
	 */
	public abstract void bindSet(DmlHandler bind, Object bean)
			throws SQLException;

	/**
	 * Return the time this plan was created.
	 */
	public abstract long getTimeCreated();

	/**
	 * Return the time this plan was last used.
	 */
	public abstract Long getTimeLastUsed();

	/**
	 * Return the hash key for this plan.
	 */
	public abstract Integer getKey();

	/**
	 * Return the concurrency mode for this plan.
	 */
	public abstract ConcurrencyMode getMode();

	/**
	 * Return the update SQL statement.
	 */
	public abstract String getSql();

	/**
	 * Return the set of bindable update properties.
	 */
	public abstract Bindable getSet();

	/**
	 * Return the properties that where changed and should be included in the
	 * update statement.
	 */
	public abstract Set<String> getProperties();

}