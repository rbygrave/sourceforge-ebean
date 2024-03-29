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
import java.util.Set;

/**
 * JSON Object element.
 * <p>
 * You will only use the JsonElements when you register a JsonReadBeanVisitor.
 * The JSON elements that are not mapped to a bean property are made available
 * to the JsonReadBeanVisitor.
 * </p>
 * 
 * @see JsonReadBeanVisitor
 * 
 * @author rbygrave
 */
public class JsonElementObject implements JsonElement {

    private final Map<String,JsonElement> map = new LinkedHashMap<String, JsonElement>();
    
    public void put(String key, JsonElement value){
        map.put(key, value);
    }

    public JsonElement get(String key){
        return map.get(key);
    }

    public JsonElement getValue(String key){
        return map.get(key);
    }
    
    public Set<String> keySet() {
        return map.keySet();
    }

    public Set<Map.Entry<String, JsonElement>> entrySet() {
        return map.entrySet();
    }
    
    public String toString() {
        return map.toString();
    }

    public boolean isPrimitive() {
        return false;
    }

    public String toPrimitiveString() {
        return null;
    }
    
    
}
