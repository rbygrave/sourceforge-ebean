package com.avaje.ebean;

import java.util.Map;

import com.avaje.ebean.RawSql.ColumnMapping;
import com.avaje.ebean.RawSql.ColumnMapping.Column;

import junit.framework.TestCase;

public class TestRawSqlColumnParsing extends TestCase {

    public void test_simple() {
        
        ColumnMapping columnMapping = DRawSqlColumnsParser.parse("a,b,c");
        Map<String, Column> mapping = columnMapping.mapping();
        Column c = mapping.get("a");
        
        assertEquals("a",c.getDbColumn());
        assertEquals(0, c.getIndexPos());
        assertNull(c.getPropertyName());

        c = mapping.get("b");
        assertEquals("b",c.getDbColumn());
        assertEquals(1, c.getIndexPos());
        assertNull(c.getPropertyName());

        c = mapping.get("c");
        assertEquals("c",c.getDbColumn());
        assertEquals(2, c.getIndexPos());
        assertNull(c.getPropertyName());

    }

   public void test_simpleWithSpacing() {
        
        ColumnMapping columnMapping = DRawSqlColumnsParser.parse(" a  ,  b    ,  c   ");
        Map<String, Column> mapping = columnMapping.mapping();
        Column c = mapping.get("a");
        
        assertEquals("a",c.getDbColumn());
        assertEquals(0, c.getIndexPos());
        assertNull(c.getPropertyName());

        c = mapping.get("b");
        assertEquals("b",c.getDbColumn());
        assertEquals(1, c.getIndexPos());
        assertNull(c.getPropertyName());

        c = mapping.get("c");
        assertEquals("c",c.getDbColumn());
        assertEquals(2, c.getIndexPos());
        assertNull(c.getPropertyName());

    }
   
    public void test_withAlias() {
        
        ColumnMapping columnMapping = DRawSqlColumnsParser.parse("a a0,b    b1, c  c2 ,   d    d3  , e  e4 ");
        Map<String, Column> mapping = columnMapping.mapping();
        
        assertEquals(5, mapping.size());
        
        Column c = mapping.get("a");
        
        assertEquals("a",c.getDbColumn());
        assertEquals(0, c.getIndexPos());
        assertNull(c.getPropertyName());

        c = mapping.get("b");
        assertEquals("b",c.getDbColumn());
        assertEquals(1, c.getIndexPos());
        assertNull(c.getPropertyName());

        c = mapping.get("c");
        assertEquals("c",c.getDbColumn());
        assertEquals(2, c.getIndexPos());
        assertNull(c.getPropertyName());

        c = mapping.get("d");
        assertEquals("d",c.getDbColumn());
        assertEquals(3, c.getIndexPos());
        assertNull(c.getPropertyName());

        
        c = mapping.get("e");
        assertEquals("e",c.getDbColumn());
        assertEquals(4, c.getIndexPos());
        assertNull(c.getPropertyName());

    }

    
    public void test_withAsAlias() {
        
        ColumnMapping columnMapping = DRawSqlColumnsParser.parse("a as a0,'b'    b1, \"c(blah)\" as c2 ,   d as   d3  , e     as e4 ");
        Map<String, Column> mapping = columnMapping.mapping();
        
        assertEquals(5, mapping.size());
        
        Column c = mapping.get("a");
        
        assertEquals("a",c.getDbColumn());
        assertEquals(0, c.getIndexPos());
        assertNull(c.getPropertyName());

        c = mapping.get("'b'");
        assertEquals("'b'",c.getDbColumn());
        assertEquals(1, c.getIndexPos());
        assertNull(c.getPropertyName());

        c = mapping.get("\"c(blah)\"");
        assertEquals("\"c(blah)\"",c.getDbColumn());
        assertEquals(2, c.getIndexPos());
        assertNull(c.getPropertyName());

        c = mapping.get("d");
        assertEquals("d",c.getDbColumn());
        assertEquals(3, c.getIndexPos());
        assertNull(c.getPropertyName());

        
        c = mapping.get("e");
        assertEquals("e",c.getDbColumn());
        assertEquals(4, c.getIndexPos());
        assertNull(c.getPropertyName());

    }

}
