package com.avaje.tests.compositekeys.db;

import javax.persistence.Embeddable;
import javax.persistence.Column;

@Embeddable
public class ItemKey
{
    private int customer;

    @Column(name = "itemNumber")
    private String itemNumber;

    public int getCustomer() {
        return customer;
    }

    public void setCustomer(int customer) {
        this.customer = customer;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ItemKey))
        {
            return false;
        }

        ItemKey itemKey = (ItemKey) o;

        if (customer != itemKey.customer)
        {
            return false;
        }
        if (!itemNumber.equals(itemKey.itemNumber))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = customer;
        result = 31 * result + itemNumber.hashCode();
        return result;
    }
}
