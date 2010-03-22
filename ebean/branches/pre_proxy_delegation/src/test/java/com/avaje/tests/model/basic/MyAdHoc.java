package com.avaje.tests.model.basic;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.avaje.ebean.annotation.Sql;
import com.avaje.ebean.annotation.SqlSelect;

@Entity
@Sql(select={
  @SqlSelect(query="select order_id, count(*) from o_order_detail group by order_id"
      ,columnMapping="order_id order, count(*) detailCount")      
})
public class MyAdHoc {

    @ManyToOne
    Order order;
    
    int detailCount;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public int getDetailCount() {
        return detailCount;
    }

    public void setDetailCount(int detailCount) {
        this.detailCount = detailCount;
    }
    
}
