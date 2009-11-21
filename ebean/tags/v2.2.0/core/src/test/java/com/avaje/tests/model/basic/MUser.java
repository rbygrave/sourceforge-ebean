package com.avaje.tests.model.basic;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Entity
public class MUser {

	@Id
	Integer userid;
	
	String userName;

	@ManyToMany(mappedBy="users")//,cascade=CascadeType.ALL)
	List<MRole> roles;
	
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
	
	
	
}
