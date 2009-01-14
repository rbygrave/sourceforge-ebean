/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.avaje.ebean.server.lib.cache;

import java.util.Iterator;
import java.util.Map;

import com.avaje.ebean.server.lib.cache.Element;
import com.avaje.ebean.server.lib.cache.Validator;

/**
 * Holds a map of entries of a common type.
 */
public interface Cache {

    /**
     * Return the name of the cache.
     */
    public String getName();

    /**
     * Set the name of the cache.
     */
    public void setName(String cacheName);
    
    /**
     * The current number of elements cached.
     */
    public int size();

    /**
     * Return the Elements cached.
     */
    public Iterator<Element> elements();

    /**
     * Set the number of seconds unused content is allowed to stay in the cache.
     * If content is unused for longer than this period then it will be removed.
     */
    public void setTimeoutSeconds(int timeoutSeconds);

    /**
     * return the time in seconds unused content is allowed to stay in the
     * cache. 
     */
    public int getTimeoutSeconds();

    /**
     * Set the default validator.
     */
    public void setValidator(Validator defaultValid, Validator altValid);

    /**
     * Return the default validator.
     */
    public Validator getValidator();

    /**
     * Return true if a validator has been set on this cache.
     */
    public boolean hasValidator();
    
    /**
     * Get an entry from the cache. If not entry is found null is returned.
     */
    public Object get(Object key);
    
    /**
     * Return the Element that holds the cached data.
     */
    public Element getElement(Object key);

    /**
     * Put an entry into the cache with the default validator.
     */
    public Element put(Object key, Object value);

    /**
     * Put an entry into the cache with the alternate validator.
     */
    public Element putUsingAltValid(Object key, Object value);

    /**
     * Add all the entries from the map into the cache.
     */
    public void putAll(Map<?,?> map);
    
    /**
     * Additionally specify a validator for this entry.
     */
    public Element put(Object key, Object value, Validator validator);

    /**
     * Remove an entry from the cache.
     */
    public void remove(Object key);

}
