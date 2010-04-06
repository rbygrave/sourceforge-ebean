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
package com.avaje.ebean.text.json;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides options for customising the JSON write process.
 * <p>
 * You can optionally provide a custom JsonValueAdapter to handle specific
 * formatting for Date and DateTime types.
 * </p>
 * <p>
 * You can optionally register JsonWriteBeanVisitors to customise the processing
 * of the beans as they are processed and <strong>add raw JSON elements</strong>.
 * </p>
 * 
 * @see JsonContext#toList(Class, String, JsonReadOptions)
 * 
 * @author rbygrave
 *
 */
public class JsonWriteOptions {
    
    protected JsonValueAdapter valueAdapter;

    protected Map<String, JsonWriteBeanVisitor<?>> visitorMap = new HashMap<String, JsonWriteBeanVisitor<?>>();

    /**
     * Return the JsonValueAdapter.
     */
    public JsonValueAdapter getValueAdapter() {
        return valueAdapter;
    }

    /**
     * Set a JsonValueAdapter for custom DateTime and Date formatting.
     */
    public JsonWriteOptions setValueAdapter(JsonValueAdapter valueAdapter) {
        this.valueAdapter = valueAdapter;
        return this;
    }

    /**
     * Register a JsonWriteBeanVisitor for the root level.
     */
    public JsonWriteOptions addRootVisitor(JsonWriteBeanVisitor<?> visitor) {
        return addVisitor(null, visitor);
    }
    
    /**
     * Register a JsonWriteBeanVisitor for the given path.
     */
    public JsonWriteOptions addVisitor(String path, JsonWriteBeanVisitor<?> visitor) {
        visitorMap.put(path, visitor);
        return this;
    }

    /**
     * Return the Map of registered JsonWriteBeanVisitor's by path.
     */
    public Map<String, JsonWriteBeanVisitor<?>> getVisitorMap() {
        return visitorMap;
    }
 
}
