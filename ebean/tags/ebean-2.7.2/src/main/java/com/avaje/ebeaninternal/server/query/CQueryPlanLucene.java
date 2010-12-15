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
package com.avaje.ebeaninternal.server.query;

import java.sql.ResultSet;

import com.avaje.ebeaninternal.server.core.LuceneOrmQueryRequest;
import com.avaje.ebeaninternal.server.core.OrmQueryRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.type.DataReader;

public class CQueryPlanLucene extends CQueryPlan {

    private final OrmQueryRequest<?> request;
    
    public CQueryPlanLucene(OrmQueryRequest<?> request, SqlTree sqlTree) {
        super(request, null, sqlTree, false, "", getLuceneDescription(request)); 
        this.request = request;
    }

    @Override
    public boolean isLucene() {
        return true;
    }

    @Override
    public DataReader createDataReader(ResultSet rset) {
        return new LuceneIndexDataReader(request);
    }
    
    private static String getLuceneDescription(OrmQueryRequest<?> request) {
        
        LuceneOrmQueryRequest req = request.getLuceneOrmQueryRequest();
        String description = req.getDescription();
        String sortDesc = req.getSortDesc();
        BeanDescriptor<?> beanDescriptor = request.getBeanDescriptor();
        
        StringBuilder sb = new StringBuilder();
        sb.append("lucene query from ").append(beanDescriptor.getName());
        sb.append(" ").append(description);
        if (sortDesc != null && sortDesc.length() > 0) {
            sb.append(" order by ").append(sortDesc);
        }
        return sb.toString();
    }
    
    
}
