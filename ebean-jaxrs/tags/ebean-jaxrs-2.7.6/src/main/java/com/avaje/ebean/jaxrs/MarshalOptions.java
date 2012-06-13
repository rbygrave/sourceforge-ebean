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

import com.avaje.ebean.text.PathProperties;
import com.avaje.ebean.text.json.JsonWriteOptions;

/**
 * Options for controlling the Marshaling of JSON and XML content.
 * <p>
 * This uses a ThreadLocal to pass the options from your resource code to the
 * JSON or XML Marshalling provider.
 * </p>
 * <p>
 * So your code in the JAX-RS resource sets the options to use and the provider
 * removes them and then uses them to control the marshalling.
 * </p>
 * 
 * @author rbygrave
 */
public final class MarshalOptions {

    private static ThreadLocal<Holder> local = new ThreadLocal<Holder>() {
        protected synchronized Holder initialValue() {
            return new Holder();
        }
    };

    /**
     * Not allowed.
     */
    private MarshalOptions() {
    }

    /**
     * Typically called by the provider to get the JsonWriteOptions to use (if any).
     */
    public static JsonWriteOptions removeJsonWriteOptions() {
        return local.get().removeJsonWriteOptions();
    }

    /**
     * Set the JsonWriteOptions to use when rendering the object with the JSON provider.
     */
    public static void setJsonWriteOptions(JsonWriteOptions writeOptions) {
        local.get().setJsonWriteOptions(writeOptions);
    }

    /**
     * Typically called by the provider to get the PathProperties to use (if any).
     */
    public static PathProperties removePathProperties() {
        return local.get().removePathProperties();
    }

    /**
     * Set the PathProperties to use when rendering the object.
     * <p>
     * If the PathProperties are set on a JsonWriteOptions then they will be
     * used, otherwise these PathProperties are merged with any JsonWriteOptions that
     * have been set.
     * </p>
     * <p>
     * The reason for 'merging' PathProperties and JsonWriteOptions is that it is 
     * expected that often a JsonWriteOptions will contain JsonWriteBeanVisitors and 
     * PathProperties might come from the UriOptions for the specific request.
     * </p>
     */
    public static void setPathProperties(PathProperties pathProperties) {
        local.get().setPathProperties(pathProperties);
    }

    private static class Holder {

        private PathProperties pathProperties;
        
        private JsonWriteOptions writeOptions;

        private void setPathProperties(PathProperties pathProperties) {
            this.pathProperties = pathProperties;
        }

        private PathProperties removePathProperties() {
            PathProperties p = pathProperties;
            pathProperties = null;
            return p;
        }

        private JsonWriteOptions removeJsonWriteOptions() {
            JsonWriteOptions o = writeOptions;
            writeOptions = null;
            return o;
        }

        private void setJsonWriteOptions(JsonWriteOptions writeOptions) {
            this.writeOptions = writeOptions;
        }

    }
}
