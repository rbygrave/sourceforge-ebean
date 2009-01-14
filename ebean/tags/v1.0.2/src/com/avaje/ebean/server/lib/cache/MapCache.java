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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.server.lib.cache.Cache;
import com.avaje.ebean.server.lib.cache.Element;
import com.avaje.ebean.server.lib.cache.MapCache;
import com.avaje.ebean.server.lib.cache.Validator;
import com.avaje.lib.log.LogFactory;

/**
 * The implementation of Cache based on Doug Lea's ConcurrentHashMap.
 */
public class MapCache implements Cache {

	private static final Logger logger = LogFactory.get(MapCache.class);
	
    /**
     * Cache level validator.
     */
    private Validator defaultValid;
    
    /**
     * Alternate validator.
     */
    private Validator altValid;

    /**
     * The underlying map.
     */
    private Map<Object,Element> map;

    /**
     * The cache name.
     */
    private String name;

    /**
     * The timeout seconds used to remove old elements.
     */
    private int timeoutSeconds = 0;

    
    /**
     * Create the cache.
     */
    public MapCache() {
        this.map = new ConcurrentHashMap<Object, Element>();
    }

    /**
     * Set the name of the cache.
     */
    public void setName(String cacheName){
        this.name = cacheName;
    }
    
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public int size(){
        return map.size();
    }
    
    public String getName() {
        return name;
    }

    public Element getElement(Object key) {

        Element e = (Element) map.get(key);
        if (e == null) {
            return null;
        }

        int status = e.checkValid();
        if (status == Validator.ELEMENT_VALID) {
            return e;
        }
        // don't bother to remove element from Map
        // as likely to replaced by a put(
        if (status == Validator.ELEMENT_INVALID) {
        	if (logger.isLoggable(Level.FINER)){
            	logger.finer("Cache[" + getName() + "] element invalid on key[" + e.getKey()+ "]");
            }
        }
        if (status == Validator.CACHE_INVALID) {
        	if (logger.isLoggable(Level.FINER)){
                logger.finer("Cache[" + getName() + "] clear() due to invalid on key["+ e.getKey() + "]");
            }
            clear();
        }

        return null;
    }

    /**
     * Clear the cache of all its elements. That is, flush the cache.
     */
    public void clear() {
        map.clear();
    }

    public Iterator<Element> elements() {
        return map.values().iterator();
    }

    /**
     * Return the cached data for a given key.
     */
    public Object get(Object key) {
        Element e = getElement(key);
        if (e != null) {
            return e.getData();
        } else {
            return null;
        }
    }

    public void remove(Object key) {
        map.remove(key);
    }

    /**
     * Add an entry with the default validator.
     */
    public Element put(Object key, Object value) {
        return put(key, value, defaultValid);
    }

    /**
     * Add an entry with the alternate validator.
     */
    public Element putUsingAltValid(Object key, Object value) {
        return put(key, value, altValid);
    }
    
    /**
     * Add all the entries from the map into the cache.
     */
    public void putAll(Map<?,?> map) {
        Iterator<?> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?,?> entry = (Map.Entry<?,?>) it.next();
            put(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Add an entry with an explicit validator.
     */
    public Element put(Object key, Object value, Validator validator) {
        Element e = new Element(key, value, validator);
        map.put(key, e);
        return e;
    }

    public boolean hasValidator() {
        return (defaultValid != null);
    }
    
    public Validator getValidator() {
        return defaultValid;
    }

    /**
     * Set the default Validator. This is set to any entry added without an
     * explicit validator.
     */
    public void setValidator(Validator defaultValid, Validator altValid) {
        this.defaultValid  = defaultValid;
        this.altValid = altValid;
    }
}
