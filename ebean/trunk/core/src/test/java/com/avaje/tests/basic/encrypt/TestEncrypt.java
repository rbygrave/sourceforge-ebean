package com.avaje.tests.basic.encrypt;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.avaje.tests.model.basic.EBasicEncrypt;

public class TestEncrypt extends TestCase {

    public void test() {
        
        EBasicEncrypt e = new EBasicEncrypt();
        e.setName("testname");
        e.setDescription("testdesc");
        
        Ebean.save(e);
        
        
        SqlQuery q = Ebean.createSqlQuery("select * from e_basicenc where id = :id");
        q.setParameter("id", e.getId());
        
        SqlRow row = q.findUnique();
        String name = row.getString("name");
        Object desc = row.get("description");
        System.out.println(""+name+" "+desc);
        
        EBasicEncrypt e1 = Ebean.find(EBasicEncrypt.class, e.getId());
        
        String desc1 = e1.getDescription();
        System.out.println(""+desc1);
        
        e1.setName("testmod");
        e1.setDescription("moddesc");
        
        Ebean.save(e1);

        EBasicEncrypt e2 = Ebean.find(EBasicEncrypt.class, e.getId());
        
        String desc2 = e2.getDescription();
        System.out.println(""+desc2);
        
        List<EBasicEncrypt> list 
            = Ebean.find(EBasicEncrypt.class)
                .where().eq("description", "moddesc")
                .findList();

        Assert.assertEquals(1, list.size());
        
        list 
            = Ebean.find(EBasicEncrypt.class)
                .where().startsWith("description", "modde")
                .findList();
        
        Assert.assertEquals(1, list.size());
    }
    
}
