package app.data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * User Log entity bean.
 */
@Entity
@Table(name="s_user_log")
public class UserLog {


    @Id
    Integer id;

    String email;

    String eventType;

    String eventDescription;

    String userAgent;

    String ipAddress;


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
     * Return event type.
     */    
    public String getEventType() {
  	    return eventType;
    }

    /**
     * Set event type.
     */    
    public void setEventType(String eventType) {
  	    this.eventType = eventType;
    }

    /**
     * Return event description.
     */    
    public String getEventDescription() {
  	    return eventDescription;
    }

    /**
     * Set event description.
     */    
    public void setEventDescription(String eventDescription) {
  	    this.eventDescription = eventDescription;
    }

    /**
     * Return user agent.
     */    
    public String getUserAgent() {
  	    return userAgent;
    }

    /**
     * Set user agent.
     */    
    public void setUserAgent(String userAgent) {
  	    this.userAgent = userAgent;
    }

    /**
     * Return ip address.
     */    
    public String getIpAddress() {
  	    return ipAddress;
    }

    /**
     * Set ip address.
     */    
    public void setIpAddress(String ipAddress) {
  	    this.ipAddress = ipAddress;
    }


}
