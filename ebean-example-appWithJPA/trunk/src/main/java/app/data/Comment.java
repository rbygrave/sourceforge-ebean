package app.data;

import java.sql.Timestamp;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Comment entity bean.
 */
@Entity
@Table(name="c_comment")
public class Comment {


    @Id
    Integer id;

    String title;

    @Lob
    String body;

    Timestamp cretime;

    @Version
    Timestamp updtime;

    @ManyToOne
    CommentLink link;

    @ManyToOne
    User user;


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
     * Return link.
     */    
    public CommentLink getLink() {
  	    return link;
    }

    /**
     * Set link.
     */    
    public void setLink(CommentLink link) {
  	    this.link = link;
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


}
