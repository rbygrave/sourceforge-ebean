package org.avaje.ebean.spi.spring;

import java.util.logging.Logger;

import javax.sql.DataSource;

import com.avaje.ebean.server.core.DataSourceFactory;
import com.avaje.ebean.server.lib.ConfigProperties;

/**
 * DOCTASK: DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
 */
public class EBeanDataSourceFactory implements DataSourceFactory
{
    //~ Constants ----------------------------------------------------------------------------------------------------------------

    private static Logger log = Logger.getLogger("jpa.spi");
    private static DataSource m_data_source;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** Access to injected data source */
    public static DataSource getDataSource()
    {
        return m_data_source;
    }

    /**
     * @see DataSourceFactory#createDataSource(String, ConfigProperties)
     */
    public DataSource createDataSource(String arg0, ConfigProperties propset)
    {
        log.info("arg0="+arg0+"  ds="+m_data_source);
        return m_data_source;
    }

    /** Dependency injection point */
    public void setDataSource(DataSource v)
    {
        System.err.println(getClass().getName()+"setDS:"+v);
        m_data_source = v;
    }
}