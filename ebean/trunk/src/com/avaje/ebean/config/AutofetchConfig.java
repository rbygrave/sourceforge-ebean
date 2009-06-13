package com.avaje.ebean.config;


public class AutofetchConfig {

	AutofetchMode mode = AutofetchMode.DEFAULT_ONIFEMPTY;
	
	boolean queryTuning = true;
	
	boolean profiling = true;
	
	int profilingMin = 1;
	
	int profilingBase = 10;
	
	double profilingRate = 0.05; 
	
	boolean useFileLogging = true;
	
	String logDirectory;
	
	int profileUpdateFrequency = 60;
	
	int garbageCollectionWait = 100;
	
	public AutofetchMode getMode() {
		return mode;
	}

	public void setMode(AutofetchMode mode) {
		this.mode = mode;
	}

	public boolean isQueryTuning() {
		return queryTuning;
	}

	public void setQueryTuning(boolean queryTuning) {
		this.queryTuning = queryTuning;
	}

	public boolean isProfiling() {
		return profiling;
	}

	public void setProfiling(boolean profiling) {
		this.profiling = profiling;
	}

	public int getProfilingMin() {
		return profilingMin;
	}

	public void setProfilingMin(int profilingMin) {
		this.profilingMin = profilingMin;
	}


	public int getProfilingBase() {
		return profilingBase;
	}


	public void setProfilingBase(int profilingBase) {
		this.profilingBase = profilingBase;
	}


	public double getProfilingRate() {
		return profilingRate;
	}


	public void setProfilingRate(double profilingRate) {
		this.profilingRate = profilingRate;
	}


	public boolean isUseFileLogging() {
		return useFileLogging;
	}


	public void setUseFileLogging(boolean useFileLogging) {
		this.useFileLogging = useFileLogging;
	}

	public String getLogDirectory() {
		return logDirectory;
	}

	public void setLogDirectory(String logDirectory) {
		this.logDirectory = logDirectory;
	}

	public int getProfileUpdateFrequency() {
		return profileUpdateFrequency;
	}


	public void setProfileUpdateFrequency(int profileUpdateFrequency) {
		this.profileUpdateFrequency = profileUpdateFrequency;
	}


	public int getGarbageCollectionWait() {
		return garbageCollectionWait;
	}

	public void setGarbageCollectionWait(int garbageCollectionWait) {
		this.garbageCollectionWait = garbageCollectionWait;
	}

	/**
	 * Load the settings from the properties file.
	 */
	public void loadSettings(ConfigPropertyMap p){
		

		queryTuning = p.getBoolean("autofetch.querytuning", true);
		profiling = p.getBoolean("autofetch.profiling", true);
		mode = p.getEnum(AutofetchMode.class, "implicitmode", AutofetchMode.DEFAULT_ONIFEMPTY);
		
		profilingMin = p.getInt("autofetch.profiling.min", 1);
		profilingBase = p.getInt("autofetch.profiling.base", 10);		

		String rate = p.get("autofetch.profiling.rate", "0.05");
		profilingRate = Double.parseDouble(rate);
		
		useFileLogging = p.getBoolean("autofetch.useFileLogging", true);
		profileUpdateFrequency = p.getInt("autofetch.profiling.updatefrequency", 60);
		
	}
}
