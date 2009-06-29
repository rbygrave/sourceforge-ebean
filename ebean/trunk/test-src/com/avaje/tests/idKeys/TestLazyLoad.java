package com.avaje.tests.idKeys;

import com.avaje.ebean.expression.Expr;
import com.avaje.tests.idKeys.db.AuditLog;
import com.avaje.tests.lib.EbeanTestCase;

import java.sql.SQLException;
import java.util.List;

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
                .where(Expr.eq("id", log.getId()))
                .select("id")
                .findList();

        assertNotNull(logs);
        assertEquals(1, logs.size());

        AuditLog logLazy = logs.get(0);

        assertEquals(log.getDescription(), logLazy.getDescription());
    }
}