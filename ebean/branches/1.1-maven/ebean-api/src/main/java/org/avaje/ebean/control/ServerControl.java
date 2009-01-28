package org.avaje.ebean.control;

/**
 * Provides control over the services run by an EbeanServer.
 */
public interface ServerControl {

	/**
	 * Return the LogControl.
	 */
	public LogControl getLogControl();
	
	/**
	 * Return the ProfileControl.
	 */
	public AutoFetchControl getAutoFetchControl();
}
