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
package com.avaje.ebean.server.util;

import java.util.logging.Logger;

import com.avaje.ebean.server.core.ServerCache;
import com.avaje.ebean.server.lib.cache.Element;
import com.avaje.ebean.server.lib.cache.Validator;
import com.avaje.ebean.server.transaction.TableState;
import com.avaje.ebean.util.Message;

/**
 * Validates a cached element against a list of tables.
 * <p>
 * If any of the tables have been modified then the element is invalidated. This
 * is used when caching beans and lookups to invalidate.
 * </p>
 */
public class TableValidator implements Validator {

	private static final Logger logger = Logger.getLogger(TableValidator.class.getName());
	
    /**
     * false if only dependent on updates and deletes. Set to true if
     * additionally dependent on inserts.
     */
    private boolean isDependantOnInserts = true;

    /**
     * The tables this is dependent on.
     */
    private final String[] tableNames;

    /**
     * set to false once it is invalidated.
     */
    private boolean stillValid = true;

    /**
     * set to false to stop logging of the invalidation event.
     */
    private boolean logInvalidEvent = true;

    /**
     * A string description.
     */
    private String description = "";

    /**
     * The type of invalidation that should occur. Invalidate the element or the
     * entire cache.
     */
    private int invalidStatus = Validator.CACHE_INVALID;

    /**
     * Has access to the table states.
     */
    private final ServerCache cacheAccess;

    /**
     * Create the TableValidator.
     * 
     * @param tableNames the tables this is dependent on
     * @param cacheAccess the cacheAccess is aware of the table state
     */
    public TableValidator(String[] tableNames, ServerCache cacheAccess) {
        this.tableNames = tableNames;
        this.cacheAccess = cacheAccess;
    }

    /**
     * When a element invalidates. Invalidate only the element rather than the
     * entire cache. Used for Lookup caches.
     */
    public void setInvalidatesElement() {
        this.invalidStatus = Validator.ELEMENT_INVALID;
    }

    /**
     * When an element invalidates. Invalidate the entire cache this element is
     * in. Used for bean caches.
     */
    public void setInvalidatesCache() {
        this.invalidStatus = Validator.CACHE_INVALID;
    }

    /**
     * This is dependant on Updates and Deletes only.
     */
    public void setDependantOnInserts(boolean dependantOnInserts) {
        this.isDependantOnInserts = dependantOnInserts;
    }

    /**
     * Test to see if the element is valid. Returns one of ELEMENT_VALID,
     * ELEMENT_INVALID, CACHE_INVALID.
     * <p>
     * ELEMENT_VALID = the element is valid<br />
     * ELEMENT_INVALID = invalidate this element<br />
     * CACHE_INVALID = invalidate the cache this element is in<br />
     * </p>
     */
    public int isValid(Element element) {
        if (tableNames == null) {
            return Validator.ELEMENT_VALID;
        }
        if (!stillValid) {
            // once invalid, always invalid... so just return the appropriate
            // invalid response. Either ELEMENT_INVALID or CACHE_INVALID.
            return invalidStatus;
        }
        for (int i = 0; i < tableNames.length; i++) {
            if (hasBeenInvalidated(tableNames[i], element.lastModified())) {
                if (stillValid) {
                    // this check can occur mutliple times for the same element.
                    // set the stillValid flag so that we only log the event
                    // once.
                    if (logInvalidEvent) {
                        String msg = Message.msg("tablevalidate.invalidate", new Object[] {
                                description, element.getKey(), tableNames[i] });

                        logger.info(msg);
                    }
                }
                stillValid = false;
                // return either ELEMENT_INVALID or CACHE_INVALID
                // to invalidate either this element or the entire cache that it
                // is in.
                return invalidStatus;
            }
        }
        return Validator.ELEMENT_VALID;
    }

    /**
     * Test if the table has been modified.
     * 
     * @param tableName the table to test
     * @param sinceTime the time the element was cached
     */
    protected boolean hasBeenInvalidated(String tableName, long sinceTime) {

        TableState tableState = cacheAccess.getTableState(tableName);
        return tableState.isModified(sinceTime, isDependantOnInserts);

    }
}
