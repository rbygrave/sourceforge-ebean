package app.trans;

import java.io.IOException;
import java.util.List;

import app.data.User;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.annotation.Transactional;

public class MySimpleUserService implements ServiceInterface {

	// @Transactional(type=TxType.REQUIRES_NEW,
	// noRollbackFor={RuntimeException.class,IOException.class})
	
	@Transactional(noRollbackFor={NullPointerException.class})
	public void runInTrans() throws IOException {

		User u1 = Ebean.find(User.class, 1);
		
		
		List<User> users = 
			Ebean.find(User.class)
				.where()
				.like("name", "T%")
				.findList();

		System.out.println("..... name " + u1.getName());
		u1.setName(u1.getName() + "_md");
		//Ebean.save(u1);

		Object o = null;
		o.toString();
//
//		if (true) {
//			throw new RuntimeException("Rob was here.");
//		}
		System.out.println("hello 1:" + u1 + " size:" + users.size());
	}

	public void other() {
		System.out.println("other...");
	}
}
