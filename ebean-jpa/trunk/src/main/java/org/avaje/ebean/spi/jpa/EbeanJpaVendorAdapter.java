package org.avaje.ebean.spi.jpa;

import javax.persistence.spi.PersistenceProvider;


public class EbeanJpaVendorAdapter extends org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter {

	public PersistenceProvider getPersistenceProvider() {
		return new EbeanPersistenceProvider();
	}

}
