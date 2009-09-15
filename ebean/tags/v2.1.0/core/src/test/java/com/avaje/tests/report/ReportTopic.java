package com.avaje.tests.report;

import com.avaje.ebean.annotation.Sql;
import com.avaje.ebean.annotation.SqlSelect;

import javax.persistence.Entity;

@Entity
@Sql(select = {
	@SqlSelect(
		query =
			"select t.id, t.title, count(t.id) as score " +
				"from topic t " +
				"group by t.id, t.title"),
	@SqlSelect(
		name = "as.max",
		query = "select t.id, t.title, max(t.*) as score " +
			"from topic t")
	})
public class ReportTopic
{
	Integer id;
	String title;
	Double score;

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

	public Double getScore()
	{
		return score;
	}

	public void setScore(Double score)
	{
		this.score = score;
	}
}
