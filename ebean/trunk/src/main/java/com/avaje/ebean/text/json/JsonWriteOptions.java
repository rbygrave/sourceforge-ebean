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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides options for customising the JSON write process.
 * <p>
 * You can optionally provide a custom JsonValueAdapter to handle specific
 * formatting for Date and DateTime types.
 * </p>
 * <p>
 * You can optionally register JsonWriteBeanVisitors to customise the processing
 * of the beans as they are processed and <strong>add raw JSON
 * elements</strong>.
 * </p>
 * <p>
 * You can explicitly state which properties to include in the JSON output for
 * the root level and each path.
 * </p>
 * 
 * <pre class="code">
 *  // find some customers ...
 *  
 * List&lt;Customer&gt; list = Ebean.find(Customer.class)
 *     .select(&quot;id, name, status, shippingAddress&quot;)
 *     .fetch(&quot;billingAddress&quot;,&quot;line1, city&quot;)
 *     .fetch(&quot;billingAddress.country&quot;, &quot;*&quot;)
 *     .fetch(&quot;contacts&quot;, &quot;firstName,email&quot;)
 *     .order().desc(&quot;id&quot;)
 *     .findList();
 * 
 * JsonContext json = Ebean.createJsonContext();
 * 
 * JsonWriteOptions writeOptions = new JsonWriteOptions();
 * writeOptions.setRootPathVisitor(new JsonWriteBeanVisitor&lt;Customer&gt;() {
 * 
 *     public void visit(Customer bean, JsonWriter ctx) {
 *         System.out.println(&quot;write visit customer: &quot; + bean);
 *         ctx.appendKeyValue(&quot;dummyCust&quot;, &quot;34&quot;);
 *         ctx.appendKeyValue(&quot;smallCustObject&quot;, &quot;{\&quot;a\&quot;:34,\&quot;b\&quot;:\&quot;asdasdasd\&quot;}&quot;);
 *     }
 * });
 * 
 * writeOptions.setPathProperties(&quot;contacts&quot;, &quot;firstName,id&quot;);
 * writeOptions.setPathVisitor(&quot;contacts&quot;, new JsonWriteBeanVisitor&lt;Contact&gt;() {
 * 
 *     public void visit(Contact bean, JsonWriter ctx) {
 *         System.out.println(&quot;write additional custom json on customer: &quot; + bean);
 *         ctx.appendKeyValue(&quot;dummy&quot;, &quot;  3400&quot; + bean.getId() + &quot;&quot;);
 *         ctx.appendKeyValue(&quot;smallObject&quot;, &quot;{\&quot;contactA\&quot;:34,\&quot;contactB\&quot;:\&quot;banana\&quot;}&quot;);
 *     }
 * 
 * });
 * 
 *  // output as a JSON string with pretty formatting
 * String s = json.toJsonString(list, true, writeOptions);
 * 
 * </pre>
 * 
 * @see JsonContext#toList(Class, String, JsonReadOptions)
 * 
 * @author rbygrave
 * 
 */
public class JsonWriteOptions {

    protected String callback;
    
    protected JsonValueAdapter valueAdapter;

    protected Map<String, JsonWriteBeanVisitor<?>> visitorMap = new HashMap<String, JsonWriteBeanVisitor<?>>();

    protected Map<String, Set<String>> includePropertiesMap = new HashMap<String, Set<String>>();

    /**
     * Return a JSONP callback function.
     */
    public String getCallback() {
        return callback;
    }

    /**
     * Set a JSONP callback function.
     */
    public JsonWriteOptions setCallback(String callback) {
        this.callback = callback;
        return this;
    }

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
    public JsonWriteOptions setRootPathVisitor(JsonWriteBeanVisitor<?> visitor) {
        return setPathVisitor(null, visitor);
    }

    /**
     * Register a JsonWriteBeanVisitor for the given path.
     */
    public JsonWriteOptions setPathVisitor(String path, JsonWriteBeanVisitor<?> visitor) {
        visitorMap.put(path, visitor);
        return this;
    }

    /**
     * Set the properties to include in the JSON output for the given path.
     * 
     * @param propertiesToInclude
     *            The set of properties to output
     */
    public JsonWriteOptions setPathProperties(String path, Set<String> propertiesToInclude) {
        includePropertiesMap.put(path, propertiesToInclude);
        return this;
    }

    /**
     * Set the properties to include in the JSON output for the given path.
     * 
     * @param propertiesToInclude
     *            Comma delimited list of properties to output
     */
    public JsonWriteOptions setPathProperties(String path, String propertiesToInclude) {
        return setPathProperties(path, parseProps(propertiesToInclude));
    }

    /**
     * Set the properties to include in the JSON output for the root level.
     * 
     * @param propertiesToInclude
     *            Comma delimited list of properties to output
     */
    public JsonWriteOptions setRootPathProperties(String propertiesToInclude) {
        return setPathProperties(null, parseProps(propertiesToInclude));
    }

    /**
     * Set the properties to include in the JSON output for the root level.
     * 
     * @param propertiesToInclude
     *            The set of properties to output
     */
    public JsonWriteOptions setRootPathProperties(Set<String> propertiesToInclude) {
        return setPathProperties(null, propertiesToInclude);
    }

    private Set<String> parseProps(String propertiesToInclude) {

        LinkedHashSet<String> props = new LinkedHashSet<String>();

        String[] split = propertiesToInclude.split(",");
        for (int i = 0; i < split.length; i++) {
            String s = split[i].trim();
            if (s.length() > 0) {
                props.add(s);
            }
        }
        return props;
    }

    /**
     * Return the Map of registered JsonWriteBeanVisitor's by path.
     */
    public Map<String, JsonWriteBeanVisitor<?>> getVisitorMap() {
        return visitorMap;
    }

    /**
     * Return the Map of properties to include by path.
     */
    public Map<String, Set<String>> getIncludePropertiesMap() {
        return includePropertiesMap;
    }

}
