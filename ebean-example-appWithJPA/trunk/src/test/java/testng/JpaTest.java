package testng;

import app.data.User;
import app.data.User.State;

import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

/**
 * DOCTASK: DOCUMENT ME!
 *
 * @author Paul Mendelson
 * @version $Revision$, $Date$
 */
@ContextConfiguration(locations = 
{
    "/simpleSpringContext.xml"})
public class JpaTest extends AbstractTestNGSpringContextTests
{
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    @Autowired
    private EntityManagerFactory emf;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** Finds a known user */
    @Test
    public void testSimpleFind() throws SQLException
    {
        Assert.assertNotNull(emf, "Spring must inject emf");
        EntityManager em = emf.createEntityManager();
        Assert.assertNotNull(em, "EMF must create EM");
        Reporter.log("find em="+em.getClass().getName(), true);
        User u = em.find(User.class, 1);
        Assert.assertNotNull(u, "Must find a ser");
        Reporter.log("found "+u.getName(), true);
        Reporter.log("finish TestSimpleFind()",true);
    }

    /** Finds a known user */
    @Test
    public void testSimpleInsert() throws SQLException
    {
        Assert.assertNotNull(emf, "Spring must inject emf");
        EntityManager em = emf.createEntityManager();
        Assert.assertNotNull(em, "EMF must create EM");
        removeIfNecessary(99);
        User u = new User();
        u.setId(99);
        u.setEmail("testmail@email.com");
        u.setName("test insert");
        u.setState(State.NEW);
        em.persist(u);
    }

    private void removeIfNecessary(int uid)
    {
        EntityManager em = emf.createEntityManager();
        User u = em.find(User.class, uid);
        if(u != null)
        {
            Reporter.log("Deleting "+u.getName()+"...", true);
            em.remove(u);
        }
    }

    /** Creates new user */
    @Test(dependsOnMethods = "testSimpleInsert")
    public void testTransactionUpdate() throws SQLException
    {
        Assert.assertNotNull(emf, "Spring must inject emf");
        EntityManager em = emf.createEntityManager();
        Assert.assertNotNull(em, "EMF must create EM");
        em.getTransaction().begin();
        User u = em.find(User.class, 99);
        Assert.assertNotNull(u, "Must find a ser");
        Reporter.log("found "+u.getName(), true);
        u.setEmail("testmail2@email.com");
        u.setState(State.ACTIVE);
        em.persist(u);
    }
}