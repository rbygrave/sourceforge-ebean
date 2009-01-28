package com.avaje.ebean.server.deploy;

import java.util.HashMap;
import java.util.Map;

public class TestDeploy {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		Map<String,String> m = new HashMap<String,String>();
		m.put("user","f_user");
		m.put("title","db_title");
		m.put("id","db_id");
		m.put("postcount","db_post_count");
		//m.put("status.code","db_status_code");

	
		DeployUpdateParser p = new DeployUpdateParser(m);
		
		String o = p.parse("update topic set title = :title,postCount = :count where id = :id");
		//"update User k k.status .code = code set k.title = :title where k.id = :id");
		
		System.out.println(o);
	}

}
