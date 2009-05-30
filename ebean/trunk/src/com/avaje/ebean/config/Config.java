package com.avaje.ebean.config;

public class Config {

	Boolean ddlGenerate;
	
	Boolean ddlRun;
	
	public void setDdlGenerate(boolean ddlGenerate) {
		this.ddlGenerate = ddlGenerate;
	}
	
	public void setDdlRun(boolean ddlRun) {
		this.ddlRun = ddlRun;
	}
	
	
	public void apply() {
		if (ddlGenerate != null){
			GlobalProperties.setProperty("ebean.ddl.generate", ddlGenerate.toString());
		}
		if (ddlRun != null){
			GlobalProperties.setProperty("ebean.ddl.run", ddlRun.toString());
		}
	}
}
