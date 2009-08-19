package com.avaje.ebean.internal;


public interface BackgroundExecutor {

	public void execute(Runnable r);
	
	//public void executePeriodically(Runnable r, long delay, TimeUnit unit);
}
