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
import javax.persistence.Version;

import com.avaje.ebean.annotation.CreatedTimestamp;
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

//
//	@Formula(select="c.name",join="join o_customer c on c.id = ${ta}.kcustomer_id")
//	String custName;
//
//	@Formula(select="c.updtime",join="join o_customer c on c.id = ${ta}.kcustomer_id")
//	Timestamp custUpdtime;
	
	@Id
    Integer id;
    
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
