package com.avaje.ebean.server.jmx;

import com.avaje.ebean.control.LogControl;
import com.avaje.ebean.control.AutoFetchControl;
import com.avaje.ebean.control.ServerControl;

public class MServerControl implements ServerControl {

	final LogControl logControl;
	
	final AutoFetchControl autoFetchControl;
	
	public MServerControl(LogControl logControl, AutoFetchControl autoFetchControl) {
		this.logControl = logControl;
		this.autoFetchControl = autoFetchControl;
	}

	public LogControl getLogControl() {
		return logControl;
	}

	public AutoFetchControl getAutoFetchControl() {
		return autoFetchControl;
	}
	
}
