package com.avaje.tests.noid;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.annotation.SqlSelect;

@Entity
@Table(name="No_Id_Entity_Rob")
@SqlSelect(name="noid", query="select value from No_Id_Entity_Rob")
public class NoIdEntity {

	 @Id
	private int id;
	private String value;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
