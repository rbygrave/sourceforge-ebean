
package com.avaje.tests.model.ivo.converter;

import com.avaje.ebean.config.ScalarTypeConverter;
import com.avaje.tests.model.ivo.Oid;

public class OidTypeConverter {//implements ScalarTypeConverter<Oid<?>,Long> {

    public Oid<?> wrapValue(Long scalarType) {
        if (scalarType == null){
            return null;
        }
        return new Oid<Object>(scalarType);
    }

    public Long unwrapValue(Oid<?> beanType) {
        if (beanType == null){
            return null;
        }
        return beanType.getValue();
    }

}
