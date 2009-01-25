package app.data;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Role entity bean.
 */
@Entity
@Table(name="s_role")
public class Role {


    @Id
    Integer id;

    String role;

    String description;

    @OneToOne
    RoleTest test;

    @OneToMany
    List<RoleModule> modules;

    @ManyToMany
    List<User> users;


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
     * Return role.
     */    
    public String getRole() {
  	    return role;
    }

    /**
     * Set role.
     */    
    public void setRole(String role) {
  	    this.role = role;
    }

    /**
     * Return description.
     */    
    public String getDescription() {
  	    return description;
    }

    /**
     * Set description.
     */    
    public void setDescription(String description) {
  	    this.description = description;
    }

    /**
     * Return test.
     */    
    public RoleTest getTest() {
  	    return test;
    }

    /**
     * Set test.
     */    
    public void setTest(RoleTest test) {
  	    this.test = test;
    }

    /**
     * Return modules.
     */    
    public List<RoleModule> getModules() {
  	    return modules;
    }

    /**
     * Set modules.
     */    
    public void setModules(List<RoleModule> modules) {
  	    this.modules = modules;
    }

    /**
     * Return users.
     */    
    public List<User> getUsers() {
  	    return users;
    }

    /**
     * Set users.
     */    
    public void setUsers(List<User> users) {
  	    this.users = users;
    }


}
