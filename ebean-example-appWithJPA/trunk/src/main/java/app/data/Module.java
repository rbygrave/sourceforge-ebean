package app.data;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Module entity bean.
 */
@Entity
@Table(name="s_module")
public class Module {


    @Id
    Integer id;

    String module;

    @OneToMany
    List<RoleModule> modules;


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
     * Return module.
     */    
    public String getModule() {
  	    return module;
    }

    /**
     * Set module.
     */    
    public void setModule(String module) {
  	    this.module = module;
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


}
