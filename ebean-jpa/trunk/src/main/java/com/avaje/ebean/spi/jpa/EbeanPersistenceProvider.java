package com.avaje.ebean.springsupport.jpa;

import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


/**
 * DOCTASK: DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$, $Date$
 * @since Jan, 2009
  */
public class EbeanPersistenceProvider implements PersistenceProvider, ApplicationContextAware
{
    //~ Constants ----------------------------------------------------------------------------------------------------------------

    private static Logger log = Logger.getLogger("jpa.spi");

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new EbeanPersistenceProvider object.
     */
    public EbeanPersistenceProvider() {log.info("creating EbeanPersistance...");}

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** @see PersistenceProvider#createContainerEntityManagerFactory(PersistenceUnitInfo, Map) */
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo pu, Map overridenProperties)
    {
        log.info("createContainerEntityManagerFactory  "+pu);
        DataSource ds = null;//EBeanDataSourceFactory.getDataSource();
        log.info("container "+pu.getPersistenceUnitName()+"  ds="+ds);
        EbeanEntityManagerFactory r = new EbeanEntityManagerFactory(pu.getPersistenceUnitName());
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

	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        log.info("set  "+ctx);
	}
}