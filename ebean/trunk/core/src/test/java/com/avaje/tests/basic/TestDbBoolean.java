package com.avaje.tests.basic;

import java.util.List;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.tests.model.basic.TOne;

public class TestDbBoolean {

	public static void main(String[] args) {
		TestDbBoolean me = new TestDbBoolean();
		EbeanServer server = me.createEbeanServer();
		
		me.simpleTest(server);
	}
	
	private EbeanServer createEbeanServer() {

		ServerConfig c = new ServerConfig();
		c.setName("pgtest");

		// requires postgres driver in class path
		DataSourceConfig postgresDb = new DataSourceConfig();
		postgresDb.setDriver("org.postgresql.Driver");
		postgresDb.setUsername("test");
		postgresDb.setPassword("test");
		postgresDb.setUrl("jdbc:postgresql://127.0.0.1:5432/test");
		postgresDb.setHeartbeatSql("select count(*) from t_one");

		// requires oracle driver in class path
		DataSourceConfig oraDb = new DataSourceConfig();
		oraDb.setDriver("oracle.jdbc.driver.OracleDriver");
		oraDb.setUsername("junk");
		oraDb.setPassword("junk");
		oraDb.setUrl("jdbc:oracle:thin:junk/junk@localhost:1521:XE");
		oraDb.setHeartbeatSql("select count(*) from dual");
		
		
		c.loadFromProperties();
		c.setDdlGenerate(true);
		c.setDdlRun(true);
		c.setDefaultServer(false);
		c.setRegister(false);
		//c.setDataSourceConfig(postgresDb);
		c.setDataSourceConfig(oraDb);
		
		//c.setDatabaseBooleanTrue("1");
		//c.setDatabaseBooleanFalse("0");
		//c.setDatabaseBooleanTrue("T");
		//c.setDatabaseBooleanFalse("F");

		//c.setDatabasePlatform(new Postgres83Platform());

		c.addClass(TOne.class);

		return EbeanServerFactory.create(c);

	}

	private void simpleTest(EbeanServer server) {

		TOne o = new TOne();
		o.setName("banan");
		o.setDescription("this one is true");
		o.setActive(true);

		server.save(o);

		TOne o2 = new TOne();
		o2.setName("banan");
		o2.setDescription("this one is false");
		o2.setActive(false);

		server.save(o2);

		
		List<TOne> list = server.find(TOne.class)
			.setAutofetch(false)
			.orderBy("id")
			.findList();
		
		System.out.println(list);

		System.out.println("done");
	}
}
