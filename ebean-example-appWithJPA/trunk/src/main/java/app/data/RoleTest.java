package app.data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Role Test entity bean.
 */
@Entity
@Table(name="s_role_test")
public class RoleTest {


    @Id
    Integer id;

    String acol;

    @OneToOne
    Role role;


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
     * Return acol.
     */    
    public String getAcol() {
  	    return acol;
    }

    /**
     * Set acol.
     */    
    public void setAcol(String acol) {
  	    this.acol = acol;
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


}
