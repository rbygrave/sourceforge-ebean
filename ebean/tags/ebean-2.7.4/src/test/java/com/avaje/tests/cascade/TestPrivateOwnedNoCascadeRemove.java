package com.avaje.tests.cascade;

import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.bean.BeanCollection;
import com.avaje.tests.model.basic.TSDetailTwo;
import com.avaje.tests.model.basic.TSMasterTwo;

public class TestPrivateOwnedNoCascadeRemove extends TestCase {

	public void test() {
		
		TSMasterTwo m0 = new TSMasterTwo();
		m0.setName("m1");
		
		m0.addDetail(new TSDetailTwo("m1 detail 1"));
		m0.addDetail(new TSDetailTwo("m1 detail 2"));
		
		Ebean.save(m0);
		
		TSMasterTwo master = Ebean.find(TSMasterTwo.class, m0.getId());
		List<TSDetailTwo> details = master.getDetails();
		
		TSDetailTwo removedDetail = details.remove(1);
		
		BeanCollection<?> bc = (BeanCollection<?>)details;
		Set<?> modifyRemovals = bc.getModifyRemovals();
		
		Assert.assertNotNull(modifyRemovals);
		Assert.assertTrue(modifyRemovals.size() == 1);
		Assert.assertTrue(modifyRemovals.contains(removedDetail));
		
		Ebean.save(master);
		
		TSMasterTwo masterReload = Ebean.find(TSMasterTwo.class, m0.getId());
		List<TSDetailTwo> detailsReload = masterReload.getDetails();

		// the removed bean has really been removed
		Assert.assertTrue(detailsReload.size() == 1);
		
		try {
		    Ebean.delete(masterReload);
		    fail("delete should error");
		} catch (Exception e){
		    assertTrue("delete failed",true);
		}
	}
	
}
