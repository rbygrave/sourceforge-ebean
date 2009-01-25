package app.model.order.otherschema;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(schema="other", name="or_order_detail")
public class OtherOrderDetail {

	@Id
	int id;
	
	String productName;
	
	int orderQty;
	
	int shipQty;

	@ManyToOne
	OtherOrder order;
	
	Timestamp cretime;
	
	@Version
	Timestamp updtime;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public int getOrderQty() {
		return orderQty;
	}

	public void setOrderQty(int orderQty) {
		this.orderQty = orderQty;
	}

	public int getShipQty() {
		return shipQty;
	}

	public void setShipQty(int shipQty) {
		this.shipQty = shipQty;
	}

	public OtherOrder getOrder() {
		return order;
	}

	public void setOrder(OtherOrder order) {
		this.order = order;
	}

	public Timestamp getCretime() {
		return cretime;
	}

	public void setCretime(Timestamp cretime) {
		this.cretime = cretime;
	}

	public Timestamp getUpdtime() {
		return updtime;
	}

	public void setUpdtime(Timestamp updtime) {
		this.updtime = updtime;
	}
	
	
	
}
