package app.data;

import java.sql.Timestamp;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Bug Detail entity bean.
 */
@Entity
@Table(name="b_bug_detail")
public class BugDetail {


    @Id
    Integer id;

    String title;

    Timestamp postDate;

    @Lob
    String body;

    Timestamp cretime;

    @Version
    Timestamp updtime;

    @ManyToOne
    User user;

    @ManyToOne
    Bug bug;


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
     * Return post date.
     */    
    public Timestamp getPostDate() {
  	    return postDate;
    }

    /**
     * Set post date.
     */    
    public void setPostDate(Timestamp postDate) {
  	    this.postDate = postDate;
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
     * Return bug.
     */    
    public Bug getBug() {
  	    return bug;
    }

    /**
     * Set bug.
     */    
    public void setBug(Bug bug) {
  	    this.bug = bug;
    }


}
