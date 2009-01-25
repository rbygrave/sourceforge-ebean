package app.data;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Topic entity bean.
 */
//@NamedUpdates(value = {
//	@NamedUpdate(
//		name = "setTitle", 
//		isSql = false,
//		notifyCache = false, 
//		update = "update topic set title = :title, postCount = :postCount where id = :id"),
//	@NamedUpdate(
//		name = "setPostCount", 
//		notifyCache = false, 
//		update = "update f_topic set post_count = :postCount where id = :id"),
//	@NamedUpdate(
//		name = "incrementPostCount", 
//		notifyCache = false, 
//		isSql = false,
//		update = "update Topic set postCount = postCount + 1 where id = :id") 
//		//update = "update f_topic set post_count = post_count + 1 where id = :id") 
//})
@Entity
@Table(name = "f_topic")
public class Topic {

	@Id
	Integer id;

	String title;

	@Lob
	String body;

	Integer postCount;

	Timestamp cretime;

	@Version
	Timestamp updtime;

	@ManyToOne
	User user;

	@ManyToOne
	Forum forum;

	@OneToMany
	List<TopicPost> posts;

	/**
	 * Return id.
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set id.
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Return title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set title.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Return body.
	 */
	public String getBody() {
		return body;
	}

	/**
	 * Set body.
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * Return post count.
	 */
	public Integer getPostCount() {
		return postCount;
	}

	/**
	 * Set post count.
	 */
	public void setPostCount(Integer postCount) {
		this.postCount = postCount;
	}

	/**
	 * Return cretime.
	 */
	public Timestamp getCretime() {
		return cretime;
	}

	/**
	 * Set cretime.
	 */
	public void setCretime(Timestamp cretime) {
		this.cretime = cretime;
	}

	/**
	 * Return updtime.
	 */
	public Timestamp getUpdtime() {
		return updtime;
	}

	/**
	 * Set updtime.
	 */
	public void setUpdtime(Timestamp updtime) {
		this.updtime = updtime;
	}

	/**
	 * Return user.
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Set user.
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * Return forum.
	 */
	public Forum getForum() {
		return forum;
	}

	/**
	 * Set forum.
	 */
	public void setForum(Forum forum) {
		this.forum = forum;
	}

	/**
	 * Return posts.
	 */
	public List<TopicPost> getPosts() {
		return posts;
	}

	/**
	 * Set posts.
	 */
	public void setPosts(List<TopicPost> posts) {
		this.posts = posts;
	}

}
