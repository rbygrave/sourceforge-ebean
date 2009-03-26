package com.avaje.eclipse.buildplugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.avaje.eclipse.buildplugin.preferences.PreferenceConstants;

/**
 * The activator controlling the plug-in life cycle
 */
public class BuildPluginActivator extends AbstractUIPlugin {

	public static final String VERSION = "1.1.0";
	
	// The plug-in ID
	public static final String PLUGIN_ID = "com.avaje.ebean.enhancer.plugin";

	// The shared instance
	private static BuildPluginActivator plugin;

	public BuildPluginActivator() {
	}

	public static PrintStream createTransformLog() {
		
		IPath stateLocation = plugin.getStateLocation();
		IPath path = stateLocation.addTrailingSeparator().append("enhance.log");
		try {
			
			File logFile = path.toFile();
			if (!logFile.exists()){
				logFile.createNewFile();
			}
			
			// append to the file...
			FileOutputStream fos = new FileOutputStream(logFile, true);
			return new PrintStream(fos);
			
		} catch (IOException e){
			logError("Error creating log file ["+path.toString()+"]", e);
			return System.out;
		}
	}

	public static void logError(String msg, Exception e) {
		ILog log = plugin.getLog();
		log.log(new Status(Status.ERROR, PLUGIN_ID, Status.OK, msg, e));
	}

	public static void logInfo(String msg) {
		logInfo(msg, null);
	}
	
	public static void logInfo(String msg, Exception e) {
		ILog log = plugin.getLog();
		log.log(new Status(Status.INFO, PLUGIN_ID, Status.OK, msg, e));
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

	}

	public void stop(BundleContext context) throws Exception {

		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static BuildPluginActivator getDefault() {
		return plugin;
	}

	public static int getDebugLevel() {
		if (plugin == null) {
			return 0;
		}

		IPreferenceStore store = plugin.getPreferenceStore();
		return store.getInt(PreferenceConstants.P_PLUGIN_DEBUG_LEVEL);
	}

	public static int getEnhanceDebugLevel() {
		if (plugin == null) {
			return 0;
		}

		IPreferenceStore store = plugin.getPreferenceStore();
		return store.getInt(PreferenceConstants.P_ENHANCE_DEBUG_LEVEL);
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
