package app.data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import com.avaje.ebean.annotation.EnumMapping;

/**
 * User entity bean.
 */
@Entity
@Table(name="s_user_vw")
public class UserView implements Serializable {

	private static final long serialVersionUID = -8686268470992580463L;

	@EnumMapping(nameValuePairs="NEW=N, ACTIVE=A, INACTIVE=I")
	public enum State {
		NEW,
		ACTIVE,
		INACTIVE;
	}
	
	@Id
    Integer id;
	
    String email;

    String name;

    String pwd;

    String salt;

    Timestamp lastLogin;

    String cookieLogin;

    Integer errorCount;

    String resetCode;

    Timestamp resetTime;

    Timestamp cretime;

    @Version
    Timestamp updtime;

    @Column(name="status_code")
    State state;


    @OneToMany(mappedBy="userAssigned")
    @JoinColumn(referencedColumnName="user_assigned_id",name="id")
    List<Bug> assignedBugs;

//    @OneToMany
//    @Where(clause="${ta}.title='y'")
//    List<Topic> topics;
//
//    @OneToMany
//    List<BugDetail> details;
//
//    @OneToMany(mappedBy="userLogged")
//    @JoinColumn(name="user_logged_id")
//    List<Bug> loggedBugs;
//
//    @ManyToMany
//    List<Role> roles;
//
//    @OneToMany
//    List<Comment> comments;
//
//    @OneToMany
//    List<TopicPost> posts;


    public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

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
     * Return email.
     */    
    public String getEmail() {
  	    return email;
    }

    /**
     * Set email.
     */    
    public void setEmail(String email) {
  	    this.email = email;
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
     * Return pwd.
     */    
    public String getPwd() {
  	    return pwd;
    }

    /**
     * Set pwd.
     */    
    public void setPwd(String pwd) {
  	    this.pwd = pwd;
    }

    /**
     * Return salt.
     */    
    public String getSalt() {
  	    return salt;
    }

    /**
     * Set salt.
     */    
    public void setSalt(String salt) {
  	    this.salt = salt;
    }

    /**
     * Return last login.
     */    
    public Timestamp getLastLogin() {
  	    return lastLogin;
    }

    /**
     * Set last login.
     */    
    public void setLastLogin(Timestamp lastLogin) {
  	    this.lastLogin = lastLogin;
    }

    /**
     * Return cookie login.
     */    
    public String getCookieLogin() {
  	    return cookieLogin;
    }

    /**
     * Set cookie login.
     */    
    public void setCookieLogin(String cookieLogin) {
  	    this.cookieLogin = cookieLogin;
    }

    /**
     * Return error count.
     */    
    public Integer getErrorCount() {
  	    return errorCount;
    }

    /**
     * Set error count.
     */    
    public void setErrorCount(Integer errorCount) {
  	    this.errorCount = errorCount;
    }

    /**
     * Return reset code.
     */    
    public String getResetCode() {
  	    return resetCode;
    }

    /**
     * Set reset code.
     */    
    public void setResetCode(String resetCode) {
  	    this.resetCode = resetCode;
    }

    /**
     * Return reset time.
     */    
    public Timestamp getResetTime() {
  	    return resetTime;
    }

    /**
     * Set reset time.
     */    
    public void setResetTime(Timestamp resetTime) {
  	    this.resetTime = resetTime;
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

//    /**
//     * Return status.
//     */    
//    public UserStatus getStatus() {
//  	    return status;
//    }
//
//    /**
//     * Set status.
//     */    
//    public void setStatus(UserStatus status) {
//  	    this.status = status;
//    }

    /**
     * Return assigned bugs.
     */    
    public List<Bug> getAssignedBugs() {
  	    return assignedBugs;
    }

    /**
     * Set assigned bugs.
     */    
    public void setAssignedBugs(List<Bug> assignedBugs) {
  	    this.assignedBugs = assignedBugs;
    }

//    /**
//     * Return topics.
//     */    
//    public List<Topic> getTopics() {
//  	    return topics;
//    }
//
//    /**
//     * Set topics.
//     */    
//    public void setTopics(List<Topic> topics) {
//  	    this.topics = topics;
//    }
//
//    /**
//     * Return details.
//     */    
//    public List<BugDetail> getDetails() {
//  	    return details;
//    }
//
//    /**
//     * Set details.
//     */    
//    public void setDetails(List<BugDetail> details) {
//  	    this.details = details;
//    }
//
//    /**
//     * Return logged bugs.
//     */    
//    public List<Bug> getLoggedBugs() {
//  	    return loggedBugs;
//    }
//
//    /**
//     * Set logged bugs.
//     */    
//    public void setLoggedBugs(List<Bug> loggedBugs) {
//  	    this.loggedBugs = loggedBugs;
//    }
//
//    /**
//     * Return roles.
//     */    
//    public List<Role> getRoles() {
//  	    return roles;
//    }
//
//    /**
//     * Set roles.
//     */    
//    public void setRoles(List<Role> roles) {
//  	    this.roles = roles;
//    }
//
//    /**
//     * Return comments.
//     */    
//    public List<Comment> getComments() {
//  	    return comments;
//    }
//
//    /**
//     * Set comments.
//     */    
//    public void setComments(List<Comment> comments) {
//  	    this.comments = comments;
//    }
//
//    /**
//     * Return posts.
//     */    
//    public List<TopicPost> getPosts() {
//  	    return posts;
//    }
//
//    /**
//     * Set posts.
//     */    
//    public void setPosts(List<TopicPost> posts) {
//  	    this.posts = posts;
//    }


}
