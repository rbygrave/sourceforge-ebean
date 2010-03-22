package com.avaje.ebean.springsupport.jpa;

import java.util.logging.Logger;

import javax.persistence.spi.PersistenceProvider;

import com.avaje.ebean.spi.jpa.EbeanPersistenceProvider;


public class EbeanJpaVendorAdapter extends org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter {
    private static Logger log = Logger.getLogger("jpa.spi");
   public EbeanJpaVendorAdapter() {log.info("creating EbeanJpaVendorAdapter...");}

	public PersistenceProvider getPersistenceProvider() {
		return new EbeanPersistenceProvider();
	}

}
