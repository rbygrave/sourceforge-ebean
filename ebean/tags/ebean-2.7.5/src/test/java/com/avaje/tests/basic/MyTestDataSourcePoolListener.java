package com.avaje.tests.basic;

import com.avaje.ebeaninternal.server.lib.sql.DataSourcePoolListener;

import java.sql.Connection;

public class MyTestDataSourcePoolListener implements DataSourcePoolListener
{
    public static int SLEEP_AFTER_BORROW = 0; 
    
    public void onAfterBorrowConnection(Connection c)
    {
        if (SLEEP_AFTER_BORROW > 0)
        {
            try
            {
                Thread.sleep(SLEEP_AFTER_BORROW);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public void onBeforeReturnConnection(Connection c)
    {
    }
}
