package com.avaje.tests.lucene;

import java.util.concurrent.ExecutionException;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.config.lucene.IndexUpdateFuture;
import com.avaje.ebean.config.lucene.LuceneIndex;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.ResetBasicData;

public class LuceneServerMain {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        
        
        EbeanServer server = Ebean.getServer(null);
        
        ResetBasicData.reset();
        
        LuceneIndex luceneIndex = server.getLuceneIndex(Customer.class);
        IndexUpdateFuture rebuildFuture = luceneIndex.rebuild();
        System.out.println("Indexed " + rebuildFuture.get());
        
        int count = 100;
        
        boolean run = true;
        do {
            Thread.sleep(30000);
            
            Customer c = server.find(Customer.class, 4);
            String newName = "m-"+(count++);
            c.setName(newName);
            server.save(c);
            System.out.println("set "+newName);
            System.out.print("");
        } while (run);
        
        System.out.println("done");
    }
}
