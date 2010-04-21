package com.avaje.tests.model.basic;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;

@Entity
@DiscriminatorValue("1")
public class ListAttribute extends Attribute {
	
	private static final long serialVersionUID = 1L;
	
	@ManyToMany(mappedBy="listAttributes", cascade={CascadeType.PERSIST})
	private Set<ListAttributeValue> values = new HashSet<ListAttributeValue>();

	public Set<ListAttributeValue> getValues() {
		return values;
	}

	public void setValues(Set<ListAttributeValue> values) {
		this.values = values;
	}
	
	public void add(ListAttributeValue value){
		getValues().add(value);
		value.getListAttributes().add(this);
	}
}
