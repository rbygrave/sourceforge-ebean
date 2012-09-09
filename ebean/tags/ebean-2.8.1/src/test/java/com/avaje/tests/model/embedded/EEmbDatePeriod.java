package com.avaje.tests.model.embedded;

import java.util.Date;

import javax.persistence.Embeddable;

@Embeddable
public class EEmbDatePeriod {

	Date date1;
	Date date2;

	public Date getDate1() {
		return date1;
	}

	public void setDate1(Date date1) {
		this.date1 = date1;
	}

	public Date getDate2() {
		return date2;
	}

	public void setDate2(Date date2) {
		this.date2 = date2;
	}
}
