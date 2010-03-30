package com.avaje.tests.text.json;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.text.json.JsonContext;
import com.avaje.ebean.text.json.JsonWriteBeanVisitor;
import com.avaje.ebean.text.json.JsonWriter;
import com.avaje.ebean.text.json.JsonWriteOptions;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestTextJsonSimple extends TestCase {

    public void test() {
        
        ResetBasicData.reset();
        
        List<Customer> list = Ebean.find(Customer.class)
            .select("id, name, status, shippingAddress")
            .fetch("billingAddress","line1, city")
            .fetch("billingAddress.country","*")
            .fetch("contacts", "firstName,email")
            //.filterMany("contacts").raw("(cc.first_name is null or cc.first_name like 'J%')").query()
            //.where().lt("id", 3)
            .order().desc("id")
            .findList();
        
        EbeanServer server = Ebean.getServer(null);

        JsonContext json = server.createJsonContext();
        
        JsonWriteOptions options = new JsonWriteOptions();
        options.setPretty(true);
        options.addRootVisitor(new JsonWriteBeanVisitor<Customer>() {

            
            public Set<String> getIncludeProperties() {
                final LinkedHashSet<String> p = new LinkedHashSet<String>();
                p.add("name");
                p.add("id");
                return null;
            }

            public void visit(Customer bean, JsonWriter ctx) {
                System.out.println("visiting "+bean);
                ctx.appendKeyValue("dummy", "34");
                //ctx.appendKeyValue("dummy", "{\"a\":34,\"b\":\"asdasdasd\"}");
            }
            
        });
        
        String s = json.toJsonString(list, options);
        System.out.println(s);
        
        List<Customer> mList = json.toList(Customer.class, s);
        System.out.println(mList);
        
    }
}
