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
package com.avaje.tests.ldap;

import java.util.Arrays;

import junit.framework.Assert;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Query;
import com.avaje.ebean.internal.SpiEbeanServer;
import com.avaje.ebean.internal.SpiQuery;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.ldap.LdapOrmQueryRequest;
import com.avaje.ebean.server.ldap.LdapQueryDeployHelper;
import com.avaje.tests.model.ldap.LDPerson;

public class TestLdapQueryParse extends BaseLdapTest{

    public void test() {
        
        boolean b = true;
        if (b){
            // turn this test off for the moment
            return;
        }

        EbeanServer server = createServer();
        
        Query<LDPerson> query = server.createQuery(LDPerson.class);
        query.select("uid, cn, status");
        query.where("(cn=rob*)");
        query.where()
            .raw("(inetUserStatus=Banana)")
            .eq("status", LDPerson.Status.ACTIVE)
            .eq("sn", "bana");
        
        SpiQuery<LDPerson> sq = (SpiQuery<LDPerson>)query;
        
        SpiEbeanServer spiServer = (SpiEbeanServer)server;
        BeanDescriptor<LDPerson> desc = spiServer.getBeanDescriptor(LDPerson.class);
        LdapOrmQueryRequest<LDPerson> req =  new LdapOrmQueryRequest<LDPerson>(sq, desc, null);

        LdapQueryDeployHelper deployHelper = new LdapQueryDeployHelper(req);
        
        String[] sp = deployHelper.getSelectedProperties();
        String filterExpr = deployHelper.getFilterExpr();
        Object[] filterVals = deployHelper.getFilterValues();
        
        System.out.println("filterExpr: "+filterExpr);
        System.out.println("filterVals: "+Arrays.toString(filterVals));
        Assert.assertNotNull(sp);
        
    }
}
