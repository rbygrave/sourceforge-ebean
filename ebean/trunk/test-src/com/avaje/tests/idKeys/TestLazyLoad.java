package com.avaje.tests.idKeys;

import java.sql.SQLException;
import java.util.List;

import com.avaje.tests.idKeys.db.AuditLog;
import com.avaje.tests.lib.EbeanTestCase;

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
        AuditLog log = new AuditLog();
        log.setDescription("log");

        getServer().save(log);

        assertNotNull(log.getId());

        List<AuditLog> logs = getServer().find(AuditLog.class)
                .select("id")
                .where().eq("id", log.getId())
                .findList();

        assertNotNull(logs);
        assertEquals(1, logs.size());

        AuditLog logLazy = logs.get(0);

        String description = logLazy.getDescription();
        assertEquals(log.getDescription(), description);
    }
}