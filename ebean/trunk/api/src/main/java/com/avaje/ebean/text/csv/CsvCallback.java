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
package com.avaje.ebean.text.csv;

/**
 * Provides a callback for more custom control over CSV processing.
 * <p>
 * Allows the developer to further process the bean
 * </p>
 * 
 * @author rbygrave
 * 
 * @param <T>
 */
public interface CsvCallback<T> {

    /**
     * Called for each bean after it has been loaded from the CSV content.
     * <p>
     * This allows you to process the bean however you like.
     * </p>
     * <p>
     * When you use a CsvCallback the CsvReader *WILL NOT* create a transaction
     * and will not save the bean for you. You have complete control and must do
     * these things yourself (if that is want you want).
     * </p>
     * 
     * @param row
     *            the index of the content being processed
     * @param bean
     *            the entity bean after it has been loaded from the csv content
     * @param lineContent
     *            the content that has been used to load the bean
     */
    public void processBean(int row, T bean, String[] lineContent);
}
