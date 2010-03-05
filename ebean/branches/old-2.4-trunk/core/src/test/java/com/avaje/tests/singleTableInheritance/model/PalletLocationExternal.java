package com.avaje.tests.singleTableInheritance.model;

import javax.persistence.Entity;
import javax.persistence.DiscriminatorValue;

@Entity
@DiscriminatorValue("EXT")
public class PalletLocationExternal extends PalletLocation
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
