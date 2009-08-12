package com.avaje.tests.compositekeys.db;

import javax.persistence.Embeddable;

@Embeddable
public class RegionKey
{
    private int customer;

    private int type;

    public int getCustomer() {
        return customer;
    }

    public void setCustomer(int customer) {
        this.customer = customer;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}