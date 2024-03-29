package com.avaje.tests.model.basic;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Version;

@Entity
public class OGearBox {

	private static final long serialVersionUID = 1L;

	@Id
	private UUID id;

	private String boxDesc;

	private Integer size;
	
	@Version
	private Integer version;
	
	@OneToOne
	private OCar car;
	
	public OGearBox() {
	}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBoxDesc() {
        return boxDesc;
    }

    public void setBoxDesc(String boxDesc) {
        this.boxDesc = boxDesc;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public OCar getCar() {
        return car;
    }

    public void setCar(OCar car) {
        this.car = car;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

}
