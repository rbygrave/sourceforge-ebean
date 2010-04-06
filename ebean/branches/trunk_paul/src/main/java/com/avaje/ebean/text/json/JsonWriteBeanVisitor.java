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

import java.util.Set;

/**
 * Allows for customising the JSON write processing.
 * <p>
 * You can use this to add raw JSON content via {@link JsonWriter}. You can also
 * limit the properties included in the JSON content (rather than just the
 * loaded properties).
 * </p>
 * 
 * @author rbygrave
 * 
 * @param <T>
 *            the type of entity bean
 */
public interface JsonWriteBeanVisitor<T> {

    /**
     * Return the set of properties to write to JSON. If null is returned then
     * the default will output the properties loaded for this bean.
     */
    public Set<String> getIncludeProperties();

    /**
     * Visit the bean that has just been writing it's content to JSON. You can
     * write your own additional JSON content to the JsonWriter if you wish.
     * 
     * @param bean
     *            the bean that has been writing it's content
     * @param jsonWriter
     *            the JsonWriter which you can append custom json content to if
     *            you wish.
     */
    public void visit(T bean, JsonWriter jsonWriter);

}
