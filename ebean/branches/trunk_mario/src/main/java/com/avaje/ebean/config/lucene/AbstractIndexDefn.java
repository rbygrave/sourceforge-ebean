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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Fieldable;

/**
 * Provides default implementations for IndexDefn settings.
 * 
 * @author rbygrave
 *
 * @param <T> The bean type the index is on
 */
public abstract class AbstractIndexDefn<T> implements IndexDefn<T> {

    public Analyzer getAnalyzer() {
        // Just use System wide default
        return null;
    }

    public int getMaxBufferedDocs() {
        // Use system default
        return 0;
    }

//    public MaxFieldLength getMaxFieldLength() {
//        return MaxFieldLength.UNLIMITED;
//    }

    public double getRAMBufferSizeMB() {
        // Use system default
        return 0;
    }

    public int getTermIndexInterval() {
        // Use system default
        return 0;
    }

    public void visitCreatedField(Fieldable field) {
        // don't need to tweak any of the fields 
        // that where created
    }

}
