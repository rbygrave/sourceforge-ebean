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

import com.avaje.ebean.text.json.JsonContext;
import com.avaje.ebean.text.json.JsonWriteOptions;

/**
 * Provides the ability to control the JSON output with explicit
 * JsonWriteOptions and JsonContext.
 * <p>
 * You can return this from a resource method for more explicit control over how
 * the object is rendered.
 * </p>
 * 
 * @author rbygrave
 * 
 */
public class JsonWriteRequest {

    final Object object;

    final JsonWriteOptions options;

    final JsonContext jsonContext;

    final Boolean pretty;

    public JsonWriteRequest(Object object, JsonWriteOptions options, Boolean pretty, JsonContext jsonContext) {
        this.object = object;
        this.options = options;
        this.pretty = pretty;
        this.jsonContext = jsonContext;
    }

    /**
     * Return the bean or list of beans to render as JSON.
     */
    public Object getObject() {
        return object;
    }

    /**
     * Return the JsonWriteOptions to use.
     */
    public JsonWriteOptions getOptions() {
        return options;
    }

    /**
     * Return the JsonContext to use (otherwise use the default).
     */
    public JsonContext getJsonContext() {
        return jsonContext;
    }

    /**
     * Return 
     * @return
     */
    public Boolean getPretty() {
        return pretty;
    }

}
