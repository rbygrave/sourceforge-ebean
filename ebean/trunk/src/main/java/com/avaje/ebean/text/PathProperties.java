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
package com.avaje.ebean.text;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.avaje.ebean.Query;

public class PathProperties {

    private final Map<String, Props> pathMap;

    private final Props rootProps;
    
    /**
     * Parse and return a PathProperties from nested string format like
     * (a,b,c(d,e),f(g)) where "c" is a path containing "d" and "e" and "f" is a
     * path containing "g" and the root path contains "a","b","c" and "f".
     */
    public static PathProperties parse(String source) {
        return PathPropertiesParser.parse(source);
    }
    
    /**
     * Construct an empty PathProperties.
     */
    public PathProperties() {
        this.rootProps = new Props(this, null, null);
        this.pathMap = new LinkedHashMap<String, Props>();
        this.pathMap.put(null, rootProps);
    }
    
    /**
     * Return true if there are no paths defined.
     */
    public boolean isEmpty() {
        return pathMap.isEmpty(); 
    }
    
    public String toString() {
        return pathMap.toString();
    }
    
    /**
     * Return true if the path is defined and has properties.
     */
    public boolean hasPath(String path){
        Props props = pathMap.get(path);
        return props != null && !props.isEmpty();
    }
    
    /**
     * Get the properties for a given path.
     */
    public Set<String> get(String path) {
        Props props = pathMap.get(path);
        return props == null ? null : props.getProperties();
    }

    /**
     * Set the properties for a given path.
     */
    public void put(String path, Set<String> properties){
        pathMap.put(path, new Props(this, null, path, properties));
    }

    /**
     * Remove a path returning the properties set for that path.
     */
    public Set<String> remove(String path){
        Props props = pathMap.remove(path);
        return props == null ? null : props.getProperties();
    }
    
    /**
     * Return a shallow copy of the paths.
     */
    public Set<String> getPaths() {
        return new LinkedHashSet<String>(pathMap.keySet());
    }
    
    /**
     * Apply these path properties as fetch paths to the query.
     */
    public void apply(Query<?> query) {
        
        for (Entry<String,Props> entry : pathMap.entrySet()) {
            String path = entry.getKey();
            String props = entry.getValue().getPropertiesAsString();
            
            if ("".equals(path)) {
                query.select(props);
            } else {
                query.fetch(path, props);
            }
        }
    }
    
    protected Props getRootProperties() {
        return rootProps;
    }
    
    protected static class Props {
        
        private final PathProperties owner;
        
        private final String parentPath;
        private final String path;
        
        private final Set<String> propSet;

        private Props(PathProperties owner, String parentPath, String path, Set<String> propSet) {
            this.owner = owner;
            this.path = path;
            this.parentPath = parentPath;
            this.propSet = propSet;
        }
        
        private Props(PathProperties owner, String parentPath, String path) {
            this(owner, parentPath, path, new LinkedHashSet<String>());
        }
        
        public String toString() {
            return propSet.toString();
        }
        
        protected boolean isEmpty() {
            return propSet.isEmpty();
        }
        
        /**
         * Return the properties for this property set.
         */
        protected Set<String> getProperties() {
            return propSet;
        }
        
        /**
         * Return the properties as a comma delimited string.
         */
        protected String getPropertiesAsString() {

            StringBuilder sb = new StringBuilder();
            
            Iterator<String> it = propSet.iterator();
            boolean hasNext = it.hasNext();
            while (hasNext) {
                sb.append(it.next());
                hasNext = it.hasNext();
                if (hasNext){
                    sb.append(",");
                }
            }
            return sb.toString();
        }
        
        /**
         * Return the parent path 
         */
        protected Props getParent() {
            return owner.pathMap.get(parentPath);
        }
        
        /**
         * Add a child Property set.
         */
        protected Props addChild(String subpath) {
            
            subpath = subpath.trim();
            addProperty(subpath);
            
            // build the subpath
            String p = path == null ? subpath : path+"."+subpath;
            Props nested = new Props(owner, path, p);
            owner.pathMap.put(p, nested);
            return nested;
        }
    
        /**
         * Add a properties to include for this path.
         */
        protected void addProperty(String property){
            propSet.add(property.trim());
        }
    }
    
}
