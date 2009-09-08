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
import com.avaje.ebean.bean.BeanCollection;
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
	
	public void test() throws Exception {

		loadData();

		bgFetchOne();
		pagingOne();
	}
	
	@SuppressWarnings("unchecked")
	private void bgFetchOne() {
		
		
		Query<TOne> query = Ebean.find(TOne.class)
			.setAutofetch(false)
			.select("id")
			.where().gt("name", "2")
			.setBackgroundFetchAfter(10)
			//.setMaxRows(20)
			.orderBy("id");

		//query.findList();
		//query.findIds();

//		long t1 = System.currentTimeMillis();
		
		List<TOne> ids = query.findList();
		//List<Object> ids = query.findIds();
		
//		long t0 = System.currentTimeMillis();
//		System.out.println("Got: "+ids.size());
		BeanCollection<TOne> bc = (BeanCollection<TOne>)ids; 
		bc.backgroundFetchWait();
		
//		long ex0 = System.currentTimeMillis() - t0;
//		long ex1 = System.currentTimeMillis() - t1;
//		System.out.println("Got: "+ids.size());
//		System.out.println("exetime t0:"+ex0+" t1:"+ex1);
		//System.out.println("done "+bc.size());
	}
	
	private void pagingOne() throws InterruptedException {
		
		loadData();
		

		int pageSize = 10;
		
		PagingList<TOne> pagingList = 
			Ebean.find(TOne.class)
				.where().gt("name", "2")
				.findPagingList(10);
		
				
		// get the row count in the background...
		// ... otherwise it is fetched on demand
		// ... when getRowCount() or getPageCount() 
		// ... is called
		pagingList.getFutureRowCount();
		
		// use fetch ahead... fetching the next page
		// in a background thread when the data in
		// the current page is touched
		pagingList.setFetchAhead(1);
		
		// get the first page
		Page<TOne> page = pagingList.getPage(0);
		
		// get the beans from the page as a list
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
				//System.out.println("here");
			}
			TOne tOne = asList.get(i);
			tOne.hashCode();
			//System.out.print(".");
		}
	}
	
}
