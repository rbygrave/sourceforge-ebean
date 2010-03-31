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

import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 * Converts objects to and from JSON format.
 * 
 * @author rbygrave
 */
public interface JsonContext {

    /**
     * Convert json string input into a Bean of a specific type. 
     */
    public <T> T toBean(Class<T> rootType, String json);

    /**
     * Convert json reader input into a Bean of a specific type. 
     */
    public <T> T toBean(Class<T> rootType, Reader json);

    /**
     * Convert json string input into a Bean of a specific type with options. 
     */
    public <T> T toBean(Class<T> rootType, String json, JsonReadOptions options);

    /**
     * Convert json reader input into a Bean of a specific type with options. 
     */
    public <T> T toBean(Class<T> rootType, Reader json, JsonReadOptions options);

    /**
     * Convert json string input into a list of beans of a specific type. 
     */
    public <T> List<T> toList(Class<T> rootType, String json);

    /**
     * Convert json string input into a list of beans of a specific type with options. 
     */
    public <T> List<T> toList(Class<T> rootType, String json, JsonReadOptions options);

    /**
     * Convert json reader input into a list of beans of a specific type. 
     */
    public <T> List<T> toList(Class<T> rootType, Reader json);

    /**
     * Convert json reader input into a list of beans of a specific type with options. 
     */
    public <T> List<T> toList(Class<T> rootType, Reader json, JsonReadOptions options);

    /**
     * Write the bean or collection in JSON format to the writer with default
     * options.
     * 
     * @param o
     *            the bean or collection of beans to write
     * @param writer
     *            used to write the json output to
     */
    public void toJsonWriter(Object o, Writer writer);

    /**
     * With additional pretty output option.
     */
    public void toJsonWriter(Object o, Writer writer, boolean pretty);

    /**
     * With additional options to specify JsonValueAdapter and JsonWriteBeanVisitor's.
     * 
     * @param o
     *            the bean or collection of beans to write
     * @param writer
     *            used to write the json output to
     * @param options
     *            additional options to control the JSON output
     */
    public void toJsonWriter(Object o, Writer writer, boolean pretty, JsonWriteOptions options);

    /**
     * Convert a bean or collection to json string using default options. 
     */
    public String toJsonString(Object o);

    /**
     * Convert a bean or collection to json string with pretty format using default options. 
     */
    public String toJsonString(Object o, boolean pretty);

    /**
     * Convert a bean or collection to json string using options. 
     */
    public String toJsonString(Object o, boolean pretty, JsonWriteOptions options);


}