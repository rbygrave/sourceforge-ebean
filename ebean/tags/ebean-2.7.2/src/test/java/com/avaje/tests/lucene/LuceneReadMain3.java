package com.avaje.tests.lucene;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Query.UseIndex;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.lucene.IndexUpdateFuture;
import com.avaje.ebean.config.lucene.LuceneIndex;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.ResetBasicData;

public class LuceneReadMain3 {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        boolean rebuildIndex = true;
        if (rebuildIndex) {
            ResetBasicData.reset();

            EbeanServer server = Ebean.getServer(null);
            LuceneIndex luceneIndex = server.getLuceneIndex(Customer.class);
            IndexUpdateFuture future = luceneIndex.rebuild();
            System.out.println("Indexed " + future.get());

        } else {
            GlobalProperties.put("ebean.ddl.generate", "false");
            GlobalProperties.put("ebean.ddl.run", "false");
        }

        EbeanServer server = Ebean.getServer(null);

        findAll(server, UseIndex.NO);
        findAll(server, UseIndex.YES_OBJECTS);
        
        if (true){
            return;
        }
        
//        java.sql.Date dateAfter = java.sql.Date.valueOf("2010-01-01");

        java.sql.Date onAfter = java.sql.Date.valueOf("2009-08-31");

        List<Customer> list = server.find(Customer.class)
            .setUseIndex(UseIndex.YES_OBJECTS)
            .where().disjunction()
                        .istartsWith("name", "r")
                        .eq("anniversary", onAfter)
                        .eq("status", Customer.Status.ACTIVE)
                        .endJunction()
//                        .gt("anniversary", dateAfter)
            .order().asc("name")
            .findList();

        for (Customer c : list) {
            System.out.println("Got Cust: " + c.getId() + " " + c.getName() + " " + c.getStatus()+ " " + c.getAnniversary());
        }

        Customer c = server.find(Customer.class, 1);
        
        c.setName("Modified name");
        c.getBillingAddress().setLine1("Another Nill Street");
        
        server.save(c);
        
        List<Customer> list2 = server.find(Customer.class)
            .setUseIndex(UseIndex.YES_OBJECTS)
            .where().istartsWith("name", "mod")
            .findList();
        
        out(list2);
        
        Thread.sleep(3000);
        
            
        c.setName("Modified name Agi");
        
        server.save(c);
        
        Thread.sleep(3000);
        
        findAll(server, UseIndex.NO);
        findAll(server, UseIndex.YES_OBJECTS);
        
    }
    
    private static void findAll(EbeanServer server, UseIndex useIndex) {
        List<Customer> list3 = server.find(Customer.class)
            .setUseIndex(useIndex)
            .fetch("shippingAddress")
            .fetch("billingAddress")
            //.where().lucene("_nameAddress", "ro* auckl*")
            .orderBy().asc("name")
            //.orderBy().desc("id")
            .findList();
               
        out(list3);
    }
    
    private static void out(List<Customer> list3) {
        
        for (Customer customer : list3) {
            System.out.println("Cust2: " + customer);
            System.out.println(" ... shippingAddress : " + customer.getShippingAddress());
            System.out.println(" ... billingAddress : " + customer.getBillingAddress());
        }
    }
}
