package com.avaje.ebean.util;

/**
 * An exception thrown when processing the deployment information.
 */
public class DeploymentException extends RuntimeException {

	private static final long serialVersionUID = -605415276643625853L;

	public DeploymentException(String msg) {
		super(msg);
	}

	public DeploymentException(String msg, Throwable e) {
		super(msg, e);
	}

}
