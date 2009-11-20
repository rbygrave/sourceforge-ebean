package com.avaje.tests.autofetch;

import java.util.List;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class MainAutoQueryTune1 {

	public static void main(String[] args) {
		
		//GlobalProperties.put("ebean.ddl.run", "false");
		//GlobalProperties.put("ebean.ddl.generate", "false");
		GlobalProperties.put("ebean.autofetch.queryTuning", "true");
//		GlobalProperties.put("ebean.autofetch.queryTuningAddVersion", "true");

		ResetBasicData.reset();

		MainAutoQueryTune1 me = new MainAutoQueryTune1();		
		me.tuneQuery();
	}
	
	private void tuneQuery() {
		
		
		Query<Order> query = Ebean.find(Order.class)
			.order().asc("id")
			.setAutofetch(true);

		List<Order> list = query.findList();
		
		for (Order order : list) {
			order.getShipDate();
			// with this modification... tuning should fetch version
			//order.setShipDate(new java.sql.Date(System.currentTimeMillis()));
		}
	
		//String generatedSql = query.getGeneratedSql();
		
	}
}
