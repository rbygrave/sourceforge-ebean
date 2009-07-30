package com.avaje.ebean.server.autofetch;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.internal.SpiEbeanServer;
import com.avaje.ebean.server.resource.ResourceManager;

public class AutoFetchManagerFactory {

	private static final Logger logger = Logger.getLogger(AutoFetchManagerFactory.class.getName());
	
	
	public static AutoFetchManager create(SpiEbeanServer server, ServerConfig serverConfig, ResourceManager resourceManager) {
		
		AutoFetchManagerFactory me = new AutoFetchManagerFactory();
		return me.createAutoFetchManager(server, serverConfig, resourceManager);
	}
	
	private AutoFetchManager createAutoFetchManager(SpiEbeanServer server, ServerConfig serverConfig, ResourceManager resourceManager){
		
		AutoFetchManager manager = createAutoFetchManager(server.getName(), resourceManager);
		manager.setOwner(server, serverConfig);
		
		return manager;
	}
	
	private AutoFetchManager createAutoFetchManager(String serverName, ResourceManager resourceManager) {

		File autoFetchFile = getAutoFetchFile(serverName, resourceManager);

		AutoFetchManager autoFetchManager = null;

		boolean readFile = GlobalProperties.getBoolean("autofetch.readfromfile", true);
		if (readFile) {
			autoFetchManager = deserializeAutoFetch(autoFetchFile);
		}

		if (autoFetchManager == null) {
			// not deserialized from file so create as empty
			// It will be populated automatically by querying the
			// database meta data
			autoFetchManager = new DefaultAutoFetchManager(autoFetchFile.getAbsolutePath());
		}
		
		return autoFetchManager;
	}
	
	private AutoFetchManager deserializeAutoFetch(File autoFetchFile) {
		try {
			
			if (!autoFetchFile.exists()) {
				return null;
			}
			FileInputStream fi = new FileInputStream(autoFetchFile);
			ObjectInputStream ois = new ObjectInputStream(fi);
			AutoFetchManager profListener = (AutoFetchManager) ois.readObject();
			
			logger.info("AutoFetch deserialized from file ["+autoFetchFile.getAbsolutePath()+"]");
			
			return profListener;

		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Error loading autofetch file "+autoFetchFile.getAbsolutePath(), ex);
			return null;
		}
	}
	
	/**
	 * Return the file name of the autoFetch meta data.
	 */
	private File getAutoFetchFile(String serverName, ResourceManager resourceManager) {

		String fileName = ".ebean."+serverName+".autofetch";

		File dir = resourceManager.getAutofetchDirectory();

		if (!dir.exists()) {
			// automatically create the directory if it does not exist.
			// this is probably a fairly reasonable thing to do
			if (!dir.mkdirs()) {
				String m = "Unable to create directory [" + dir + "] for autofetch file ["+ fileName + "]";
				throw new PersistenceException(m);
			}
		}

		return new File(dir, fileName);
	}

}
