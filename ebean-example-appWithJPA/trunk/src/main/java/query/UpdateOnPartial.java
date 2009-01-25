package query;

import app.data.User;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;

public class UpdateOnPartial {


	public static void main(String[] args) {

		Query<User> query = Ebean.createQuery(User.class);
		
		// include the version column "updtime"
		query.select("name, updtime");
		
		// no version column
		//query.select("name");
		
		query.setId(1);
		
		User user = query.findUnique();
		String origName = user.getName();
		System.out.println("origName:"+origName);
		
		user.setName("oldGary");
		
		Ebean.save(user);
		
		//WITH select including version column updtime
		//update s_user set name=?, updtime=? where id=? and updtime=?
		
		//WITHOUT a version column...
		//update s_user set name=?, updtime=? where id=? and name=? 
	}

}
