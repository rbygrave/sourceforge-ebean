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
package com.avaje.ebean.jaxrs;

import java.util.ArrayList;
import java.util.List;

import com.avaje.ebean.Query;
import com.avaje.ebean.text.PathProperties;

/**
 * Used to parse a String (typically from part of a URL) into PathProperties,
 * List of Id's and sort clause which can then be applied to the JSON/XML
 * marshaling and ORM query to optimise the building and rendering of part of an
 * object graph.
 * <p>
 * The general purpose of UriOptions is to allow the caller to optimise the
 * content they want (the properties to fetch and render) rather than have the
 * server side develop lots of variations with associated extra server side
 * development, API and documentation.
 * </p>
 * <p>
 * UriOptions is broken into segments based on the prefix. Each segment is optional.
 * </p>
 * <p>
 * <div style="width: 70px; display: inline-block;"><b>&quot;::&quot;</b></div>
 * - a list of Id values<br/>
 * <div style="width: 70px; display: inline-block;"><b>&quot;:&quot;</b></div> -
 * nested properties to fetch and render<br/>
 * <div
 * style="width: 70px; display: inline-block;"><b>&quot;:sort&quot;</b></div> -
 * an order by clause to order the results<br/>
 * </p>
 * 
 * <pre class="code">
 * 
 * Example URL:
 * .../v1/customer::(34,35):(name,id,contacts(firstName,lastName)):sort(id desc)
 * 
 * The 3 segments above are broken into:
 * 
 * 
 * ::(34,35) 
 * // fetch customer 34 and 35
 * 
 * :(name,id,contacts(firstName,lastName))
 * // fetch customer name, id the customer contacts firstName and lastName
 *    
 * :sort(id desc)
 * // sort the customers by descending order of their Id values
 * 
 * </pre>
 * 
 * @author rbygrave
 * 
 */
public final class UriOptions {

    private static final List<String> EMPTY_IDS = new ArrayList<String>(0);
  
    private final String source;

    private PathProperties pathProperties;

    private List<String> idList = EMPTY_IDS;

    private String sort;

    private List<String> unknownSegments;

    private UriOptions(String source) {
        this.source = source;
    }

    /**
     * Parse the string of raw options.
     */
    public static UriOptions parse(String source) {
        return new UriOptions(source).parse();
    }

    /**
     * Return true if this has PathProperties defined.
     */
    public boolean hasPathProperties() {
        return pathProperties != null;
    }
    
    /**
     * Return true if there is no pathProperties, sort clause or Id List in these options.
     */
    public boolean isEmpty() {
        if (pathProperties != null){
            return false;
        }
        if (sort != null){
            return false;
        }
        if (idList != null){
            return false;
        }
        return true;
    }
    
    /**
     * Apply any options such as PathProperties, sort order or Id list.
     * 
     * @param query
     *            the query to apply the options to
     * @return true if the query was modified with at least one of the options.
     */
    public boolean apply(Query<?> query) {

        boolean changed = false;
        if (pathProperties != null) {
            pathProperties.apply(query);
            changed = true;
        }
        if (idList != null) {
            query.where().idIn(idList);
            changed = true;
        }
        if (sort != null) {
            query.order(sort);
            changed = true;
        }

        return changed;
    }

    /**
     * Return the pathProperties. This is a tree like structure defining the
     * properties per path in an object graph.
     * <p>
     * This can be applied to the renderer (JSON/XML) to define what is included
     * in the rendered output, and also applied to the ORM query to define what
     * should be fetched.
     * </p>
     */
    public PathProperties getPathProperties() {
        return pathProperties;
    }

    /**
     * Return the list of Id's in string form.
     */
    public List<String> getIdList() {
        return idList;
    }

    /**
     * Return the sort clause.
     */
    public String getSort() {
        return sort;
    }

    /**
     * Return any segments that where not either Id's, pathProperties or sort.
     */
    public List<String> getUnknownSegments() {
        return unknownSegments;
    }

    private UriOptions parse() {
        
        if (source == null || source.length() == 0){
            return this;
        }
        
        String[] opts = SplitUriOptions.split(source);

        String segment;
        for (int i = 0; i < opts.length; i++) {
            segment = opts[i];
            char firstChar = segment.charAt(0);
            if (firstChar == ':') {
                parseIds(segment);
            } else if (firstChar == '(') {
                parsePathProperties(segment);
            } else if (segment.startsWith("sort(")) {
                parseSort(segment);
            } else {
                unknownSegment(segment);
            }
        }
        return this;
    }

    private void unknownSegment(String segment) {
        if (unknownSegments == null) {
            unknownSegments = new ArrayList<String>(4);
        }
        unknownSegments.add(segment);
    }

    private void parseSort(String segment) {
        if (!segment.endsWith(")")) {
            throw new IllegalArgumentException("Expecting sort segment to end with ')'");
        }
        sort = segment.substring(5, segment.length() - 1);
    }

    private void parsePathProperties(String segment) {
        if (!segment.endsWith(")")) {
            throw new IllegalArgumentException("Expecting PathProperties segment to end with ')'");
        }
        pathProperties = PathProperties.parse(segment);
    }

    private void parseIds(String segment) {
        if (!segment.startsWith(":(")) {
            throw new IllegalArgumentException("Expecting ID segment to start with ':('");
        }
        if (!segment.endsWith(")")) {
            throw new IllegalArgumentException("Expecting ID segment to end with ')'");
        }
        String rawIds = segment.substring(2, segment.length() - 1);
        String[] ids = rawIds.split(",");
        idList = new ArrayList<String>(ids.length);
        for (int i = 0; i < ids.length; i++) {
            idList.add(ids[i]);
        }
    }
}
