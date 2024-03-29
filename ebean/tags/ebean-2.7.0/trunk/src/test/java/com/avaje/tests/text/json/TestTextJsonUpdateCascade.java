package com.avaje.tests.text.json;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.Update;
import com.avaje.ebean.text.json.JsonContext;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.OrderDetail;
import com.avaje.tests.model.basic.Product;
import com.avaje.tests.model.basic.ResetBasicData;
import com.avaje.tests.model.basic.Order.Status;

public class TestTextJsonUpdateCascade extends TestCase {

    public void test() {
        
        ResetBasicData.reset();
        
        
        Customer c0  = ResetBasicData.createCustAndOrder("Test Json");
        
        Customer c2 = Ebean.getReference(Customer.class, c0.getId());
        List<Order> orders = c2.getOrders();
        
        assertEquals(1, orders.size());
        
        Order order = orders.get(0);
        int size = order.getDetails().size();
        
        assertTrue(size >= 3);

        Customer cref = Ebean.getReference(Customer.class, c0.getId());
        order.setCustomer(cref);
        order.setStatus(Status.SHIPPED);
        
        OrderDetail orderDetail0 = order.getDetails().get(0);
        orderDetail0.setShipQty(300);
        orderDetail0.setUnitPrice(56.98d);
        
        // remove one of the details...
        OrderDetail removedDetail = order.getDetails().remove(2);
        assertNotNull(removedDetail);
        
        Product p = Ebean.getReference(Product.class, 1);
        OrderDetail newDetail = new OrderDetail(p, 899, 12.12d);
        //newDetail.setOrder(order);
        order.addDetail(newDetail);
        
        EbeanServer server = Ebean.getServer(null);
        
        JsonContext jsonContext = server.createJsonContext();
        String jsonString = jsonContext.toJsonString(order, true);
        
        System.out.println(jsonString);
        
        Order updOrder = jsonContext.toBean(Order.class, jsonString);
        
        Transaction t = server.beginTransaction();
        try {
            // turn of the cascading persist for the moment
            t.setPersistCascade(false);
            
            t.log("-- stateless update of order ... ");
            server.update(updOrder);
        
            List<Integer> detailIds = new ArrayList<Integer>();
            
            t.log("-- insert/update of order details ... ");
            List<OrderDetail> details = updOrder.getDetails();
            for (OrderDetail orderDetail : details) {
                detailIds.add(orderDetail.getId());
                
                if (orderDetail.getUpdtime() == null) {
                    // force insert
                    //server.insert(orderDetail);
                    //orderDetail.setOrder(updOrder);
                    server.save(orderDetail);
                } else {
                    // force update
                    server.update(orderDetail);
                }
            }
            String del = "delete from OrderDetail where order.id=:orderId and id not in (:detailIds)";
            Update<OrderDetail> deleteOtherDetails = server.createUpdate(OrderDetail.class, del);
            deleteOtherDetails.setParameter("orderId", updOrder.getId());
            deleteOtherDetails.setParameter("detailIds", detailIds);
            
            t.log("-- deleting details that are not in list ");
            deleteOtherDetails.execute();
            t.commit();
            
        } finally {
            t.end();
        }
        
    }
    
}
