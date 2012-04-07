package com.avaje.tests.model.basic;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Entity
public class MRole {

	@Id
	Integer roleid;
	
	String roleName;

	@ManyToMany(cascade=CascadeType.ALL
//	@JoinTable(name="myint_table"//, 
//		joinColumns={
//			@JoinColumn(name="mroleid", referencedColumnName="roleid")
//		}//,
//		inverseJoinColumns={
//			@JoinColumn(name = "muserid", referencedColumnName="userid")
//		}
	
	)
	List<MUser> users;
	
    public MRole() {
        
    }
    
    public MRole(String roleName) {
        this.roleName = roleName;
    }
    
	public Integer getRoleid() {
		return roleid;
	}

	public void setRoleid(Integer roleid) {
		this.roleid = roleid;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public List<MUser> getUsers() {
		return users;
	}

	public void setUsers(List<MUser> users) {
		this.users = users;
	}

	@Override
	public String toString() {
		return "MRole [roleName=" + roleName + "]";
	}
}
