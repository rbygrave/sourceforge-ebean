package com.avaje.ebean.enhance.agent;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Helper methods for URL class path conversion.
 */
public class UrlPathHelper {

	private static final String PROTOCAL_PREFIX = "file:/";
	

	/**
	 * Convert string paths into URL class paths.
	 */
	public static URL[] convertToUrl(String[] paths){
		ArrayList<URL> list = new ArrayList<URL>();
		for (int i = 0; i < paths.length; i++) {
			list.add(convertToUrl(paths[i]));
		}
		return list.toArray(new URL[list.size()]);
	}
	
	/**
	 * Convert string path into URL class path.
	 */
	public static URL convertToUrl(String path) {
		try {
			return new URL(PROTOCAL_PREFIX + convertUrlString(path.trim()));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Convert a string path to be used in URL class path entry.
	 */
	public static String convertUrlString(String classpath) {
		if (!classpath.endsWith("/")) {
			File file = new File(classpath);
			if (file.exists() && file.isDirectory()) {
				classpath = classpath.concat("/");
			}
		}
		return classpath;
	}
}
