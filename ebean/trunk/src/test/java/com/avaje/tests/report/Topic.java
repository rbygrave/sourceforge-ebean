package com.avaje.tests.report;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Topic
{
	@Id
	Integer id;
	
	String title;

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}
}
