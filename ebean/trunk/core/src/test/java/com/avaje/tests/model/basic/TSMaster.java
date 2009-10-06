package com.avaje.tests.model.basic;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * A basic entity to test simple things.
 */
@Entity
@Table(name = "t_atable_thatisrelatively")
public class TSMaster {

	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "t_atable_master_seq")
	@Id
	Integer id;

	String name;

	String description;

	boolean active;
	
	@OneToMany
	List<TSDetail> details;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public List<TSDetail> getDetails() {
		return details;
	}

	public void setDetails(List<TSDetail> details) {
		this.details = details;
	}
}
