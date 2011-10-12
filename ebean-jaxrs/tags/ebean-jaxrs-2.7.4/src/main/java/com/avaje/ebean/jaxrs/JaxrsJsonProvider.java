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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.common.BeanList;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.text.PathProperties;
import com.avaje.ebean.text.json.JsonContext;
import com.avaje.ebean.text.json.JsonWriteOptions;

/**
 * A JAX-RS provider for JSON Marshalling and Unmarshalling.
 * <p>
 * This provider is aware of Ebean ORM issues such as partial objects etc and
 * can handle the extra options for customising the JSON output via
 * MarshalOptions, JsonWriteRequest and JsonWriteOptions.
 * </p>
 * 
 * @author rbygrave
 * 
 */
@Provider
@Consumes( { MediaType.APPLICATION_JSON, "text/json" })
@Produces( { MediaType.APPLICATION_JSON, "text/json" })
public class JaxrsJsonProvider implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

    protected final JsonContext jsonContext;

    protected final boolean defaultPretty;

    /**
     * Construct with a specific JsonContext.
     */
    public JaxrsJsonProvider(JsonContext jsonContext, boolean pretty) {
        this.jsonContext = jsonContext;
        this.defaultPretty = pretty;
    }

    /**
     * Construct using the default JsonContext.
     */
    public JaxrsJsonProvider() {
        this.defaultPretty = GlobalProperties.getBoolean("ebean.json.pretty", true);
        this.jsonContext = Ebean.createJsonContext();
    }

    public long getSize(Object o, Class<?> type, Type genericType, Annotation[] anns, MediaType mediaType) {
        return -1;
    }

    /**
     * Return true if this type can be processed by this provider. 
     */
    protected boolean isReadWriteable(Class<?> type, Type genericType, MediaType mediaType) {

        if (isJsonMediaType(mediaType)) {

            if (type.equals(BeanList.class)) {
                return true;
            }

            return jsonContext.isSupportedType(genericType);
        }
        return false;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mediaType) {

        return isReadWriteable(type, genericType, mediaType);
    }

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] anns, MediaType mediaType) {

        return isReadWriteable(type, genericType, mediaType);
    }

    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] anns, MediaType mediaType,
            MultivaluedMap<String, Object> valueMap, OutputStream os) throws IOException, WebApplicationException {

        Writer writer = new BufferedWriter(new OutputStreamWriter(os));

        // Check MarshalOptions ...
        JsonWriteOptions options = MarshalOptions.removeJsonWriteOptions();
        PathProperties pathProperties = MarshalOptions.removePathProperties();
        
        if (options == null) {
            if (pathProperties != null) {
                // only PathProperties have been set
                options = new JsonWriteOptions();
                options.setPathProperties(pathProperties);
            }
        } else {
            if (options.getPathProperties() == null && pathProperties != null) {
                // merge the PathProperties and JsonWriteOptions that were
                // both set independently into MarshalOptions

                // create a copy of the original JsonWriteOptions
                options = options.copy();
                // merge in the PathProperties that were set via
                // MarshalOptions
                options.setPathProperties(pathProperties);
            }
        }

        jsonContext.toJsonWriter(o, writer, defaultPretty, options, null);
        writer.flush();
    }

    public Object readFrom(Class<Object> type, Type genericType, Annotation[] anns, MediaType mediaType,
            MultivaluedMap<String, String> valueMap, InputStream is) throws IOException, WebApplicationException {

        Reader reader = new BufferedReader(new InputStreamReader(is));
        return jsonContext.toObject(genericType, reader, null);
    }

    /**
     * Return true if this is a JSON media type.
     */
    protected boolean isJsonMediaType(MediaType mediaType) {

        if (mediaType != null) {
            String subtype = mediaType.getSubtype();
            return subtype.indexOf("json") > -1;
        } else {
            return true;
        }
    }

}
