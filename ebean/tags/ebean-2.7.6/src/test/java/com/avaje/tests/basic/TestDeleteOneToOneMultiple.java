package com.avaje.tests.basic;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.PFile;
import com.avaje.tests.model.basic.PFileContent;

public class TestDeleteOneToOneMultiple extends TestCase {
    
    public void testCreateDeletePersistentFile() {

        PFile persistentFile = new PFile("test.txt", new PFileContent("test".getBytes()));

        Ebean.save(persistentFile);
        Integer id = persistentFile.getId();
        Integer contentId = persistentFile.getFileContent().getId();

        // should delete file and fileContent
        Ebean.delete(PFile.class, id);
        System.out.println("finished delete");

        PFile file1 = Ebean.find(PFile.class, id);
        PFileContent content1 = Ebean.find(PFileContent.class, contentId);

        Assert.assertNull(file1);
        Assert.assertNull(content1);

    }

}
