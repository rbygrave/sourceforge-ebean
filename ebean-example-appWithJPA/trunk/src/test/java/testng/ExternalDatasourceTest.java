package testng;

import app.data.User;
import app.data.User.State;

import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.avaje.ebean.spi.spring.EBeanDataSourceFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests JPA api with spring provided datasource.
 *
 * @author Paul Mendelson
 * @version $Revision$, $Date$
 */
@ContextConfiguration(locations = 
{
    "/externalDatasourceContext.xml"})
public class ExternalDatasourceTest extends AbstractTestNGSpringContextTests
{
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    @Autowired
    private EntityManagerFactory emf;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public ExternalDatasourceTest()
//    /**
//     * {@inheritDoc}
//     *
//     * @throws Exception DOCUMENT ME!
//     */
//    protected void springTestContextPrepareTestInstance() throws Exception
    {
        System.setProperty("ebean.props.file", "externalds.ebean.properties");
        Reporter.log("set Prop", true);
//        super.springTestContextPrepareTestInstance();
    }

    /**
     * {@inheritDoc}
     */
    public void setUp() {}

    /** Finds a known user */
    @Test
    public void testSpringComponents() throws SQLException
    {
        Object bean = applicationContext.getBean("dataSource");
        Assert.assertNotNull(bean);
        Reporter.log("bean="+bean.getClass().getName(), true);
        EBeanDataSourceFactory fact = (EBeanDataSourceFactory)applicationContext.getBean("ebeanFactory");
        Assert.assertNotNull(fact);
        Assert.assertNotNull(emf);
        Reporter.log("Prop2="+System.getProperty("ebean.props.file"), true);
    }
}