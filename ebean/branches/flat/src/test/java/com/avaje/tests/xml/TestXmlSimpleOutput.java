package com.avaje.tests.xml;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.w3c.dom.Document;

import com.avaje.ebean.Ebean;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.OrderDetail;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestXmlSimpleOutput extends TestCase {

    public void test() throws Exception {
        
        ResetBasicData.reset();
        
        SpiEbeanServer server = (SpiEbeanServer)Ebean.getServer(null);
        
        BeanDescriptor<Order> beanDescriptor = server.getBeanDescriptor(Order.class);
        
        ElPropertyValue elId = beanDescriptor.getElGetValue("id");
        ElPropertyValue elSt = beanDescriptor.getElGetValue("status");
        ElPropertyValue eldate = beanDescriptor.getElGetValue("orderDate");
        ElPropertyValue elcre = beanDescriptor.getElGetValue("cretime");
        ElPropertyValue elDetails = beanDescriptor.getElGetValue("details");

        XoAttribute xst = new XoPropAttribute("status", elSt, null);
        XoNode xid = new XoPropNode("id", elId, null, xst);
        XoNode xdt = new XoPropNode("ship-date", eldate, null);
        XoNode xcr = new XoPropNode("created-ts", elcre, null);


        BeanDescriptor<OrderDetail> detailDescriptor = server.getBeanDescriptor(OrderDetail.class);

        XoNode detailId = new XoPropNode("id",detailDescriptor.getElGetValue("id"), null);
        XoNode detailProdName = new XoPropNode("product-name",detailDescriptor.getElGetValue("product.name"), null);
        
        XoCompoundNode details = new XoCompoundNode("line", detailId, detailProdName);
        
        XoPropCollection xlist = new XoPropCollection("order-details", elDetails, details, true);

        
        XoCompoundNode orderNode = new XoCompoundNode("order",xid, xcr, xdt, xlist);

        
        List<Order> list = Ebean.find(Order.class)
            .join("details")
            .join("details.product","name")
            .findList();
        
        Order bean = list.get(0);
        
        Writer writer = new StringWriter();
        XmlWriterOutput output = new XmlWriterOutput(writer);
        
        orderNode.writeNode(output, bean);
        
        System.out.println(writer);
        
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        
        XmlDocumentOutput docOut = new XmlDocumentOutput(doc);
        
        
        orderNode.writeNode(docOut, null, bean);
        
        System.out.println(docOut.getDocument().getDocumentElement());
        
    }
    
}
