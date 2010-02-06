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
package com.avaje.ebean.server.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import com.avaje.ebean.internal.SpiQuery;
import com.avaje.ebean.server.deploy.BeanDescriptor;

public class LdapOrmQueryExecute<T> {

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
            
            Attributes attrs = dc.getAttributes(dn, selectProps);

            return beanBuilder.readAttributes(attrs);

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

            if (selectProps != null) {
                sc.setReturningAttributes(selectProps);
            }


            NamingEnumeration<SearchResult> result;
            if (filterValues == null) {
                result = dc.search(dn, filterExpr, sc);
            } else {
                result = dc.search(dn, filterExpr, filterValues, sc);
            }

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
