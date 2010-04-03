package com.avaje.ebeaninternal.server;

import junit.framework.Assert;

import org.junit.Test;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.dbplatform.GenericDatabasePlatform;
import com.avaje.ebean.config.dbplatform.H2Platform;
import com.avaje.ebean.config.dbplatform.MsSqlServer2000Platform;
import com.avaje.ebean.config.dbplatform.MySqlPlatform;
import com.avaje.ebean.config.dbplatform.Oracle10Platform;
import com.avaje.ebeaninternal.server.core.DatabasePlatformFactory;
import com.avaje.stub.PseudoDataSource;

public class DatabasePlatformFactoryTest {

	@Test
	public void testByJdbc() {
		doJdbcTest("oracle", 8, Oracle10Platform.class);
		doJdbcTest("oracle", 10, Oracle10Platform.class);
		doJdbcTest("xyz", 8, GenericDatabasePlatform.class);
		doJdbcTest("microsoft", 8, MsSqlServer2000Platform.class);
		doJdbcTest("h2", 8, H2Platform.class);
	}

	private void doJdbcTest(String dbProduct, int majorVersion, Class<? extends DatabasePlatform> dbmsClass) {
		DatabasePlatformFactory fact = new DatabasePlatformFactory();
		final ServerConfig cfg = new ServerConfig();
		cfg.setDataSource(new PseudoDataSource(dbProduct, majorVersion));
		final DatabasePlatform adaptor = fact.create(cfg);
		Assert.assertNotNull(adaptor);
		Assert.assertEquals(dbmsClass, adaptor.getClass());
		System.out.println(dbProduct + " v" + majorVersion + " yields " + adaptor.getClass().getName());
	}

	@Test
	public void testByProductName() {
		doProductTest("mysql", MySqlPlatform.class);
		doProductTest("xyz", GenericDatabasePlatform.class);
		doProductTest("sqlserver2000", MsSqlServer2000Platform.class);
	}

	private void doProductTest(String ebeanProduct, Class<? extends DatabasePlatform> dbmsClass) {
		DatabasePlatformFactory fact = new DatabasePlatformFactory();
		final ServerConfig cfg = new ServerConfig();
		cfg.setDatabasePlatformName(ebeanProduct);
		final DatabasePlatform adaptor = fact.create(cfg);
		Assert.assertNotNull(adaptor);
		Assert.assertEquals(dbmsClass, adaptor.getClass());
		System.out.println(ebeanProduct + " yields " + adaptor.getClass().getName());
	}
}
