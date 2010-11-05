package com.avaje.tests.lucene;

import java.util.List;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

import com.avaje.ebean.config.lucene.AbstractIndexDefn;
import com.avaje.ebean.config.lucene.IndexDefnBuilder;
import com.avaje.ebean.config.lucene.IndexFieldDefn;
import com.avaje.ebean.config.lucene.IndexFieldDefn.Sortable;
import com.avaje.tests.model.basic.Customer;

public class CustIndexDefn extends AbstractIndexDefn<Customer> {

    private List<IndexFieldDefn> fields;

    public void initialise(IndexDefnBuilder builder) {
        
        builder.addField("id");
        builder.addField("name", Sortable.YES);
        builder.addField("status", Store.YES, Index.ANALYZED, Sortable.YES);
        builder.addField("anniversary");
        builder.addFieldConcat("_nameAddress", "name","billingAddress.line1","billingAddress.city");
        builder.assocOne("shippingAddress").addAllFields();
        builder.assocOne("billingAddress").addAllFields();
        
        this.fields = builder.getFields();
    }

    public String getDefaultField() {
        return "name";
    }

    public List<IndexFieldDefn> getFields() {
        return fields;
    }

    public boolean isUpdateSinceSupported() {
        return true;
    }

    public String[] getUpdateSinceProperties(){
        return new String[]{
                "updtime",
                "shippingAddress.updtime",
                "billingAddress.updtime"};
    }
    
}
