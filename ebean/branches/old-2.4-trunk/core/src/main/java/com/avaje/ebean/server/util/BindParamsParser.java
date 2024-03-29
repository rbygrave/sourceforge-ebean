/**
 * Copyright (C) 2006  Robin Bygrave
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
package com.avaje.ebean.server.util;

import java.util.Collection;
import java.util.Iterator;

import javax.persistence.PersistenceException;

import com.avaje.ebean.config.EncryptKey;
import com.avaje.ebean.internal.BindParams;
import com.avaje.ebean.internal.BindParams.OrderedList;
import com.avaje.ebean.internal.BindParams.Param;
import com.avaje.ebean.server.deploy.BeanDescriptor;

/**
 * Parses the BindParams if they are using named parameters.
 * <p>
 * This is a thread safe implementation.
 * </p>
 */
public class BindParamsParser {


    public static final String ENCRYPTKEY_PREFIX = "encryptkey_";
    public static final String ENCRYPTKEY_GAP = "___";

    private static final int ENCRYPTKEY_PREFIX_LEN = ENCRYPTKEY_PREFIX.length();
    private static final int ENCRYPTKEY_GAP_LEN = ENCRYPTKEY_GAP.length();

    /**
     * Used to parse sql looking for named parameters.
     */
    private static final String quote = "'";

    /**
     * Used to parse sql looking for named parameters.
     */
    private static final String colon = ":";

    private final BindParams params;
    private final String sql;

    private final BeanDescriptor<?> beanDescriptor;

    public static String parse(BindParams params, String sql) {
        return parse(params, sql, null);
    }
    
    public static String parse(BindParams params, String sql, BeanDescriptor<?> beanDescriptor) {
        return new BindParamsParser(params, sql, beanDescriptor).parseSql();
    }

    public static OrderedList parseNamedParams(BindParams params, String sql) {
        return new BindParamsParser(params, sql, null).parseSqlNamedParams();
    }
    
    private BindParamsParser(BindParams params, String sql, BeanDescriptor<?> beanDescriptor) {
        this.params = params;
        this.sql = sql;
        this.beanDescriptor = beanDescriptor;
    }
        
    /**
     * Used for parsing having clauses with named parameters.
     * <p>
     * The issue here is that BindParams contains named parameters for
     * both where and having clauses. BindParams.positionedParameters is
     * used for the where and the OrderedList for the having.
     * </p>
     */
    private OrderedList parseSqlNamedParams() {
        OrderedList orderedList = new OrderedList();
        parseNamedParams(orderedList);
        return orderedList;
    }
    
    /**
     * Parse the sql changed named parameters to positioned parameters if required.
     * <p>
     * The sql is used when named parameters are used.
     * </p>
     * <p>
     * This is used in most cases of named parameters. The case it is NOT used for is
     * named parameters in a having clause. In this case some of the named parameters
     * could be for a where clause and some for the having clause.
     * </p>
     */
    private String parseSql() {
    	
    	String preparedSql = params.getPreparedSql();
    	if (preparedSql != null){
    		// the sql has already been parsed and 
    		// positionedParameters are set in order
    		return preparedSql;
    	}
    	
    	String prepardSql;
        if (params.requiresNamedParamsPrepare()) {
        	OrderedList orderedList = new OrderedList(params.positionedParameters());
        	
            parseNamedParams(orderedList);
            prepardSql = orderedList.getPreparedSql();
        } else {
        	prepardSql = sql;
        }
        params.setPreparedSql(prepardSql);
        return prepardSql;        
    }

    
    
    /**
     * Named parameters need to be parsed and replaced with ?.
     */
    private void parseNamedParams(OrderedList orderedList) {

        parseNamedParams(0, orderedList);
    }

    private void parseNamedParams(int startPos, OrderedList orderedList) {

    	if (sql == null){
    		throw new PersistenceException("query does not contain any named bind parameters?");
    	}
        if (startPos > sql.length()) {
            return;
        }

        // search for quotes and named params... in order...
        int beginQuotePos = sql.indexOf(quote, startPos);
        int nameParamStart = sql.indexOf(colon, startPos);
        if (beginQuotePos > 0 && beginQuotePos < nameParamStart) {
            // the quote precedes the named parameter...
            // find and add up to the end quote
            int endQuotePos = sql.indexOf(quote, beginQuotePos + 1);
            String sub = sql.substring(startPos, endQuotePos + 1);
            orderedList.appendSql(sub);

            // start again after the end quote
            parseNamedParams(endQuotePos + 1, orderedList);

        } else {
            if (nameParamStart < 0) {
                // no more params, add the rest
                String sub = sql.substring(startPos, sql.length());
                orderedList.appendSql(sub);

            } else {
                // find the end of the parameter name
                int endOfParam = nameParamStart + 1;
                do {
                    char c = sql.charAt(endOfParam);
                    if (c != '_' && !Character.isLetterOrDigit(c)) {
                        break;
                    }
                    endOfParam++;
                } while (endOfParam < sql.length());

                // add the named parameter value to bindList
                String paramName = sql.substring(nameParamStart + 1, endOfParam);
                
                Param param;
                if (paramName.startsWith(ENCRYPTKEY_PREFIX)){
                    param = addEncryptKeyParam(paramName);
                } else {
                    param = params.getParameter(paramName);
                }
                
                if (param == null) {
                	String msg = "Bind value is not set or null for [" + paramName
                    + "] in [" + sql+ "]";
                    throw new PersistenceException(msg);
                }

                String sub = sql.substring(startPos, nameParamStart);
                orderedList.appendSql(sub);

                // check if inValue is a Collection type...
                Object inValue = param.getInValue();
                if (inValue != null && inValue instanceof Collection<?>){
                	// Chop up Collection parameter into a number 
                	// of individual parameters and add each one individually
                    Collection<?> collection = (Collection<?>)inValue;
                    Iterator<?> it = collection.iterator();
                    int c = 0;
                    while (it.hasNext()) {
                        Object elVal = (Object) it.next();
                        if (++c > 1){
                        	orderedList.appendSql(",");
                        }
                        orderedList.appendSql("?");
                        BindParams.Param elParam = new BindParams.Param();
                        elParam.setInValue(elVal);
                        orderedList.add(elParam);
                    }
	
                } else {
                	// its a normal scalar value parameter...
                	orderedList.add(param);
                    orderedList.appendSql("?");
                }

                // continue on after the end of the parameter
                parseNamedParams(endOfParam, orderedList);
            }
        }
    }

    /**
     * Add an encryption key bind parameter.
     */
    private Param addEncryptKeyParam(String keyNamedParam) {
        
        
        int pos = keyNamedParam.indexOf(ENCRYPTKEY_GAP, ENCRYPTKEY_PREFIX_LEN);
        
        String tableName = keyNamedParam.substring(ENCRYPTKEY_PREFIX_LEN, pos);
        String columnName = keyNamedParam.substring(pos+ENCRYPTKEY_GAP_LEN);
        
        EncryptKey key = beanDescriptor.getEncryptKey(tableName, columnName);
        String strKey = key.getStringValue();
        
        return params.setEncryptionKey(keyNamedParam, strKey);
    }

}
