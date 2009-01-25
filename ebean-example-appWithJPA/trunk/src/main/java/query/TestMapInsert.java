package query;

import java.util.LinkedHashMap;
import java.util.Map;

import app.data.Comment;
import app.data.CommentLink;
import app.data.User;

import com.avaje.ebean.Ebean;

public class TestMapInsert {

	public static void main(String[] args) {

		User u = (User)Ebean.getReference(User.class, 1);
		
		CommentLink cl = new CommentLink();
		cl.setLinkUrl("http://none");
		
		Comment cm = new Comment();
		cm.setTitle("t2");
		cm.setBody("body2");
		cm.setUser(u);
		

		Map<String,Comment> comments = new LinkedHashMap<String, Comment>();
		comments.put("kumera", cm);

		cl.setComments(comments);

		Ebean.save(cl);
	}

}
