package app.trans;

import java.io.IOException;

import app.data.User;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.TxIsolation;
import com.avaje.ebean.TxRunnable;
import com.avaje.ebean.TxScope;
import com.avaje.ebean.TxType;
import com.avaje.ebean.annotation.Transactional;

public class MyModelService {

	public void runInTransExecute() {

		TxScope scope = TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE);

		Ebean.execute(scope, new TxRunnable() {
			public void run() {
				User u1 = Ebean.find(User.class, 1);
				User u2 = Ebean.find(User.class, 2);

				u1.setName("u1 mod");
				u2.setName("u2 mod");

				Ebean.save(u1);
				Ebean.save(u2);

			}
		});

	}

	@Transactional
	public void runFirst() throws IOException {

		System.out.println("runFirst");
		User u1 = Ebean.find(User.class, 1);

		runInTrans();
	}

	@Transactional(type = TxType.REQUIRES_NEW)
	public void runInTrans() throws IOException {

		System.out.println("runInTrans ...");
		User u1 = Ebean.find(User.class, 1);
		// if (u1 == null){
		// //throw new NullPointerException("asd");
		// return;
		// }

		runInExecute();

		if (true) {
			throw new IOException("Hello");
		}
		System.out.println("hello");
	}

	public void runInExecute() { 

		TxScope txScope = TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE).setNoRollbackFor(
			IOException.class);

		Ebean.execute(txScope, new TxRunnable() {
			public void run() {

				// running in REQUIRED transactional scope
				System.out.println(Ebean.currentTransaction());

				User u1 = Ebean.find(User.class, 1);

				Ebean.save(u1);

			}
		});
	}
}
