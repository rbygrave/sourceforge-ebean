package com.avaje.ebean.server.jmx;

import java.util.logging.Logger;

import com.avaje.ebean.AdminAutofetch;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.config.AutofetchMode;
import com.avaje.ebean.server.autofetch.AutoFetchManager;

/**
 * Implementation of the AutoFetchControl.
 * <p>
 * This is accessible via {@link EbeanServer#getServerControl()} or via JMX
 * MBeans.
 * </p>
 */
public class MAdminAutofetch implements MAdminAutofetchMBean, AdminAutofetch {

	final Logger logger = Logger.getLogger(MAdminAutofetch.class.getName());

	final AutoFetchManager autoFetchManager;

	final String modeOptions;

	public MAdminAutofetch(AutoFetchManager autoFetchListener) {
		this.autoFetchManager = autoFetchListener;
		this.modeOptions = AutofetchMode.DEFAULT_OFF + ", "
				+ AutofetchMode.DEFAULT_ON + ", "
				+ AutofetchMode.DEFAULT_ONIFEMPTY;
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

	public String getModeOptions() {
		return modeOptions;
	}

	public String getMode() {
		return autoFetchManager.getMode().name();
	}

	public void setMode(String implicitMode) {
		try {
			AutofetchMode mode = AutofetchMode.valueOf(implicitMode);
			autoFetchManager.setMode(mode);
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
	
	public void clearQueryStatistics() {
		autoFetchManager.clearQueryStatistics();
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
