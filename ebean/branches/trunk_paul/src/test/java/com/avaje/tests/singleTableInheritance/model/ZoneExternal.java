package com.avaje.tests.singleTableInheritance.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EXT")
public class ZoneExternal extends Zone
{
	private String attribute;

	public String getAttribute()
	{
		return attribute;
	}

	public void setAttribute(String attribute)
	{
		this.attribute = attribute;
	}
}
