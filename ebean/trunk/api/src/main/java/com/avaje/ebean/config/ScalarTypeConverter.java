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
package com.avaje.ebean.config;

/**
 * Used to convert between a value object and a known scalar type.
 * <p>
 * The Value object should be immutable and scalar (aka not compound) and
 * converts to and from a known scalar type which Ebean will use to persist the
 * value.
 * </p>
 * 
 * @author rbygrave
 * 
 * @param <B>
 *            The value object type.
 * @param <S>
 *            The scalar object type that is used to persist the value object.
 */
public interface ScalarTypeConverter<B, S> {

    /**
     * Convert the scalar type value into the value object.
     * <p>
     * This typically occurs when Ebean reads the value from a resultSet or
     * other data source.
     * </p>
     * 
     * @param scalarType
     *            the value from the data source
     */
    public B wrapValue(S scalarType);

    /**
     * Convert the value object into a scalar value that Ebean knows how to
     * persist.
     * <p>
     * This typically occurs when Ebean is persisting the value object to the
     * data store.
     * </p>
     * 
     * @param beanType
     *            the value object
     */
    public S unwrapValue(B beanType);

}
