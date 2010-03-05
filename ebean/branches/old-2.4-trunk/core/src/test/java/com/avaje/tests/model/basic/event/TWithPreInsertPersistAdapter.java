package com.avaje.tests.model.basic.event;

import com.avaje.ebean.event.BeanPersistAdapter;
import com.avaje.ebean.event.BeanPersistRequest;
import com.avaje.tests.model.basic.TWithPreInsert;

public class TWithPreInsertPersistAdapter extends BeanPersistAdapter {

	@Override
	public boolean isRegisterFor(Class<?> cls) {
		return TWithPreInsert.class.equals(cls);
	}

	@Override
	public boolean preInsert(BeanPersistRequest<?> request) {
		
		TWithPreInsert e = (TWithPreInsert)request.getBean();
		
		e.setName("aname");
		return true;
	}

	
	
}
