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
package com.avaje.ebeaninternal.server.expression;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;

import com.avaje.ebean.LikeType;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.api.SpiLuceneExpr;

public class LikeExpressionLucene {

    public SpiLuceneExpr createLuceneExpr(SpiExpressionRequest request, String propertyName, LikeType type,
            boolean caseInsensitive, String val) {

        try {
            String exprValue = getLuceneValue(val, caseInsensitive, type);

            String desc = propertyName+" like "+exprValue;
            
            QueryParser queryParser = request.getLuceneIndex().createQueryParser(propertyName);
            return new LuceneExprResponse(queryParser.parse(exprValue), desc);
        } catch (ParseException e) {
            throw new PersistenceLuceneParseException(e);
        }
    }
    
    private static String getLuceneValue(String value, boolean caseInsensitive, LikeType type) {
        if (caseInsensitive) {
            value = value.toLowerCase();
        }
        value = value.replace('%', '*');
        
        switch (type) {
        case RAW:
            return value;
        case STARTS_WITH:
            return value + "*";
        case CONTAINS:
            return value;
        case EQUAL_TO:
            return value;
        case ENDS_WITH:
            throw new RuntimeException("Not Supported - Never get here");

        default:
            throw new RuntimeException("LikeType " + type + " missed?");
        }
    }
}
