package com.avaje.tests.model.basic;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.avaje.ebean.annotation.EnumMapping;
import com.avaje.ebean.annotation.Where;
import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotNull;

/**
 * Customer entity bean.
 */
@Entity
@Table(name="o_customer")
public class Customer extends BasicDomain { 

	private static final long serialVersionUID = 1L;

	/**
	 * EnumMapping is an Ebean specific mapping for enums.
	 */
	@EnumMapping(nameValuePairs="NEW=N,ACTIVE=A,INACTIVE=I")
	public enum Status {
		NEW,
		ACTIVE,
		INACTIVE
	}
	
	@Transient 
	Boolean selected;

	@Transient
	ReentrantLock lock = new ReentrantLock();
	
    Status status;
    
    @NotNull
    @Length(max=40)
    //@Column(length=39,nullable=false)
    String name;

    @ManyToOne(cascade=CascadeType.ALL)
    Address billingAddress;

    @ManyToOne(cascade=CascadeType.ALL)
    Address shippingAddress;

    @OneToMany(mappedBy="customer")
    @Where(clause="${ta}.order_date is not null")
    List<Order> orders;


    /**
     * Return name.
     */    
    public String getName() {
  	    return name;
    }

    /**
     * Set name.
     */    
    public void setName(String name) {
  	    this.name = name;
    }

    /**
     * Return billing address.
     */    
    public Address getBillingAddress() {
  	    return billingAddress;
    }

    /**
     * Set billing address.
     */    
    public void setBillingAddress(Address billingAddress) {
  	    this.billingAddress = billingAddress;
    }

    /**
     * Return status.
     */    
    public Status getStatus() {
  	    return status;
    }

    /**
     * Set status.
     */    
    public void setStatus(Status status) {
  	    this.status = status;
    }

    /**
     * Return shipping address.
     */    
    public Address getShippingAddress() {
  	    return shippingAddress;
    }

    /**
     * Set shipping address.
     */    
    public void setShippingAddress(Address shippingAddress) {
  	    this.shippingAddress = shippingAddress;
    }

    /**
     * Return orders.
     */    
    public List<Order> getOrders() {
  	    return orders;
    }

    /**
     * Set orders.
     */    
    public void setOrders(List<Order> orders) {
  	    this.orders = orders;
    }

	public Boolean getSelected() {
		return selected;
	}

	public void setSelected(Boolean selected) {
		this.selected = selected;
	}

	public ReentrantLock getLock() {
		return lock;
	}
	
	
}
