package app.data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;


/**
 * Product entity bean.
 */
@Entity
@Table(name="b_product")
public class BugProduct implements Serializable {

	private static final long serialVersionUID = 244977312462598492L;

	@Id
    Integer id;

    String name;

    Integer sortOrder;

    Timestamp cretime;

    @Version
    Timestamp updtime;

    @OneToMany
    List<Bug> bugs;


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
     * Return name.
     */    
    public String getName() {
  	    return name;
    }

    /**
     * Set name.
     */    
    public void setName(String name) {
  	    this.name = name;
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
     * Return bugs.
     */    
    public List<Bug> getBugs() {
  	    return bugs;
    }

    /**
     * Set bugs.
     */    
    public void setBugs(List<Bug> bugs) {
  	    this.bugs = bugs;
    }


}
