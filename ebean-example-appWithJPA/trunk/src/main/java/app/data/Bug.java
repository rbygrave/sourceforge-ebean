package app.data;

import java.sql.Timestamp;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import com.avaje.ebean.annotation.Sql;
import com.avaje.ebean.annotation.SqlSelect;

/**
 * Bug entity bean.
 */
@Sql(select={
		// a simple named query based on raw sql...
		@SqlSelect(name="simple", query="select id, title, status_code as status from b_bug")
})
@Entity
@Table(name="b_bug")
public class Bug {

    @Id
    Integer id;
    
    String title;
    
    @Lob
    String body;

    String fixedVersion;

    String duplicateOf;

    Timestamp cretime;

    @Version
    Timestamp updtime;

    @ManyToOne
    User userAssigned;

    @ManyToOne
    User userLogged;

    @ManyToOne
    @JoinColumn(name="priority_code")
    BugPriority priority;

    @ManyToOne
    BugProduct product;
    
    @ManyToOne
    BugType type;

    @ManyToOne
    BugStatus status;

    @OneToMany
    List<BugAttachment> attachments;

    @OneToMany
    List<BugDetail> details;


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
     * Return fixed version.
     */    
    public String getFixedVersion() {
  	    return fixedVersion;
    }

    /**
     * Set fixed version.
     */    
    public void setFixedVersion(String fixedVersion) {
  	    this.fixedVersion = fixedVersion;
    }

    /**
     * Return duplicate of.
     */    
    public String getDuplicateOf() {
  	    return duplicateOf;
    }

    /**
     * Set duplicate of.
     */    
    public void setDuplicateOf(String duplicateOf) {
  	    this.duplicateOf = duplicateOf;
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
     * Return user assigned.
     */    
    public User getUserAssigned() {
  	    return userAssigned;
    }

    /**
     * Set user assigned.
     */    
    public void setUserAssigned(User userAssigned) {
  	    this.userAssigned = userAssigned;
    }

    /**
     * Return user logged.
     */    
    public User getUserLogged() {
  	    return userLogged;
    }

    /**
     * Set user logged.
     */    
    public void setUserLogged(User userLogged) {
  	    this.userLogged = userLogged;
    }

    /**
     * Return priority.
     */    
    public BugPriority getPriority() {
  	    return priority;
    }

    /**
     * Set priority.
     */    
    public void setPriority(BugPriority priority) {
  	    this.priority = priority;
    }

    /**
     * Return type.
     */    
    public BugType getType() {
  	    return type;
    }

    /**
     * Set type.
     */    
    public void setType(BugType type) {
  	    this.type = type;
    }

    
    public BugProduct getProduct() {
		return product;
	}

	public void setProduct(BugProduct product) {
		this.product = product;
	}

	/**
     * Return status.
     */    
    public BugStatus getStatus() {
  	    return status;
    }

    /**
     * Set status.
     */    
    public void setStatus(BugStatus status) {
  	    this.status = status;
    }

    /**
     * Return attachments.
     */    
    public List<BugAttachment> getAttachments() {
  	    return attachments;
    }

    /**
     * Set attachments.
     */    
    public void setAttachments(List<BugAttachment> attachments) {
  	    this.attachments = attachments;
    }

    /**
     * Return details.
     */    
    public List<BugDetail> getDetails() {
  	    return details;
    }

    /**
     * Set details.
     */    
    public void setDetails(List<BugDetail> details) {
  	    this.details = details;
    }


}
