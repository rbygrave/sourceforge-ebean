package com.avaje.tests.text.json;

import java.io.IOException;
import java.util.Map;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.text.json.JsonContext;
import com.avaje.ebean.text.json.JsonWriteOptions;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestJsonMap extends TestCase {

	public void test() throws IOException {
		
		ResetBasicData.reset();
        
		Map<String, Customer> map = Ebean.find(Customer.class)
			.findMap("id", String.class);
				
		JsonContext jsonContext = Ebean.createJsonContext();
		JsonWriteOptions jsonWriteOptions = JsonWriteOptions.parsePath("(id,status,name)");
		
		String jsonString = jsonContext.toJsonString(map, true, jsonWriteOptions);
		System.out.println(jsonString);
		//Assert.assertTrue(jsonString.indexOf("{\"1\":") > -1);
		//Assert.assertTrue(jsonString.indexOf("{\"id\":1,\"status\":\"NEW\",\"name\":\"Rob\"},") > -1);
	}
	
	
	
}
