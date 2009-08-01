package com.avaje.tests.model.basic;

import java.util.ArrayList;
import java.util.List;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.TxRunnable;

public class ResetBasicData {

	public static void reset() {
		
		final ResetBasicData me = new ResetBasicData();
		
		Ebean.execute(new TxRunnable() {
			public void run() {
				me.deleteAll();
				me.insertCountries();
				me.insertProducts();
				me.insertTestCustAndOrders();
			}
		});
	}
	
	
	
	
	public void deleteAll() {
		Ebean.execute(new TxRunnable() {
			public void run() {
				// orm update use bean name and bean properties
				Ebean.createUpdate(OrderDetail.class, "delete from orderDetail")
					.execute();
				
				Ebean.createUpdate(Order.class,"delete from order")
					.execute();
	
				Ebean.createUpdate(Address.class,"delete from address")
					.execute();
	
				Ebean.createUpdate(Customer.class,"delete from Customer")
					.execute();
	
				// sql update uses table and column names
				Ebean.createSqlUpdate("delete from o_country")
					.execute();
	
				Ebean.createSqlUpdate("delete from o_product")
					.execute();
			
			}
		});
	}
	
	
	public void insertCountries() {
		
		Ebean.execute(new TxRunnable() {
			public void run() {
				Country c = new Country();
				c.setCode("NZ");
				c.setName("New Zealand");
				Ebean.save(c);
				
				Country au = new Country();
				au.setCode("AU");
				au.setName("Australia");
				Ebean.save(au);				
			}
		});
	}
	

	public void insertProducts() {
		
		Ebean.execute(new TxRunnable() {
			public void run() {
				Product p = new Product();
				p.setId(1);
				p.setName("Chair");
				p.setSku("C001");
				Ebean.save(p);
		
				p = new Product();
				p.setId(2);
				p.setName("Desk");
				p.setSku("DSK1");
				Ebean.save(p);
		
				p = new Product();
				p.setId(3);
				p.setName("Computer");
				p.setSku("C002");
				Ebean.save(p);
		
				p = new Product();
				p.setId(4);
				p.setName("Printer");
				p.setSku("C003");
				Ebean.save(p);
			}
		});
	}
	
	public void insertTestCustAndOrders() {

		Ebean.execute(new TxRunnable() {
			public void run() {
				Customer cust1 = insertCustomerNoAddress();
				Customer cust2 = insertCustomer();
			
				createOrder1(cust1);
				createOrder2(cust2);
				createOrder3(cust1);
			}
		});	
	}

	private Customer insertCustomerNoAddress() {
		
		Customer c = new Customer();
		c.setName("Cust NoAddress");
		c.setStatus(Customer.Status.NEW);

		Ebean.save(c);
		return c;
	}
	
	private Customer insertCustomer() {
		
		Customer c = new Customer();
		c.setName("Rob");
		c.setStatus(Customer.Status.NEW);
		
		Address shippingAddr = new Address();
		shippingAddr.setLine1("1 Banana St");
		shippingAddr.setLine2("Sandringham");
		shippingAddr.setCity("Auckland");
		shippingAddr.setCountry(Ebean.getReference(Country.class, "NZ"));
		
		c.setShippingAddress(shippingAddr);
		
		Address billingAddr = new Address();
		billingAddr.setLine1("P.O.Box 1234");
		billingAddr.setLine2("Sandringham");
		billingAddr.setCity("Auckland");
		billingAddr.setCountry(Ebean.getReference(Country.class, "NZ"));
		
		c.setBillingAddress(billingAddr);
		
		Ebean.save(c);
		
		return c;
	}
	
	private void createOrder1(Customer customer) {
		
		Product product1 = Ebean.getReference(Product.class, 1);
		Product product2 = Ebean.getReference(Product.class, 2);
		Product product3 = Ebean.getReference(Product.class, 3);
			
		
		Order order = new Order();
		order.setCustomer(customer);
		
		List<OrderDetail> details = new ArrayList<OrderDetail>();
		details.add(new OrderDetail(product1, 5));
		details.add(new OrderDetail(product2, 3));
		details.add(new OrderDetail(product3, 1));
		order.setDetails(details);
		
		
		order.add(new OrderShipment());
		
		Ebean.save(order);
	}

	private void createOrder2(Customer customer) {
		
		Product product1 = Ebean.getReference(Product.class, 1);
					
		Order order = new Order();
		order.setCustomer(customer);
		
		List<OrderDetail> details = new ArrayList<OrderDetail>();
		details.add(new OrderDetail(product1, 4));
		order.setDetails(details);
		
		order.add(new OrderShipment());

		Ebean.save(order);
	}

	private void createOrder3(Customer customer) {
		
		Product product1 = Ebean.getReference(Product.class, 1);
		Product product3 = Ebean.getReference(Product.class, 3);
					
		Order order = new Order();
		order.setCustomer(customer);
		
		List<OrderDetail> details = new ArrayList<OrderDetail>();
		details.add(new OrderDetail(product1, 3));
		details.add(new OrderDetail(product3, 40));
		order.setDetails(details);
		
		order.add(new OrderShipment());

		Ebean.save(order);
	}
}
