package query;

import app.data.Topic;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;

public class TopicPosts {

	public static void main(String[] args) {
		

		Query<Topic> query = Ebean.createQuery(Topic.class).setId(1);
		query.join("posts", "*");
		query.join("posts.user", "*");
		query.join("forum", "*");
		query.join("user", "*");
		
		
		Topic topic = query.findUnique();
		System.out.println(topic);
		
	}

	/*
	 
<sql summary='[app.data.Topic, forum, user] +many[posts, posts.user]'>
SELECT t.id, t.body, t.cretime, t.editor_note, t.open, t.post_count, t.rating_count, t.rating_total, t.title, t.updtime
        , ff.id, ff.cretime, ff.sort_order, ff.title, ff.topic_count, ff.updtime, t.type_code
        , uu.id, uu.cookie_login, uu.cretime, uu.email, uu.error_count, uu.last_login, uu.name, uu.pwd, uu.reset_code, uu.reset_time, uu.salt, uu.status_code, uu.updtime
        , pp.id, pp.body, pp.cretime, pp.editor_note, pp.title, pp.updtime, pp.parent_id, pp.topic_id
        , puu.id, puu.cookie_login, puu.cretime, puu.email, puu.error_count, puu.last_login, puu.name, puu.pwd, puu.reset_code, puu.reset_time, puu.salt, puu.status_code, puu.updtime 
FROM f_topic t
JOIN f_forum ff ON t.forum_id = ff.id 
JOIN s_user uu ON t.user_id = uu.id 
LEFT OUTER JOIN f_topic_post pp ON t.id = pp.topic_id 
LEFT OUTER JOIN s_user puu ON pp.user_id = puu.id  
WHERE t.id = ? 
</sql>

*/
	 
}
