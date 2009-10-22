package com.avaje.tests.model.basic;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.avaje.ebean.annotation.CreatedTimestamp;
import com.avaje.ebean.annotation.Formula;
import com.avaje.ebean.annotation.Sql;
import com.avaje.ebean.annotation.SqlSelect;
import com.avaje.ebean.validation.NotNull;

/**
 * Order entity bean.
 */
@Entity
@Table(name="o_order")
@Sql(select={
  @SqlSelect(name="test",query="select id, status from o_customer u", tableAlias="u"),
  @SqlSelect(name="test2",extend="test",where="u.status = :status")
})
public class Order implements Serializable {

	private static final long serialVersionUID = 1L;

	//@EnumMapping(nameValuePairs="APPROVED=A, COMPLETE=C, NEW=N, SHIPPED=S")
	public enum Status {
		NEW,
		APPROVED,
		SHIPPED,
		COMPLETE
	}
	
	public Order(){
		
	}

	
	@Id
    Integer id;

	/**
	 * Derived total amount from the order details. Needs to be explicitly included in query as Transient.
	 * Removing the Transient would mean by default it would be included in a order query.
	 * <p>
	 * NOTE: The join clause for totalAmount and totalItems is the same. If your query includes both 
	 * totalAmount and totalItems only the one join is added to the query.
	 * </p>
	 */
	@Transient
	@Formula(
		select="z_b${ta}.total_amount",
		join="join (select order_id, count(*) as total_items, sum(order_qty*unit_price) as total_amount from o_order_detail group by order_id) z_b${ta} on z_b${ta}.order_id = ${ta}.id")
	Double totalAmount;

	/**
	 * Derived total item count from the order details. Needs to be explicitly included in query as Transient.
	 */
	@Transient
	@Formula(
		select="z_b${ta}.total_items",
		join="join (select order_id, count(*) as total_items, sum(order_qty*unit_price) as total_amount from o_order_detail group by order_id) z_b${ta} on z_b${ta}.order_id = ${ta}.id")
	Integer totalItems;
	
	@Enumerated(value=EnumType.ORDINAL)
    Status status = Status.NEW;
    
    Date orderDate = new Date(System.currentTimeMillis());

    Date shipDate;

    @NotNull
    @ManyToOne
    @JoinColumn(name="kcustomer_id")
    Customer customer;
    
    @CreatedTimestamp
    Timestamp cretime;

    @Version
    Timestamp updtime;

    @OneToMany(cascade=CascadeType.ALL, mappedBy="order")
    List<OrderDetail> details = new ArrayList<OrderDetail>();
    
    @OneToMany(cascade=CascadeType.ALL, mappedBy="order")
    List<OrderShipment> shipments;
    
    public String toString() {
    	return id+" totalAmount:"+totalAmount+" totalItems:"+totalItems;
    }
    
	/**
     * Return id.
     */    
    public Integer getId() {
  	    return id;
    }

    /**
     * Set id.
     */    
    public void setId(Integer id) {
  	    this.id = id;
    }

    public Double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(Double totalAmount) {
		this.totalAmount = totalAmount;
	}
	
	public Integer getTotalItems() {
		return totalItems;
	}

	public void setTotalItems(Integer totalItems) {
		this.totalItems = totalItems;
	}

	/**
     * Return order date.
     */    
    public Date getOrderDate() {
  	    return orderDate;
    }

    /**
     * Set order date.
     */    
    public void setOrderDate(Date orderDate) {
  	    this.orderDate = orderDate;
    }

    /**
     * Return ship date.
     */    
    public Date getShipDate() {
  	    return shipDate;
    }

    /**
     * Set ship date.
     */    
    public void setShipDate(Date shipDate) {
  	    this.shipDate = shipDate;
    }

    /**
     * Return cretime.
     */    
    public Timestamp getCretime() {
  	    return cretime;
    }

    /**
     * Set cretime.
     */    
    public void setCretime(Timestamp cretime) {
  	    this.cretime = cretime;
    }

    /**
     * Return updtime.
     */    
    public Timestamp getUpdtime() {
  	    return updtime;
    }

    /**
     * Set updtime.
     */    
    public void setUpdtime(Timestamp updtime) {
  	    this.updtime = updtime;
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
     * Return customer.
     */    
    public Customer getCustomer() {
  	    return customer;
    }

    /**
     * Set customer.
     */    
    public void setCustomer(Customer customer) {
  	    this.customer = customer;
    }

    /**
     * Return details.
     */    
    public List<OrderDetail> getDetails() {
  	    return details;
    }

    /**
     * Set details.
     */    
    public void setDetails(List<OrderDetail> details) {
  	    this.details = details;
    }

	public List<OrderShipment> getShipments() {
		return shipments;
	}

	public void setShipments(List<OrderShipment> shipments) {
		this.shipments = shipments;
	}
	
	public void add(OrderShipment shipment){
		
		if (shipments == null){
			shipments = new ArrayList<OrderShipment>();	
		}
		
		shipments.add(shipment);		
	}
}
