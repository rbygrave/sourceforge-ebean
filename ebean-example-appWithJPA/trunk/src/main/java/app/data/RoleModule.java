package app.data;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Role Module entity bean.
 */
@Entity
@Table(name="s_role_module")
public class RoleModule {


    @EmbeddedId
    RoleModuleId id;

    String c;

    String r;

    String u;

    String d;

    @ManyToOne
    Role role;

    @ManyToOne
    Module module;


    /**
     * Return id.
     */    
    public RoleModuleId getId() {
  	    return id;
    }

    /**
     * Set id.
     */    
    public void setId(RoleModuleId id) {
  	    this.id = id;
    }

    /**
     * Return c.
     */    
    public String getC() {
  	    return c;
    }

    /**
     * Set c.
     */    
    public void setC(String c) {
  	    this.c = c;
    }

    /**
     * Return r.
     */    
    public String getR() {
  	    return r;
    }

    /**
     * Set r.
     */    
    public void setR(String r) {
  	    this.r = r;
    }

    /**
     * Return u.
     */    
    public String getU() {
  	    return u;
    }

    /**
     * Set u.
     */    
    public void setU(String u) {
  	    this.u = u;
    }

    /**
     * Return d.
     */    
    public String getD() {
  	    return d;
    }

    /**
     * Set d.
     */    
    public void setD(String d) {
  	    this.d = d;
    }

    /**
     * Return role.
     */    
    public Role getRole() {
  	    return role;
    }

    /**
     * Set role.
     */    
    public void setRole(Role role) {
  	    this.role = role;
    }

    /**
     * Return module.
     */    
    public Module getModule() {
  	    return module;
    }

    /**
     * Set module.
     */    
    public void setModule(Module module) {
  	    this.module = module;
    }


}
