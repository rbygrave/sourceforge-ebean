package com.avaje.tests.basic;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.tests.model.embedded.EMain;
import junit.framework.TestCase;

public class TestDynamicUpdate extends TestCase {

	public void testUpdate() {

		// insert
		EMain b = new EMain();
		b.setName("123");
		b.getEmbeddable().setDescription("123");

		EbeanServer server = Ebean.getServer(null);
		server.save(b);

		assertNotNull(b.getId());

		// reload object und update the name
		EMain b2 = server.find(EMain.class, b.getId());

		b2.getEmbeddable().setDescription("ABC");
		server.save(b2);

		EMain b3 = server.find(EMain.class, b.getId());

		assertEquals("ABC", b3.getEmbeddable().getDescription());
	}
}
