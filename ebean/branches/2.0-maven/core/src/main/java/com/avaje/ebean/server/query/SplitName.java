package com.avaje.ebean.server.query;

public class SplitName {

	public static String add(String prefix, String name){
		if (prefix != null){
			return prefix+"."+name;
		} else {
			return name;
		}
	}
	
	public static String[] split(String name){
		
		int pos = name.lastIndexOf('.');
		if (pos == -1){
			return new String[]{null, name};
		} else {
			String s0 = name.substring(0, pos);
			String s1 = name.substring(pos+1);
			return new String[]{s0,s1};
		}
	}
	
}
