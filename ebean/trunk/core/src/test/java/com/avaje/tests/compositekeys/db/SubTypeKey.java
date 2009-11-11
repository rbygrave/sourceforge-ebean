package com.avaje.tests.compositekeys.db;

import javax.persistence.Embeddable;

@Embeddable
public class SubTypeKey
{
    private int subTypeId;

	public int getSubTypeId()
	{
		return subTypeId;
	}

	public void setSubTypeId(int subTypeId)
	{
		this.subTypeId = subTypeId;
	}
}