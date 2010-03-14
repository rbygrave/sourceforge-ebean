package com.avaje.tests.query;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Article;
import com.avaje.tests.model.basic.Section;

public class TestQueryFindReadOnly extends TestCase {

    public void test() {
        
        Section s0 = new Section("some content");
        Article a0 = new Article("art1","auth1");
        a0.addSection(s0);
        
        Ebean.save(a0);
        
        Ebean.runCacheWarming(Article.class);
        
        Article ar0 = Ebean.find(Article.class, a0.getId());
        
        Assert.assertNotNull(ar0);
        Assert.assertTrue("readonly true",Ebean.getBeanState(ar0).isReadOnly());

        List<Section> ar0sections = ar0.getSections();
        Section s1 = ar0sections.get(0);
        Assert.assertTrue("readonly true",Ebean.getBeanState(s1).isReadOnly());

//        Article ar1 = Ebean.find(Article.class)
//            .setReadOnly(false)
//            .setId(a0.getId())
//            .findUnique();
//        
//        Assert.assertNotNull(ar1);
//        Assert.assertFalse("readonly",Ebean.getBeanState(ar1).isReadOnly());
//        
//        
//        List<Section> ar1sections = ar1.getSections();
//        Assert.assertEquals(1, ar1sections.size());
//        
//        Section s2 = ar1sections.get(0);
//        Assert.assertFalse("readonly cascading",Ebean.getBeanState(s2).isReadOnly());
        
        
        
    }
    
}
