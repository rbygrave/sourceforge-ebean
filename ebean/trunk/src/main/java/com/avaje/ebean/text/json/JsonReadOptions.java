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

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonReadOptions {
    
    protected JsonValueAdapter valueAdapter;

    protected Map<String, JsonReadBeanVisitor<?>> visitorMap;
    
    public JsonReadOptions(){
        this.visitorMap = new LinkedHashMap<String, JsonReadBeanVisitor<?>>();
    }
    
    public JsonReadOptions(JsonValueAdapter valueAdapter, Map<String, JsonReadBeanVisitor<?>> visitorMap) {
        this.valueAdapter = valueAdapter;
        this.visitorMap = visitorMap;
    }
    
    public JsonValueAdapter getValueAdapter() {
        return valueAdapter;
    }

    public Map<String, JsonReadBeanVisitor<?>> getVisitorMap() {
        return visitorMap;
    }
 
    public JsonReadOptions setValueAdapter(JsonValueAdapter valueAdapter) {
        this.valueAdapter = valueAdapter;
        return this;
    }

    public JsonReadOptions addRootVisitor(JsonReadBeanVisitor<?> visitor) {
        return addVisitor(null, visitor);
    }
    
    public JsonReadOptions addVisitor(String path, JsonReadBeanVisitor<?> visitor) {
        visitorMap.put(path, visitor);
        return this;
    }
 
    
}
