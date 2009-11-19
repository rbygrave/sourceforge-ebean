
package com.avaje.tests.model.basic;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public class TMappedSuper2 {

	String something;
	
	@Transient
	SomeObject someObject;

	@Transient
	Integer myint;

	public String getSomething() {
		return something;
	}

	public void setSomething(String something) {
		this.something = something;
	}

	public SomeObject getSomeObject() {
		return someObject;
	}

	public void setSomeObject(SomeObject someObject) {
		this.someObject = someObject;
	}

	public Integer getMyint() {
		return myint;
	}

	public void setMyint(Integer myint) {
		this.myint = myint;
	}

}

