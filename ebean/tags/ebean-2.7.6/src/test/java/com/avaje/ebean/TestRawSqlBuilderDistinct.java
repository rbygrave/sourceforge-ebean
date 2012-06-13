package com.avaje.ebean;

import org.junit.Assert;

import com.avaje.ebean.RawSql.Sql;

import junit.framework.TestCase;

public class TestRawSqlBuilderDistinct extends TestCase {

  public void testDistinct() {

    RawSqlBuilder r = RawSqlBuilder.parse("select distinct id, name from t_cust");
    Sql sql = r.getSql();
    Assert.assertEquals("id, name", sql.getPreFrom());
    Assert.assertEquals("from t_cust", sql.getPreWhere());
    Assert.assertEquals("", sql.getPreHaving());
    Assert.assertNull(sql.getOrderBy());

  }

}
