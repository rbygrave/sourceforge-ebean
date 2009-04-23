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

import com.avaje.ebean.server.lib.cache.Element;

/**
 * Validates cached Elements.
 * <p>
 * Can be assigned to a Cache or individual elements within a cache.
 * </p>
 * <p>
 * Before an Element is returned from the cache it is checked to make sure that
 * it is valid. This interface enables the developer to specify when to
 * invalidate an Element or the entire cache.
 * </p>
 */
public interface Validator {

    /**
     * The element is valid.
     */
    public static final int ELEMENT_VALID = 0;

    /**
     * Just this element of the cache is invalid. It should not be returned but
     * rather removed from the cache.
     */
    public static final int ELEMENT_INVALID = 1;

    /**
     * The entire cache this element is in should be considered invalid and be
     * cleared.
     */
    public static final int CACHE_INVALID = 2;

    /**
     * Test that an element is valid returning one of ELEMENT_VALID,
     * ELEMENT_INVALID or CACHE_INVALID.
     */
    public int isValid(Element element);

}
