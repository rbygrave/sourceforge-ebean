package com.avaje.tests.query;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Junction;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.ResetBasicData;
import junit.framework.TestCase;

import java.util.List;

public class TestLimitQuery extends TestCase {

	public void testHasManyWithLimit()
	{
		ResetBasicData.reset();

		Query<Customer> query = Ebean.find(Customer.class);
		query.setAutofetch(false);
		query.setFirstRow(1);
		query.setMaxRows(10);

		Junction junc = Expr.disjunction();
		junc.add(Expr.like("name", "%a%"));
		query.where(junc);

		List<Customer> customer = query.findList();
		assertTrue(customer.size() > 0); // should at least find the "Cust NoAddress" customer
	}
}