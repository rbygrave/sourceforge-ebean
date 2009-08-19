package com.avaje.tests.basic;

import java.util.List;
import java.util.Random;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Page;
import com.avaje.ebean.PagingList;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.TOne;

public class TestPaging extends TestCase {

	
	private void loadData() {
		
		int rowCount = Ebean.find(TOne.class).findRowCount();
		if (rowCount > 500){
			return;
		}
		
		Random r = new Random();
		Ebean.beginTransaction();
		try {
			for (int i = 0; i < 1000; i++) {
				TOne o = new TOne();
				
				int rvalue = r.nextInt(100000);
				o.setName(rvalue+"name");
				o.setDescription(rvalue+"");
				Ebean.save(o);
			}
			Ebean.commitTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
	}
	
	public void testInitial() throws InterruptedException {
		
		loadData();
		
		EbeanServer server = Ebean.getServer(null);
		
		Query<TOne> query = server.find(TOne.class)
			.where().gt("name", "2")
			.query();
		
		int pageSize = 10;
		
		PagingList<TOne> pagingList = query.findPagingList(pageSize);
		
		// get the row count in the background...
		pagingList.getFutureRowCount();
		
		Page<TOne> page = pagingList.getPage(0);
		List<TOne> list = page.getList();
		
		Assert.assertTrue("page size ",list.size() == pageSize);
		
		int totalRows = pagingList.getRowCount();
		Assert.assertTrue("page size ",totalRows >= list.size());
		
		Thread.sleep(300);
		
		Page<TOne> next = page.next();
		List<TOne> list2 = next.getList();
		
		Assert.assertTrue("page size ",list2.size() == pageSize);
		
		checkForLoop();
	}
	
	private void checkForLoop() {
		
		EbeanServer server = Ebean.getServer(null);
		
		Query<TOne> query = server.find(TOne.class)
			.where().gt("name", "2")
			.query();
	
		int pageSize = 10;
		
		PagingList<TOne> pagingList = server.findPagingList(query, null, pageSize);

		List<TOne> asList = pagingList.getAsList();
		for (int i = 0; i < asList.size(); i++) {
			if (i % 10 == 0){
				System.out.println("here");
			}
			TOne tOne = asList.get(i);
			tOne.hashCode();
			System.out.print(".");
		}
	}
}
