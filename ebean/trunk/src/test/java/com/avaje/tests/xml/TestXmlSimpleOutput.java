package com.avaje.tests.xml;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
        
        XomBuilder builder = new XomBuilder(beanDescriptor, null);
        
        builder.addElement("id").addAttribute("status");
        builder.addElement("cretime","created-ts");
        builder.addElement("orderDate","ship-date");
        //builder.addCollection("details","order-details");
        
        ElPropertyValue elId = beanDescriptor.getElGetValue("id");
        ElPropertyValue elSt = beanDescriptor.getElGetValue("status");
        ElPropertyValue eldate = beanDescriptor.getElGetValue("orderDate");
        ElPropertyValue elcre = beanDescriptor.getElGetValue("cretime");
        ElPropertyValue elDetails = beanDescriptor.getElGetValue("details");

        XoAttribute xst = new XoPropAttribute("status", elSt);
        XoNode xid = new XoPropNode("id", elId, null, null, null, new XoAttribute[]{xst});
        XoNode xdt = new XoPropNode("ship-date", eldate);
        XoNode xcr = new XoPropNode("created-ts", elcre);


        BeanDescriptor<OrderDetail> detailDescriptor = server.getBeanDescriptor(OrderDetail.class);

        XoNode detailId = new XoPropNode("id",detailDescriptor.getElGetValue("id"));
        XoNode detailProdName = new XoPropNode("product-name",detailDescriptor.getElGetValue("product.name"));
        
        //XoCompoundNode details = new XoCompoundNode("line", detailId, detailProdName);
        XoPropNode details = new XoPropNode("line", detailId, detailProdName);
        
        
        XoPropCollection xlist = new XoPropCollection("order-details", elDetails, details, true);

        
        XoCompoundNode orderNode = new XoCompoundNode("order",xid, xcr, xdt, xlist);

        
        List<Order> list = Ebean.find(Order.class)
            .join("details")
            .join("details.product","name")
            .findList();
        
        Order bean = list.get(0);
        
        Writer writer = new StringWriter();
        XmlOutputWriter output = new XmlOutputWriter(writer);
        
        orderNode.writeNode(output, bean);
        
        System.out.println(writer);
        
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        
        XmlOutputDocument docOut = new XmlOutputDocument(doc);
        
        
        orderNode.writeNode(docOut, null, bean);
        
        System.out.println(docOut.getDocument().getDocumentElement());
        
        XoWriteContext ctx = new XoWriteContext();
        
        Document document = docOut.getDocument();
        NodeList childNodes = document.getChildNodes();
        int len = childNodes.getLength();
        for (int i = 0; i < len; i++) {
            ctx.setBean(new Order());
            Node childNode = childNodes.item(i);
            orderNode.readNode(childNode, ctx);
            Order o = (Order)ctx.getBean();
            System.out.println(o);
            System.out.println(o.getDetails());
            
            
        }
        
        
    }
    
}
