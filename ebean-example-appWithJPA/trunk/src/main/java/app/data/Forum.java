package app.data;

import java.sql.Timestamp;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Forum entity bean.
 */
@Entity
@Table(name="f_forum")
public class Forum {


    @Id
    Integer id;

    String title;

    Integer sortOrder;

    Timestamp cretime;

    @Version
    Timestamp updtime;

    @OneToMany
    List<Topic> topics;


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
     * Return sort order.
     */    
    public Integer getSortOrder() {
  	    return sortOrder;
    }

    /**
     * Set sort order.
     */    
    public void setSortOrder(Integer sortOrder) {
  	    this.sortOrder = sortOrder;
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
     * Return topics.
     */    
    public List<Topic> getTopics() {
  	    return topics;
    }

    /**
     * Set topics.
     */    
    public void setTopics(List<Topic> topics) {
  	    this.topics = topics;
    }


}
