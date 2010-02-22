package com.avaje.tests.model.basic;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Entity
public class MUser {

	@Id
	Integer userid;
	
	String userName;

	// Cascade remove will delete from intersection table
	// but will not delete the actual Roles
	@ManyToMany(mappedBy="users",cascade=CascadeType.ALL)
	List<MRole> roles;
	
    public MUser() {
        
    }
    
    public MUser(String userName) {
        this.userName = userName;
    }
    
	public Integer getUserid() {
		return userid;
	}

	public void setUserid(Integer userid) {
		this.userid = userid;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public List<MRole> getRoles() {
		return roles;
	}

	public void setRoles(List<MRole> roles) {
		this.roles = roles;
	}
	
	public void addRole(MRole role){
	    if (roles == null){
	        roles = new ArrayList<MRole>();
	    }
	    roles.add(role);
	}
	
}
