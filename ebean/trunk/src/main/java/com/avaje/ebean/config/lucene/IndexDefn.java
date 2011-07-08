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
package com.avaje.ebean.config.lucene;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;

/**
 * Defines a Lucene Index for a given entity type.
 * 
 * @author rbygrave
 * 
 * @param <T>
 *            The type of the entity to build the index on.
 */
public interface IndexDefn<T> {

    /**
     * Initialise the index defining the fields on the index.
     * <p>
     * The IndexDefnBuilder can be used to make defining fields a little easier.
     * </p>
     * 
     * @param helper
     *            to help create index field definitions
     */
    public void initialise(IndexDefnBuilder helper);

    /**
     * Return the name of the default query field.
     */
    public String getDefaultField();

    /**
     * Return the index fields.
     */
    public List<IndexFieldDefn> getFields();

    /**
     * Return true if incremental index updates are supported via 'last updated
     * timestamp' properties(s).
     */
    public boolean isUpdateSinceSupported();

    /**
     * Return the properties that are the 'last updated timestamp'
     * properties(s).
     */
    public String[] getUpdateSinceProperties();

    /**
     * Return the default Analyzer to use for this index.
     */
    public Analyzer getAnalyzer();

    /**
     * Return the max buffered documents for this index.
     */
    public int getMaxBufferedDocs();

    /**
     * Return the ram buffer size for this index.
     */
    public double getRAMBufferSizeMB();

    /**
     * Return the term index interval.
     */
    public int getTermIndexInterval();

    // Similarity
    // useCompoundFile
    // MergePolicy
}
