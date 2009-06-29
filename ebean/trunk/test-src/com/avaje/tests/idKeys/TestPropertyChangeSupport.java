package com.avaje.tests.idKeys;

import com.avaje.ebean.Transaction;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.tests.idKeys.db.AuditLog;
import com.avaje.tests.lib.EbeanTestCase;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test various aspects of the PropertyChangeSupport
 */
public class TestPropertyChangeSupport extends EbeanTestCase implements PropertyChangeListener
{
    private int nuofEvents = 0;
    private List<PropertyChangeEvent> pces = new ArrayList<PropertyChangeEvent>();
    private PropertyChangeEvent lastPce;

    public void propertyChange(PropertyChangeEvent evt)
    {
        nuofEvents++;
        lastPce = evt;
        pces.add(evt);
    }

    /**
     * Test the core property change functionality 
     */
    public void testPropertyChange() throws SQLException
    {
        resetEvent();

        AuditLog al = new AuditLog();

        assertTrue("is EntityBean check", al instanceof EntityBean);

        addListener(al, this);
        
        // test if we get property notification about simple property changes
        al.setDescription("ABC");

        assertNotNull(lastPce);
        assertEquals(1, nuofEvents);
        assertEquals("description", lastPce.getPropertyName());
        assertNull(lastPce.getOldValue());
        assertEquals("ABC", lastPce.getNewValue());

        al.setDescription("DEF");

        assertNotNull(lastPce);
        assertEquals(2, nuofEvents);
        assertEquals("description", lastPce.getPropertyName());
        assertEquals("ABC", lastPce.getOldValue());
        assertEquals("DEF", lastPce.getNewValue());

        resetEvent();

        // test if we get change notification if EBean assigns an id to the bean
        getServer().save(al);

        assertNotNull(lastPce);
        assertEquals("id", lastPce.getPropertyName());
        assertNull(lastPce.getOldValue());
        assertNotNull(lastPce.getNewValue());

        String prevLogDesc = al.getDescription();

        resetEvent();

        // simulate external change and test if we get change notification when we refresh the entity
        Transaction tx = getServer().beginTransaction();
        PreparedStatement pstm = tx.getConnection().prepareStatement("update audit_log set description = ? where id = ?");
        pstm.setString(1, "GHI");
        pstm.setLong(2, al.getId());
        int updated = pstm.executeUpdate();
        pstm.close();

        assertEquals(1, updated);

        tx.commit();

        assertNull(lastPce);
        assertEquals(0, nuofEvents);

        getServer().refresh(al);

        assertEquals("GHI", al.getDescription());

        assertNotNull(lastPce);
        assertEquals(1, nuofEvents);
        assertEquals("description", lastPce.getPropertyName());
        assertEquals(prevLogDesc, lastPce.getOldValue());
        assertNotNull("GHI", lastPce.getNewValue());

        // check if we fire with the real new value which might be a modified version of what we passed to the set method
        resetEvent();
        al.setModifiedDescription("MODIFIED_VALUE");

        assertNotNull(lastPce);
        assertEquals(1, nuofEvents);
        assertEquals("modifiedDescription", lastPce.getPropertyName());
        assertEquals(null, lastPce.getOldValue());
        assertNotNull("_MODIFIED_VALUE_", lastPce.getNewValue());
    }

    private void resetEvent()
    {
        lastPce = null;
        nuofEvents = 0;
        pces.clear();
    }

    private static void addListener(AuditLog al, PropertyChangeListener listener)
    {
        try
        {
            Method apcs = al.getClass().getMethod("addPropertyChangeListener", PropertyChangeListener.class);
            apcs.invoke(al, listener);
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }
}