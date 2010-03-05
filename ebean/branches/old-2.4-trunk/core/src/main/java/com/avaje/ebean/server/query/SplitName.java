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
		return split(name, true);
	}
	
	public static String[] splitBegin(String name){
		return split(name, false);
	}
	
	private static String[] split(String name, boolean last){
		
		int pos =  last ? name.lastIndexOf('.') : name.indexOf('.');
		if (pos == -1){
			if (last){
				return new String[]{null, name};
			} else {
				return new String[]{name, null};
			}
		} else {
			String s0 = name.substring(0, pos);
			String s1 = name.substring(pos+1);
			return new String[]{s0,s1};
		}
	}

}
