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
package com.avaje.ebeaninternal.server.core;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

public class LuceneOrmQueryRequest {

    private final Query luceneQuery;
    
    private final Sort luceneSort;
 
    public LuceneOrmQueryRequest(Query luceneQuery, Sort luceneSort) {
        this.luceneQuery = luceneQuery;
        this.luceneSort = luceneSort;
    }

    public Query getLuceneQuery() {
        return luceneQuery;
    }

    public Sort getLuceneSort() {
        return luceneSort;
    }

}
