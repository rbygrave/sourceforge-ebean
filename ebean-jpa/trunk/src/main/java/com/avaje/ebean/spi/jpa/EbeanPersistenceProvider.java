package org.avaje.ebean.spi.jpa;

import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import javax.sql.DataSource;

import org.avaje.ebean.spi.spring.EBeanDataSourceFactory;

/**
 * DOCTASK: DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
  */
public class EbeanPersistenceProvider implements PersistenceProvider
{
    //~ Constants ----------------------------------------------------------------------------------------------------------------

    private static Logger log = Logger.getLogger("jpa.spi");

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new EbeanPersistenceProvider object.
     */
    public EbeanPersistenceProvider() {}

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** @see PersistenceProvider#createContainerEntityManagerFactory(PersistenceUnitInfo, Map) */
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo arg0, Map arg1)
    {
        DataSource ds = EBeanDataSourceFactory.getDataSource();
        log.info("container "+arg0.getPersistenceUnitName()+"  ds="+ds);
        EbeanEntityManagerFactory r = new EbeanEntityManagerFactory(arg0.getPersistenceUnitName());
        if(ds != null)
        {
            r.setDataSource(ds);
        }
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @param arg0 DOCUMENT ME!
     * @param arg1 DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public EntityManagerFactory createEntityManagerFactory(String arg0, Map arg1)
    {
        return new EbeanEntityManagerFactory(arg0);
    }
}