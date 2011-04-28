package com.avaje.tests.basic.one2one;

import com.avaje.ebean.Ebean;

import junit.framework.TestCase;

public class TestOne2OneBookingInvoice extends TestCase {
	
	public void test() {
		Booking b = new Booking();

		Invoice ai = new Invoice();
		Invoice ci = new Invoice();

		ai.setBooking(b);
		ci.setBooking(b);

		b.setAgentInvoice(ai);
		b.setClientInvoice(ci);

		Ebean.save(b);
		Ebean.save(ai);
		Ebean.save(ci);
		Ebean.save(b);
		
		Booking b1 = Ebean.find(Booking.class, b.getId());
		
		Invoice ai1 = b1.getAgentInvoice();
		Booking b2 = ai1.getBooking();
		Invoice ci1 = b1.getClientInvoice();

		System.out.println("New booking id: " + b.getId());
//		System.out.println("Booking has " + b.getInvoices().size() + " tires");
	}
}
