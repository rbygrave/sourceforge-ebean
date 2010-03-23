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
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestXmlSimpleOutput extends TestCase {

    public void test() throws Exception {
        
        ResetBasicData.reset();
        
        SpiEbeanServer server = (SpiEbeanServer)Ebean.getServer(null);
                
        XomBuilder builder = new XomBuilder(server, null);
        
        XbNode orderRoot = builder.addRootElement("order", Order.class);
        
        orderRoot.addElement("id").addAttribute("status");
        orderRoot.addElement("cretime","created-ts");
        orderRoot.addElement("orderDate","ship-date");
        XbNode cust = orderRoot.addElement("customer","cust");
        cust.addAttribute("id");
        cust.addElement("name");
        
        XbNode orderDetails = orderRoot.addElement("details","order-details");
        XbNode line = orderDetails.addWrapperElement("line");
        line.addElement("id");
        line.addElement("orderQty","order-quantity");
        line.addElement("unitPrice","unit-price");
        
        XbNode product = line.addElement("product");
        product.addElement("name","prodname");
        product.addElement("sku","sku");

        
        XoiNode orderNode = orderRoot.createNode();
        
        
        List<Order> list = Ebean.find(Order.class)
            .join("details")
            .join("details.product","sku,name")
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
