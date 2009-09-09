package com.avaje.tests.report;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.List;

public class TestSqlBasedEntity extends TestCase
{
	public void test()
	{
		Topic topic = new Topic();
		topic.setTitle("ABC");
		topic.setTitle("123");

		Ebean.save(topic);

		Query<ReportTopic> query = Ebean.createQuery(ReportTopic.class);
		List<ReportTopic> topics = query.findList();

		Assert.assertNotNull(topics);
		Assert.assertEquals(2, topics.size());
	}
}
