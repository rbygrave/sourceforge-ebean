package app.data;

import java.sql.Timestamp;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Comment Link entity bean.
 */
@Entity
@Table(name="c_comment_link")
public class CommentLink {


    @Id
    Integer id;

    String linkUrl;

    Timestamp cretime;

    @Version
    Timestamp updtime;

    @OneToMany(cascade=CascadeType.ALL)
    Map<String,Comment> comments;


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
     * Return link url.
     */    
    public String getLinkUrl() {
  	    return linkUrl;
    }

    /**
     * Set link url.
     */    
    public void setLinkUrl(String linkUrl) {
  	    this.linkUrl = linkUrl;
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
     * Return comments.
     */    
    public Map<String,Comment> getComments() {
  	    return comments;
    }

    /**
     * Set comments.
     */    
    public void setComments(Map<String,Comment> comments) {
  	    this.comments = comments;
    }


}
