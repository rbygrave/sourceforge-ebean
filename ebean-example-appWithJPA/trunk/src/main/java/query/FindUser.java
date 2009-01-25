package query;

import java.util.List;
import java.util.Set;

import app.data.User;

import com.avaje.ebean.Ebean;

public class FindUser {

	public static void main(String[] args) throws Exception {
		
		Set<User> users = Ebean.createQuery(User.class, "bugStatus")
			.setParameter("bugStatus", "NEW")
			.where().ilike("name", "rob%")
			.orderBy("id desc")
			.setMaxRows(20)
			.findSet();
		
		System.out.println(users);

		if (true){
			return;
		}

		
		User u = Ebean.createQuery(User.class, "bugsSummary")
			.setParameter("id", 1)
			.findUnique();
		
		System.out.println(u);
		
		if (true){
			return;
		}
		
		List<User> activeUsers = 
			Ebean.find(User.class)
				.setAutoFetch(false)
				.select("name, email, state")
				.join("loggedBugs","title, fixedVersion")
				.where().eq("state", User.State.ACTIVE)
				.orderBy("id desc")
				.findList();
		
		for (User user : activeUsers) {
			user.getName();
			user.getEmail();
			user.getState();
		}
		
//		Query<User> query = Ebean.createQuery(User.class);
//		query.setId(1);
//		
//		query.where().eq("state",User.State.ACTIVE);
//		//query.where().eq("loggedBugs.duplicateOf", "12");
//		
//		List<User> userList = query.findList();
//		
//		System.err.println("........");
//		
//		for (int i = 0; i < userList.size(); i++) {
//			User user = userList.get(i);
//			System.err.println("user: "+user);
//			List<Bug> loggedBugs = user.getLoggedBugs();
//			for (Bug bug : loggedBugs) {
//				System.out.println("bug: "+bug.getId()+" "+bug.getTitle()+" "+bug);
//			}
//	
//		}

	}

}
