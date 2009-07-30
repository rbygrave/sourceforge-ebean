package com.avaje.tests.idKeys.db;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
@SequenceGenerator(name = "AD_SEQ_NAME", sequenceName = "AD_SEQ")
public class AuditLog
{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AD_SEQ_NAME")
    private Long id;

    private String description;

    private String modifiedDescription;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getModifiedDescription()
    {
        return modifiedDescription;
    }

    public void setModifiedDescription(String modifiedDescription)
    {
        this.modifiedDescription = modifiedDescription==null?null:"_" + modifiedDescription + "_";
    }
}