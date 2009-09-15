package com.avaje.tests.basic;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.internal.SpiEbeanServer;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.type.ScalarType;
import com.avaje.tests.model.basic.TJodaEntity;

public class TestJodaType extends TestCase {

	public void test() {
		
		SpiEbeanServer server = (SpiEbeanServer)Ebean.getServer(null);
		BeanDescriptor<TJodaEntity> beanDescriptor = server.getBeanDescriptor(TJodaEntity.class);
		BeanProperty beanProperty = beanDescriptor.getBeanProperty("localTime");
		ScalarType scalarType = beanProperty.getScalarType();
		
		Assert.assertNotNull(scalarType);
	}
	
}
