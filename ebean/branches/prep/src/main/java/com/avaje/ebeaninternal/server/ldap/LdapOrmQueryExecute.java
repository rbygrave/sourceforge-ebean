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
package com.avaje.ebeaninternal.server.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

public class LdapOrmQueryExecute<T> {

    private static final Logger logger = Logger.getLogger(LdapOrmQueryExecute.class.getName());
    
    private final SpiQuery<?> query;

    private final BeanDescriptor<T> beanDescriptor;

    private final DirContext dc;

    private final LdapBeanBuilder<T> beanBuilder;

    private final String filterExpr;

    private final Object[] filterValues;

    private final String[] selectProps;

    public LdapOrmQueryExecute(LdapOrmQueryRequest<T> request, boolean defaultVanillaMode, DirContext dc) {

        this.query = request.getQuery();
        this.beanDescriptor = request.getBeanDescriptor();
        this.dc = dc;

        boolean vanillaMode = query.isVanillaMode(defaultVanillaMode);
        this.beanBuilder = new LdapBeanBuilder<T>(beanDescriptor, vanillaMode);

        LdapQueryDeployHelper deployHelper = new LdapQueryDeployHelper(request);
        this.selectProps = deployHelper.getSelectedProperties();
        this.filterExpr = deployHelper.getFilterExpr();
        this.filterValues = deployHelper.getFilterValues();
    }

    public T findId() {

        Object id = query.getId();

        try {
            LdapName dn = beanDescriptor.createLdapNameById(id);

            String[] findAttrs = selectProps;
            if (findAttrs == null){
                findAttrs = beanDescriptor.getDefaultSelectDbArray();
            }
            
            // build a string describing the query
            String debugQuery = "Name:"+dn+" attrs:"+Arrays.toString(findAttrs);

            Attributes attrs = dc.getAttributes(dn, findAttrs);

            T bean = beanBuilder.readAttributes(attrs);
            
            query.setGeneratedSql(debugQuery);
            return bean;

        } catch (NamingException e) {
            throw new LdapPersistenceException(e);
        }
    }

    public List<T> findList() {

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.ONELEVEL_SCOPE);

        List<T> list = new ArrayList<T>();

        try {
            LdapName dn = beanDescriptor.createLdapName(null);

            // build a string describing the query
            String debugQuery = "Name:"+dn;
           
            if (selectProps != null) {
                sc.setReturningAttributes(selectProps);
                debugQuery += " select:"+Arrays.toString(selectProps);
            }

            if (logger.isLoggable(Level.INFO)){
                logger.info("Ldap Query  Name:"+dn+" filterExpr:"+filterExpr);
            }
               
            debugQuery += " filterExpr:"+filterExpr;

            NamingEnumeration<SearchResult> result;
            if (filterValues == null && filterValues.length > 0) {
                result = dc.search(dn, filterExpr, sc);
            } else {
                debugQuery += " filterValues:"+Arrays.toString(filterValues);
                result = dc.search(dn, filterExpr, filterValues, sc);
            }
           
            query.setGeneratedSql(debugQuery);

            if (result != null){
                while (result.hasMoreElements()) {
                    SearchResult row = result.nextElement();
                    T bean = beanBuilder.readAttributes(row.getAttributes());
                    list.add(bean);
                }
            }

            return list;

        } catch (NamingException e) {
            throw new LdapPersistenceException(e);
        }
    }

}
