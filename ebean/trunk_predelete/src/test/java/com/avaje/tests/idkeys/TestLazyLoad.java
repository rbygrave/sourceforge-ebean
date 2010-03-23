package com.avaje.tests.idkeys;

import java.sql.SQLException;
import java.util.List;

import com.avaje.tests.lib.EbeanTestCase;
import com.avaje.tests.model.basic.TOne;

/**
 * Test lazy loading
 */
public class TestLazyLoad extends EbeanTestCase
{
    /**
     * This test loads just a single property of the Entity AuditLog and later on access
     * the description which should force a lazy load of this property
     */
    public void testPartialLoad() throws SQLException
    {
        TOne log = new TOne();
        log.setName("test partial");
        log.setDescription("log");

        getServer().save(log);

        assertNotNull(log.getId());

        List<TOne> logs = getServer().find(TOne.class)
                .select("id")
                .where().eq("id", log.getId())
                .findList();

        assertNotNull(logs);
        assertEquals(1, logs.size());

        TOne logLazy = logs.get(0);

        String description = logLazy.getDescription();
        assertEquals(log.getDescription(), description);
    }
}