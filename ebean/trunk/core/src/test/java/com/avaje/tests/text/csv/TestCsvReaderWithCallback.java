package com.avaje.tests.text.csv;

import java.io.File;
import java.io.FileReader;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.text.csv.CsvCallback;
import com.avaje.ebean.text.csv.CsvReader;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestCsvReaderWithCallback extends TestCase {

	public void test() {

		ResetBasicData.reset();
		
		try {
			File f = new File("src/test/resources/test1.csv");

			FileReader reader = new FileReader(f);

			final EbeanServer server = Ebean.getServer(null);
			
			CsvReader<Customer> csvReader = server.createCsvReader(Customer.class);

			csvReader.setPersistBatchSize(2);
			csvReader.setLogInfoFrequency(3);
			
			csvReader.addIgnore();
			//csvReader.addProperty("id");
			csvReader.addProperty("status");
			csvReader.addProperty("name");
			csvReader.addDateTime("anniversary", "dd-MMM-yyyy");
			csvReader.addProperty("billingAddress.line1");
			csvReader.addProperty("billingAddress.city");
			//processor.addReference("billingAddress.country.code");
            csvReader.addProperty("billingAddress.country.code");

            
            // when using CsvCallback we have to manage the transaction
            // and must save the bean(s) explicitly 
			final Transaction transaction = Ebean.beginTransaction();
			
			// use JDBC statement batching
			transaction.setBatchMode(true);
			transaction.setBatchSize(5);
			
			// you can turn off persist cascade if that is desired
			//transaction.setPersistCascade(false);
			
			// add a comment to the transaction log
			transaction.log("CsvReader loading test1.csv");
			try {
				csvReader.process(reader, new CsvCallback<Customer>() {
	
					public void processBean(int row, Customer cust, String[] lineContent) {
						
						System.out.println(row + "> " + cust + " " + cust.getBillingAddress());
						
						server.save(cust.getBillingAddress(), transaction);
						server.save(cust, transaction);
					}
	
				});
				transaction.commit();
				
			} finally {
				transaction.end();
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
