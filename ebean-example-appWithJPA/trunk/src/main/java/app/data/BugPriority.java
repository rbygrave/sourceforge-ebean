package app.data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Bug Priority entity bean.
 */
@Entity
@Table(name="b_bug_priority")
public class BugPriority {


    @Id
    String code;

    String title;

    Integer sortOrder;


    /**
     * Return code.
     */    
    public String getCode() {
  	    return code;
    }

    /**
     * Set code.
     */    
    public void setCode(String code) {
  	    this.code = code;
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


}
