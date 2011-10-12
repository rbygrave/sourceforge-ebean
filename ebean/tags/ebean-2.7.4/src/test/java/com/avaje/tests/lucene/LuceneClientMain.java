package com.avaje.tests.lucene;

import java.io.IOException;
import java.util.List;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query.UseIndex;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.lucene.LIndex;
import com.avaje.ebeaninternal.server.lucene.LuceneIndexManager;
import com.avaje.ebeaninternal.server.lucene.cluster.SLuceneClusterSocketClient;
import com.avaje.tests.model.basic.Customer;

public class LuceneClientMain {

    public static void main(String[] args) throws IOException {

        GlobalProperties.put("ebean.ddl.gen", "false");
        GlobalProperties.put("ebean.ddl.run", "false");

        String masterHost = "127.0.0.1:9991";
        
        GlobalProperties.put("ebean.cluster.type", "mcast");
        //GlobalProperties.put("ebean.cluster.mcast.listen.port", "9768");
        //GlobalProperties.put("ebean.cluster.mcast.listen.address", "235.1.1.1");
        //GlobalProperties.put("ebean.cluster.mcast.listen.disableLoopback", "false");
        GlobalProperties.put("ebean.cluster.mcast.send.port", "9767");
        
        
        GlobalProperties.put("ebean.cluster.lucene.port", "9992");
        GlobalProperties.put("ebean.lucene.baseDirectory", "luceneS1b");
        GlobalProperties.put("ebean.cluster.lucene.masterHostPort", masterHost);
        
        SpiEbeanServer server = (SpiEbeanServer)Ebean.getServer(null);

        LuceneIndexManager indexManager = server.getLuceneIndexManager();
        LIndex index = indexManager.getIndex(CustIndexDefn.class.getName());
        index.refresh(false);
        checkQuery();

        
        SLuceneClusterSocketClient c = new SLuceneClusterSocketClient(index);
        if (c.isSynchIndex(masterHost)) {
            c.transferFiles();
        }
        
        index.refresh(true);
        
        checkQuery();
        
    }
    
    private static void checkQuery() {
        
        List<Customer> list = Ebean.find(Customer.class)
            .setUseIndex(UseIndex.YES_OBJECTS)
            //.select("id, name")
            //.where().istartsWith("name", "m")
            .findList();
    
        for (Customer customer : list) {
            System.out.println(""+customer);
        }
    }
    
}
