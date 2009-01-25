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
 * Topic Post entity bean.
 */
@Entity
@Table(name="f_topic_post")
public class TopicPost {


    @Id
    Integer id;

    String title;

    @Lob
    String body;

    Timestamp cretime;

    @Version
    Timestamp updtime;

    @ManyToOne
    TopicPost parent;

    @ManyToOne
    Topic topic;

    @ManyToOne
    User user;

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
     * Return parent.
     */    
    public TopicPost getParent() {
  	    return parent;
    }

    /**
     * Set parent.
     */    
    public void setParent(TopicPost parent) {
  	    this.parent = parent;
    }

    /**
     * Return topic.
     */    
    public Topic getTopic() {
  	    return topic;
    }

    /**
     * Set topic.
     */    
    public void setTopic(Topic topic) {
  	    this.topic = topic;
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
