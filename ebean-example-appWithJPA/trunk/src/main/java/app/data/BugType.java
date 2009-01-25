package app.data;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Bug Type entity bean.
 */
@Entity
@Table(name="b_bug_type")
public class BugType {


    @Id
    String code;

    String title;

    @OneToMany
    List<Bug> bugs;


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
