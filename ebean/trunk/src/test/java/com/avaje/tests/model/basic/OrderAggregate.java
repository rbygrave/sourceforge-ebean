package com.avaje.tests.model.basic;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

import com.avaje.ebean.annotation.Sql;

@Entity
@Sql
public class OrderAggregate {

    @OneToOne
	Order order;
	
	Double totalAmount;
	
	Double totalItems;
    
	public String toString() {
	    return order.getId()+" totalAmount:"+totalAmount+" totalItems:"+totalItems;
	}
	
	public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(Double totalAmount) {
		this.totalAmount = totalAmount;
	}

    public Double getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Double totalItems) {
        this.totalItems = totalItems;
    }
}
