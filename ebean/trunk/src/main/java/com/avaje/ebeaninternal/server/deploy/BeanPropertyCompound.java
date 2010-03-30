/**
 * Copyright (C) 2009  Robin Bygrave
 *
 * This file is part of Ebean.
 *
 * Ebean is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Ebean is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.avaje.ebeaninternal.server.deploy;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;

import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanPropertyCompound;
import com.avaje.ebeaninternal.server.el.ElPropertyChainBuilder;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.query.SqlBeanLoad;
import com.avaje.ebeaninternal.server.text.json.WriteJsonContext;
import com.avaje.ebeaninternal.server.type.CtCompoundProperty;
import com.avaje.ebeaninternal.server.type.CtCompoundPropertyElAdapter;
import com.avaje.ebeaninternal.server.type.CtCompoundType;

/**
 * Property mapped to an Immutable Compound Value Object.
 * <p>
 * An Immutable Compound Value Object is similar to an Embedded bean but it
 * doesn't require enhancement and MUST be treated as an Immutable type.
 * </p>
 */
public class BeanPropertyCompound extends BeanProperty {

    private final CtCompoundType<?> compoundType;

    private final BeanProperty[] scalarProperties;

    private final LinkedHashMap<String, BeanProperty> propertyMap = new LinkedHashMap<String, BeanProperty>();

    private final LinkedHashMap<String, CtCompoundPropertyElAdapter> nonScalarMap = new LinkedHashMap<String, CtCompoundPropertyElAdapter>();

    /**
     * Create the property.
     */
    public BeanPropertyCompound(BeanDescriptorMap owner, BeanDescriptor<?> descriptor, DeployBeanPropertyCompound deploy) {

        super(owner, descriptor, deploy);

        this.compoundType = deploy.getCompoundType();

        BeanPropertyCompoundRoot root = deploy.getFlatProperties(owner, descriptor);

        this.scalarProperties = root.getScalarProperties();

        for (int i = 0; i < scalarProperties.length; i++) {
            propertyMap.put(scalarProperties[i].getName(), scalarProperties[i]);
        }

        List<CtCompoundProperty> nonScalarPropsList = root.getNonScalarProperties();

        for (int i = 0; i < nonScalarPropsList.size(); i++) {
            CtCompoundProperty ctProp = nonScalarPropsList.get(i);
            CtCompoundPropertyElAdapter adapter = new CtCompoundPropertyElAdapter(ctProp);
            nonScalarMap.put(ctProp.getRelativeName(), adapter);
        }

    }

    @Override
    public void initialise() {
        // do nothing for normal BeanProperty
        if (!isTransient && compoundType == null) {
            String msg = "No cvoInternalType assigned to " + descriptor.getFullName() + "." + getName();
            throw new RuntimeException(msg);
        }
    }

    public ElPropertyValue buildElPropertyValue(String propName, String remainder, ElPropertyChainBuilder chain, boolean propertyDeploy) {

        if (chain == null) {
            chain = new ElPropertyChainBuilder(true, propName);
        }

        // first add this property
        chain.add(this);

        // handle all the rest of the chain handled by the
        // BeanProperty (all depth for nested compound type)
        BeanProperty p = propertyMap.get(remainder);
        if (p != null) {
            return chain.add(p).build();
        }
        CtCompoundPropertyElAdapter elAdapter = nonScalarMap.get(remainder);
        if (elAdapter == null) {
            throw new RuntimeException("property [" + remainder + "] not found in " + getFullBeanName());
        }
        return chain.add(elAdapter).build();
    }

    @Override
    public void appendSelect(DbSqlContext ctx) {
        if (!isTransient) {
            for (int i = 0; i < scalarProperties.length; i++) {
                scalarProperties[i].appendSelect(ctx);
            }
        }
    }

    public BeanProperty[] getScalarProperties() {
        return scalarProperties;
    }

    @Override
    public Object readSet(DbReadContext ctx, Object bean, Class<?> type) throws SQLException {

        boolean assignable = (type == null || owningType.isAssignableFrom(type));

        Object v = compoundType.read(ctx.getDataReader());
        if (assignable) {
            setValue(bean, v);
        }

        return v;
    }

    /**
     * Read the data from the resultSet effectively ignoring it and returning
     * null.
     */
    @Override
    public Object read(DbReadContext ctx) throws SQLException {

        return compoundType.read(ctx.getDataReader());
    }

    @Override
    public void loadIgnore(DbReadContext ctx) {
        compoundType.loadIgnore(ctx.getDataReader());
    }

    @Override
    public void load(SqlBeanLoad sqlBeanLoad) throws SQLException {
        sqlBeanLoad.load(this);
    }

    @Override
    public Object elGetReference(Object bean) {
        return bean;
    }

    public void jsonWrite(WriteJsonContext ctx, Object bean) {
        //FIXME : jsonWrite on Compound IVO
        super.jsonWrite(ctx, bean);
    }
}
