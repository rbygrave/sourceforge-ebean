package com.avaje.ebean;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.server.core.DefaultServerFactory;
import com.avaje.ebean.server.core.ServerFactory;

public class EbeanServerFactory {

	private static final Logger logger = Logger.getLogger(EbeanServerFactory.class.getName());
	
	private static ServerFactory serverFactory = createServerFactory();
	
	public static EbeanServer create(String name){
		
		EbeanServer server = serverFactory.createServer(name);
		
		return server;
	}
	
	public static EbeanServer create(ServerConfig config){
		
		if (config.getName() == null){
			throw new PersistenceException("The name is null (it is required)");
		}
		
		EbeanServer server = serverFactory.createServer(config);
		
		if (config.isDefaultServer()){
			GlobalProperties.setSkipPrimaryServer(true);
		}
		if (config.isRegister()){
			Ebean.register(server, config.isDefaultServer());
		}
		
		return null;
	}
	
	private static ServerFactory createServerFactory() {

		
		String implClassName = GlobalProperties.get("ebean.serverfactory", null);

		int delaySecs = GlobalProperties.getInt("ebean.start.delay", 0);
		if (delaySecs > 0) {
			try {
				// perhaps useful to delay the startup to give time to
				// attach a debugger when running in a server like tomcat.
				String m = "Ebean sleeping " + delaySecs + " seconds due to ebean.start.delay";
				logger.log(Level.INFO, m);
				Thread.sleep(delaySecs * 1000);

			} catch (InterruptedException e) {
				String m = "Interrupting debug.start.delay of " + delaySecs;
				logger.log(Level.SEVERE, m, e);
			}
		}
		if (implClassName == null) {
			return new DefaultServerFactory();

		} else {
			try {
				// use a client side implementation?
				Class<?> cz = Class.forName(implClassName);
				return (ServerFactory) cz.newInstance();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}
}
