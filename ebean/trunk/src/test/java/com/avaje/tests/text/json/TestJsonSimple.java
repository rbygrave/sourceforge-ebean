package com.avaje.tests.text.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;

import junit.framework.TestCase;

import com.avaje.ebean.text.json.JsonElement;
import com.avaje.ebeaninternal.server.text.json.Json;

public class TestJsonSimple extends TestCase {

	public void test() throws IOException {
		
		InputStream is  = this.getClass().getResourceAsStream("/example1.json");
		
		final Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		LineNumberReader lineReader = new LineNumberReader(reader);
		
		String readLine = null;
		
		StringBuilder sb = new StringBuilder();
		while((readLine = lineReader.readLine()) != null){
			sb.append(readLine);
		}
		
		String jsonText = sb.toString();
		
		JsonElement el = Json.parse(jsonText);
		
		System.out.println("Got "+el);
	}
	
	
	
}
