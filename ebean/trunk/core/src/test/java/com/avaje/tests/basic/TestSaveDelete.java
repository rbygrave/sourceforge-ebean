package com.avaje.tests.basic;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.PersistentFile;
import com.avaje.tests.model.basic.PersistentFileContent;

import junit.framework.TestCase;

public class TestSaveDelete extends TestCase {

	// This fails
	public void testCreateDeletePersistentFile() {
		PersistentFile persistentFile = new PersistentFile("test.txt",
				new PersistentFileContent("test".getBytes()));

		Ebean.save(persistentFile);
		Ebean.delete(persistentFile);
	}

	// This passes
	public void testCreateLoadDeletePersistentFile() {
		PersistentFile persistentFile = new PersistentFile("test.txt",
				new PersistentFileContent("test".getBytes()));

		Ebean.save(persistentFile);

		persistentFile = Ebean.find(PersistentFile.class, persistentFile.getId());
		Ebean.delete(persistentFile);
	}
}
