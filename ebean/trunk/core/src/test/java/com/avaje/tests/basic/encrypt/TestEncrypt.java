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
        System.out.println("SqlRow: "+name+" "+desc);
        
        EBasicEncrypt e1 = Ebean.find(EBasicEncrypt.class, e.getId());
        
        String desc1 = e1.getDescription();
        System.out.println("Decrypted: "+desc1);
        
//        String s = "select TRIM(CHAR(0) FROM UTF8TOSTRING(DECRYPT('AES', STRINGTOUTF8(?), description))) as x"
//            +" from e_basicenc where id = ? and ? = TRIM(CHAR(0) FROM UTF8TOSTRING(DECRYPT('AES', STRINGTOUTF8(?), description))) ";
//        
//        SqlRow r = Ebean.createSqlQuery(s)
//            .setParameter(1, "simple")
//            .setParameter(2, e1.getId())
//            .setParameter(3, "testdesc")
//            .setParameter(4, "simple")
//            .findUnique();
//        
//        if (r == null){
//            System.out.println("No row found.");
//        }
//        System.out.println("R: "+r.get("x"));
        
        
        e1.setName("testmod");
        e1.setDescription("moddesc");
        
        Ebean.save(e1);

        EBasicEncrypt e2 = Ebean.find(EBasicEncrypt.class, e.getId());
        
        String desc2 = e2.getDescription();
        System.out.println("moddesc="+desc2);
        
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
