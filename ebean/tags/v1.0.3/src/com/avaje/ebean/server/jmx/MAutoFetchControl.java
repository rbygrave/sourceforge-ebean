package com.avaje.ebean.server.jmx;

import java.util.logging.Logger;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.control.AutoFetchControl;
import com.avaje.ebean.control.ImplicitAutoFetchMode;
import com.avaje.ebean.server.autofetch.AutoFetchManager;

/**
 * Implementation of the AutoFetchControl.
 * <p>
 * This is accessible via {@link EbeanServer#getServerControl()} or via JMX
 * MBeans.
 * </p>
 */
public class MAutoFetchControl implements MAutoFetchControlMBean, AutoFetchControl {

	final Logger logger = Logger.getLogger(MAutoFetchControl.class.getName());

	final AutoFetchManager autoFetchManager;

	final String implicitModeOptions;

	public MAutoFetchControl(AutoFetchManager autoFetchListener) {
		this.autoFetchManager = autoFetchListener;
		this.implicitModeOptions = ImplicitAutoFetchMode.DEFAULT_OFF + ", "
				+ ImplicitAutoFetchMode.DEFAULT_ON + ", "
				+ ImplicitAutoFetchMode.DEFAULT_ON_IF_EMPTY;
	}

	public boolean isQueryTuning() {
		return autoFetchManager.isQueryTuning();
	}

	public void setQueryTuning(boolean enable) {
		autoFetchManager.setQueryTuning(enable);
	}

	public boolean isProfiling() {
		return autoFetchManager.isProfiling();
	}

	public void setProfiling(boolean enable) {
		autoFetchManager.setProfiling(enable);
	}

	public String getImplicitModeOptions() {
		return implicitModeOptions;
	}

	public String getImplicitMode() {
		return autoFetchManager.getImplicitAutoFetchMode().name();
	}

	public void setImplicitMode(String implicitMode) {
		try {
			ImplicitAutoFetchMode mode = ImplicitAutoFetchMode.valueOf(implicitMode);
			autoFetchManager.setImplicitAutoFetchMode(mode);
		} catch (Exception e) {
			logger.info("Invalid implicit mode attempted "+e.getMessage());
		}
	}

	public String collectUsageViaGC() {
		return autoFetchManager.collectUsageViaGC(-1);
	}

	public double getProfilingRate() {
		return autoFetchManager.getProfilingRate();
	}

	public void setProfilingRate(double rate) {
		autoFetchManager.setProfilingRate(rate);
	}

	public int getProfilingMin() {
		return autoFetchManager.getProfilingMin();
	}

	public int getProfilingBase() {
		return autoFetchManager.getProfilingBase();
	}

	public void setProfilingMin(int profilingMin) {
		autoFetchManager.setProfilingMin(profilingMin);
	}

	public void setProfilingBase(int profilingMax) {
		autoFetchManager.setProfilingBase(profilingMax);
	}

	public String updateTunedQueryInfo() {
		return autoFetchManager.updateTunedQueryInfo();
	}

	public int clearProfilingInfo() {
		return autoFetchManager.clearProfilingInfo();
	}

	public int clearTunedQueryInfo() {
		return autoFetchManager.clearTunedQueryInfo();
	}

	public int getTotalProfileSize() {
		return autoFetchManager.getTotalProfileSize();
	}

	public int getTotalTunedQueryCount() {
		return autoFetchManager.getTotalTunedQueryCount();
	}

	public int getTotalTunedQuerySize() {
		return autoFetchManager.getTotalTunedQuerySize();
	}

}
