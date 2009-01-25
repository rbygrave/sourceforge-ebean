package query;

import java.util.List;

import app.data.Topic;
import app.data.User;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.TxIsolation;
import com.avaje.ebean.annotation.Transactional;

public class FindInTransaction {

	@Transactional(isolation=TxIsolation.SERIALIZABLE)
	public static void main(String[] args) {

		List<User> users = 
			Ebean.find(User.class)
				.join("customer")
				.where()
					.eq("state", User.State.ACTIVE)
				.findList();
		
		System.out.println("users:"+users);
		
		
		List<Topic> topics = 
			Ebean.find(Topic.class)
				.join("posts")
				.where().eq("posts.user.state", User.State.ACTIVE)
				.orderBy("id desc")
				.findList();

		System.out.println("topics:"+topics);

	}
}
