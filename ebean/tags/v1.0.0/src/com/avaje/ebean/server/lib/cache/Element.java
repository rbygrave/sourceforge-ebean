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

import com.avaje.ebean.server.lib.cache.Validator;


/**
 * Wraps data held in the Cache. 
 */
public class Element
{

    private int hitCount = 0;

    private Object data;

    private final Object key;
    
    private boolean isValid = true;

    private final long lastModified;

    private long lastAccess;

    private final Validator validator;
    
    
    /**
     * Create a new Element.
     */
    public Element(Object key, Object data, Validator validator){
        this.key = key;
        this.data = data;
        this.validator = validator;
        this.lastModified = System.currentTimeMillis();
        this.lastAccess = lastModified;
    }

    /**
     * Return the key used to put this element.
     */
    public Object getKey() {
        return key;
    }
    
    /**
     * Return the time this element was created.
     */
    public long lastModified() {
        return lastModified;
    }
    
    /**
     * Test to make sure the element is still valid.
     * Return one of ELEMENT_VALID, ELEMENT_INVALID or CACHE_INVALID. 
     */
    public int checkValid(){
        if (!isValid){
            return Validator.ELEMENT_INVALID;
        }
        if (validator == null) {
            return Validator.ELEMENT_VALID;            
        }
        int res = validator.isValid(this);
        if (res != Validator.ELEMENT_VALID){
            // remove access to the data... 
            this.data = null;
            this.isValid = false;
        }
        return res;
    }
    
    /**
     * The data cached.
     */
    public Object getData() {
        hitCount++;
        lastAccess = System.currentTimeMillis();
        return data;
    }
    
    /**
     * The number of times the data was got.
     */
    public int getHitCount() {
        return hitCount;
    }
    
    /**
     * The time the data was last read by a getData().
     */
    public long lastAccess() {
        return lastAccess;
    }
}
