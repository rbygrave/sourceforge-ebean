/**
 * Copyright (C) 2006  Robin Bygrave
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

import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.config.EncryptKey;
import com.avaje.ebean.config.dbplatform.DbEncryptFunction;
import com.avaje.ebean.config.dbplatform.DbType;
import com.avaje.ebean.config.ldap.LdapAttributeAdapter;
import com.avaje.ebean.config.lucene.LuceneIndex;
import com.avaje.ebean.text.StringFormatter;
import com.avaje.ebean.text.StringParser;
import com.avaje.ebean.validation.factory.Validator;
import com.avaje.ebeaninternal.server.core.InternString;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor.EntityType;
import com.avaje.ebeaninternal.server.deploy.generatedproperty.GeneratedProperty;
import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebeaninternal.server.el.ElPropertyChainBuilder;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.ldap.LdapPersistenceException;
import com.avaje.ebeaninternal.server.lib.util.StringHelper;
import com.avaje.ebeaninternal.server.query.SqlBeanLoad;
import com.avaje.ebeaninternal.server.reflect.BeanReflectGetter;
import com.avaje.ebeaninternal.server.reflect.BeanReflectSetter;
import com.avaje.ebeaninternal.server.text.json.ReadJsonContext;
import com.avaje.ebeaninternal.server.text.json.WriteJsonContext;
import com.avaje.ebeaninternal.server.type.DataBind;
import com.avaje.ebeaninternal.server.type.ScalarType;
import com.avaje.ebeaninternal.util.ValueUtil;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.persistence.PersistenceException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Description of a property of a bean. Includes its deployment information such
 * as database column mapping information.
 */
public class BeanProperty implements ElPropertyValue {

    /**
     * Advanced bean deployment. To exclude this property from update where
     * clause.
     */
    public static final String EXCLUDE_FROM_UPDATE_WHERE = "EXCLUDE_FROM_UPDATE_WHERE";

    /**
     * Advanced bean deployment. To exclude this property from delete where
     * clause.
     */
    public static final String EXCLUDE_FROM_DELETE_WHERE = "EXCLUDE_FROM_DELETE_WHERE";

    /**
     * Advanced bean deployment. To exclude this property from insert.
     */
    public static final String EXCLUDE_FROM_INSERT = "EXCLUDE_FROM_INSERT";

    /**
     * Advanced bean deployment. To exclude this property from update set
     * clause.
     */
    public static final String EXCLUDE_FROM_UPDATE = "EXCLUDE_FROM_UPDATE";

    /**
     * Flag to mark this at part of the unique id.
     */
    final boolean id;

    /**
     * Flag to make this as a dummy property for unidirecitonal relationships.
     */
    final boolean unidirectionalShadow;

    /**
     * Flag to mark the property as embedded. This could be on
     * BeanPropertyAssocOne rather than here. Put it here for checking Id type
     * (embedded or not).
     */
    final boolean embedded;

    /**
     * Flag indicating if this the version property.
     */
    final boolean version;

    /**
     * Set if this property is nullable.
     */
    final boolean nullable;

    final boolean unique;

    /**
     * Is this property include in database resultSet.
     */
    final boolean dbRead;

    /**
     * Include in DB insert.
     */
    final boolean dbInsertable;

    /**
     * Include in DB update.
     */
    final boolean dbUpdatable;

    /**
     * True if the property is based on a SECONDARY table.
     */
    final boolean secondaryTable;

    final TableJoin secondaryTableJoin;
    final String secondaryTableJoinPrefix;

    /**
     * The property is inherited from a super class.
     */
    final boolean inherited;

    final Class<?> owningType;

    final boolean local;

    /**
     * True if the property is a Clob, Blob LongVarchar or LongVarbinary.
     */
    final boolean lob;

    final boolean isTransient;

    /**
     * The logical bean property name.
     */
    final String name;

    /**
     * The reflected field.
     */
    final Field field;

    /**
     * The bean type.
     */
    final Class<?> propertyType;

    final String dbBind;

    /**
     * The database column. This can include quoted identifiers.
     */
    final String dbColumn;

    final String elPlaceHolder;
    final String elPlaceHolderEncrypted;

    /**
     * Select part of a SQL Formula used to populate this property.
     */
    final String sqlFormulaSelect;

    /**
     * Join part of a SQL Formula.
     */
    final String sqlFormulaJoin;

    final boolean formula;

    /**
     * Set to true if stored encrypted.
     */
    final boolean dbEncrypted;

    final boolean localEncrypted;

    final int dbEncryptedType;

    /**
     * The jdbc data type this maps to.
     */
    final int dbType;

    /**
     * The default value to insert if null.
     */
    final Object defaultValue;

    /**
     * Extra deployment parameters.
     */
    final Map<String, String> extraAttributeMap;

    /**
     * The method used to read the property.
     */
    final Method readMethod;

    /**
     * The method used to write the property.
     */
    final Method writeMethod;

    /**
     * Generator for insert or update timestamp etc.
     */
    final GeneratedProperty generatedProperty;

    final BeanReflectGetter getter;

    final BeanReflectSetter setter;

    final BeanDescriptor<?> descriptor;

    /**
     * Used for non-jdbc native types (java.util.Date Enums etc). Converts from
     * logical to jdbc types.
     */
    @SuppressWarnings("rawtypes")
    final ScalarType scalarType;

    /**
     * For LDAP attributes that have custom conversion.
     */
    final LdapAttributeAdapter ldapAttributeAdapter;

    final Validator[] validators;

    final boolean hasLocalValidators;

    boolean cascadeValidate;

    /**
     * The length or precision for DB column.
     */
    final int dbLength;

    /**
     * The scale for DB column (decimal).
     */
    final int dbScale;

    /**
     * Deployment defined DB column definition.
     */
    final String dbColumnDefn;

    /**
     * DB Constraint (typically check constraint on enum)
     */
    final String dbConstraintExpression;

    final DbEncryptFunction dbEncryptFunction;

    final boolean dynamicSubclassWithInheritance;

    int deployOrder;

    public BeanProperty(DeployBeanProperty deploy) {
        this(null, null, deploy);
    }

    public BeanProperty(BeanDescriptorMap owner, BeanDescriptor<?> descriptor, DeployBeanProperty deploy) {

        this.descriptor = descriptor;
        this.name = InternString.intern(deploy.getName());
        if (descriptor != null) {
            this.dynamicSubclassWithInheritance = (descriptor.isDynamicSubclass() && descriptor.hasInheritance());
        } else {
            this.dynamicSubclassWithInheritance = false;
        }
        this.unidirectionalShadow = deploy.isUndirectionalShadow();
        this.localEncrypted = deploy.isLocalEncrypted();
        this.dbEncrypted = deploy.isDbEncrypted();
        this.dbEncryptedType = deploy.getDbEncryptedType();
        this.dbEncryptFunction = deploy.getDbEncryptFunction();
        this.dbBind = deploy.getDbBind();
        this.dbRead = deploy.isDbRead();
        this.dbInsertable = deploy.isDbInsertable();
        this.dbUpdatable = deploy.isDbUpdateable();

        this.secondaryTable = deploy.isSecondaryTable();
        if (secondaryTable) {
            this.secondaryTableJoin = new TableJoin(deploy.getSecondaryTableJoin(), null);
            this.secondaryTableJoinPrefix = deploy.getSecondaryTableJoinPrefix();
        } else {
            this.secondaryTableJoin = null;
            this.secondaryTableJoinPrefix = null;
        }
        this.isTransient = deploy.isTransient();
        this.nullable = deploy.isNullable();
        this.unique = deploy.isUnique();
        this.dbLength = deploy.getDbLength();
        this.dbScale = deploy.getDbScale();
        this.dbColumnDefn = InternString.intern(deploy.getDbColumnDefn());
        this.dbConstraintExpression = InternString.intern(deploy.getDbConstraintExpression());

        this.inherited = false;// deploy.isInherited();
        this.owningType = deploy.getOwningType();
        this.local = deploy.isLocal();

        this.version = deploy.isVersionColumn();
        this.embedded = deploy.isEmbedded();
        this.id = deploy.isId();
        this.generatedProperty = deploy.getGeneratedProperty();
        this.readMethod = deploy.getReadMethod();
        this.writeMethod = deploy.getWriteMethod();
        this.getter = deploy.getGetter();
        if (descriptor != null && getter == null) {
            if (!unidirectionalShadow) {
                String m = "Null Getter for: " + getFullBeanName();
                throw new RuntimeException(m);
            }
        }
        this.setter = deploy.getSetter();

        this.dbColumn = tableAliasIntern(descriptor, deploy.getDbColumn(), false, null);
        this.sqlFormulaJoin = InternString.intern(deploy.getSqlFormulaJoin());
        this.sqlFormulaSelect = InternString.intern(deploy.getSqlFormulaSelect());
        this.formula = sqlFormulaSelect != null;

        this.extraAttributeMap = deploy.getExtraAttributeMap();
        this.defaultValue = deploy.getDefaultValue();
        this.dbType = deploy.getDbType();
        this.scalarType = deploy.getScalarType();
        this.ldapAttributeAdapter = deploy.getLdapAttributeAdapter();
        this.lob = isLobType(dbType);
        this.propertyType = deploy.getPropertyType();
        this.field = deploy.getField();
        this.validators = deploy.getValidators();
        this.hasLocalValidators = (validators.length > 0);

        EntityType et = descriptor == null ? null : descriptor.getEntityType();
        this.elPlaceHolder = tableAliasIntern(descriptor, deploy.getElPlaceHolder(et), false, null);
        this.elPlaceHolderEncrypted = tableAliasIntern(descriptor, deploy.getElPlaceHolder(et), dbEncrypted, dbColumn);
    }

    private String tableAliasIntern(BeanDescriptor<?> descriptor, String s, boolean dbEncrypted, String dbColumn) {
        if (descriptor != null) {
            s = StringHelper.replaceString(s, "${ta}.", "${}");
            s = StringHelper.replaceString(s, "${ta}", "${}");

            if (dbEncrypted) {
                s = dbEncryptFunction.getDecryptSql(s);
                String namedParam = ":encryptkey_" + descriptor.getBaseTable() + "___" + dbColumn;
                s = StringHelper.replaceString(s, "?", namedParam);
            }
        }
        return InternString.intern(s);
    }

    /**
     * Create a Matching BeanProperty with some attributes overridden.
     * <p>
     * Primarily for supporting Embedded beans with overridden dbColumn
     * mappings.
     * </p>
     */
    public BeanProperty(BeanProperty source, BeanPropertyOverride override) {

        this.descriptor = source.descriptor;
        this.name = InternString.intern(source.getName());
        this.dynamicSubclassWithInheritance = source.dynamicSubclassWithInheritance;

        this.dbColumn = InternString.intern(override.getDbColumn());
        this.sqlFormulaJoin = InternString.intern(override.getSqlFormulaJoin());
        this.sqlFormulaSelect = InternString.intern(override.getSqlFormulaSelect());
        this.formula = sqlFormulaSelect != null;

        this.unidirectionalShadow = source.unidirectionalShadow;
        this.localEncrypted = source.isLocalEncrypted();
        this.isTransient = source.isTransient();
        this.secondaryTable = source.isSecondaryTable();
        this.secondaryTableJoin = source.secondaryTableJoin;
        this.secondaryTableJoinPrefix = source.secondaryTableJoinPrefix;

        this.dbBind = source.getDbBind();
        this.dbEncrypted = source.isDbEncrypted();
        this.dbEncryptedType = source.getDbEncryptedType();
        this.dbEncryptFunction = source.dbEncryptFunction;
        this.dbRead = source.isDbRead();
        this.dbInsertable = source.isDbInsertable();
        this.dbUpdatable = source.isDbUpdatable();
        this.nullable = source.isNullable();
        this.unique = source.isUnique();
        this.dbLength = source.getDbLength();
        this.dbScale = source.getDbScale();
        this.dbColumnDefn = InternString.intern(source.getDbColumnDefn());
        this.dbConstraintExpression = InternString.intern(source.getDbConstraintExpression());

        this.inherited = source.isInherited();
        this.owningType = source.owningType;
        this.local = owningType.equals(descriptor.getBeanType());

        this.version = source.isVersion();
        this.embedded = source.isEmbedded();
        this.id = source.isId();
        this.generatedProperty = source.getGeneratedProperty();
        this.readMethod = source.getReadMethod();
        this.writeMethod = source.getWriteMethod();
        this.getter = source.getter;
        this.setter = source.setter;
        this.extraAttributeMap = source.extraAttributeMap;
        this.defaultValue = source.getDefaultValue();
        this.dbType = source.getDbType();
        this.scalarType = source.scalarType;
        this.ldapAttributeAdapter = source.ldapAttributeAdapter;
        this.lob = isLobType(dbType);
        this.propertyType = source.getPropertyType();
        this.field = source.getField();
        this.validators = source.getValidators();
        this.hasLocalValidators = validators.length > 0;

        this.elPlaceHolder = override.replace(source.elPlaceHolder, source.dbColumn);
        this.elPlaceHolderEncrypted = override.replace(source.elPlaceHolderEncrypted, source.dbColumn);
    }

    /**
     * Initialise the property before returning to client code. Used to
     * initialise variables that can't be done in construction due to recursive
     * issues.
     */
    public void initialise() {
        // do nothing for normal BeanProperty
        if (!isTransient && scalarType == null) {
            String msg = "No ScalarType assigned to " + descriptor.getFullName() + "." + getName();
            throw new RuntimeException(msg);
        }
    }

    /**
     * Return the order this property appears in the bean.
     */
    public int getDeployOrder() {
        return deployOrder;
    }

    /**
     * Set the order this property appears in the bean.
     */
    public void setDeployOrder(int deployOrder) {
        this.deployOrder = deployOrder;
    }

    public ElPropertyValue buildElPropertyValue(String propName, String remainder, ElPropertyChainBuilder chain,
            boolean propertyDeploy) {
        throw new PersistenceException("Not valid on scalar bean property " + getFullBeanName());
    }

    /**
     * Return the BeanDescriptor that owns this property.
     */
    public BeanDescriptor<?> getBeanDescriptor() {
        return descriptor;
    }

    /**
     * Return true is this is a simple scalar property.
     */
    public boolean isScalar() {
        return true;
    }

    /**
     * Return true if this property is based on a formula.
     */
    public boolean isFormula() {
        return formula;
    }

    public boolean hasChanged(Object bean, Object oldValues) {
        Object value = getValue(bean);
        Object oldVal = getValue(oldValues);

        return !ValueUtil.areEqual(value, oldVal);
    }

    public void copyProperty(Object sourceBean, Object destBean) {
        Object value = getValue(sourceBean);
        setValue(destBean, value);
    }

    public void copyProperty(Object sourceBean, Object destBean, CopyContext ctx, int maxDepth) {
        Object value = getValue(sourceBean);
        setValue(destBean, value);
    }

    /**
     * Return the encrypt key for the column matching this property.
     */
    public EncryptKey getEncryptKey() {
        return descriptor.getEncryptKey(this);
    }

    public String getDecryptProperty() {
        return dbEncryptFunction.getDecryptSql(this.getName());
    }

    public String getDecryptProperty(String propertyName) {
        return dbEncryptFunction.getDecryptSql(propertyName);
    }

    public String getDecryptSql() {
        return dbEncryptFunction.getDecryptSql(this.getDbColumn());
    }

    public String getDecryptSql(String tableAlias) {
        return dbEncryptFunction.getDecryptSql(tableAlias + "." + this.getDbColumn());
    }

    /**
     * Add any extra joins required to support this property. Generally a no
     * operation except for a OneToOne exported.
     */
    public void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {
        if (formula && sqlFormulaJoin != null) {
            ctx.appendFormulaJoin(sqlFormulaJoin, forceOuterJoin);

        } else if (secondaryTableJoin != null) {

            String relativePrefix = ctx.getRelativePrefix(secondaryTableJoinPrefix);
            secondaryTableJoin.addJoin(forceOuterJoin, relativePrefix, ctx);
        }
    }

    /**
     * Returns null unless this property is using a secondary table. In that
     * case this returns the logical property prefix.
     */
    public String getSecondaryTableJoinPrefix() {
        return secondaryTableJoinPrefix;
    }

    public void appendSelect(DbSqlContext ctx, boolean subQuery) {
        if (formula) {
            ctx.appendFormulaSelect(sqlFormulaSelect);

        } else if (!isTransient) {

            if (secondaryTableJoin != null) {
                String relativePrefix = ctx.getRelativePrefix(secondaryTableJoinPrefix);
                ctx.pushTableAlias(relativePrefix);
            }

            if (dbEncrypted) {
                String decryptSql = getDecryptSql(ctx.peekTableAlias());
                ctx.appendRawColumn(decryptSql);
                ctx.addEncryptedProp(this);

            } else {
                ctx.appendColumn(dbColumn);
            }

            if (secondaryTableJoin != null) {
                ctx.popTableAlias();
            }
        }
    }

    public boolean isAssignableFrom(Class<?> type) {
        return owningType.isAssignableFrom(type);
    }

    public Object readSetOwning(DbReadContext ctx, Object bean, Class<?> type) throws SQLException {

        try {
            Object value = scalarType.read(ctx.getDataReader());
            if (value == null || bean == null) {
                // not setting the value...
            } else {
                if (owningType.equals(type)) {
                    setValue(bean, value);
                }
            }
            return value;
        } catch (Exception e) {
            String msg = "Error readSet on " + descriptor + "." + name;
            throw new PersistenceException(msg, e);
        }
    }

    public void loadIgnore(DbReadContext ctx) {
        scalarType.loadIgnore(ctx.getDataReader());
    }

    public void load(SqlBeanLoad sqlBeanLoad) throws SQLException {
        sqlBeanLoad.load(this);
    }

    public void buildSelectExpressionChain(String prefix, List<String> selectChain) {
        if (prefix == null) {
            selectChain.add(name);
        } else {
            selectChain.add(prefix + "." + name);
        }
    }

    public Object read(DbReadContext ctx) throws SQLException {
        return scalarType.read(ctx.getDataReader());
    }

    public Object readSet(DbReadContext ctx, Object bean, Class<?> type) throws SQLException {

        try {
            Object value = scalarType.read(ctx.getDataReader());
            if (bean == null || (type != null && !owningType.isAssignableFrom(type))) {
                // not setting the value...
            } else {
                setValue(bean, value);
            }
            return value;
        } catch (Exception e) {
            String msg = "Error readSet on " + descriptor + "." + name;
            throw new PersistenceException(msg, e);
        }
    }

    /**
     * Convert the type to the bean type if required.
     * <p>
     * Generally only used to ensure id properties are converted for
     * Query.setId() use.
     * </p>
     */
    public Object toBeanType(Object value) {
        return scalarType.toBeanType(value);
    }

    @SuppressWarnings("unchecked")
    public void bind(DataBind b, Object value) throws SQLException {
        scalarType.bind(b, value);
    }

    public void writeData(DataOutput dataOutput, Object value) throws IOException {
        scalarType.writeData(dataOutput, value);
    }

    public Object readData(DataInput dataInput) throws IOException {
        return scalarType.readData(dataInput);
    }

    Validator[] getValidators() {
        return validators;
    }

    public boolean isCascadeValidate() {
        return cascadeValidate;
    }

    public boolean hasLocalValidators() {
        return hasLocalValidators;
    }

    public boolean hasValidationRules(boolean cascade) {
        return hasLocalValidators || (cascade && cascadeValidate);
    }

    /**
     * Checks to see if a bean is a reference (will be lazy loaded) or a
     * BeanCollection that has not yet been populated.
     * <p>
     * For base types this returns true.
     * </p>
     */
    public boolean isValueLoaded(Object value) {
        return true;
    }

    /**
     * Cascade the validation to the associated bean or collection.
     */
    public InvalidValue validateCascade(Object value) {
        return null;
    }

    /**
     * Validate the property with the given value.
     * 
     * @param cascade
     *            if true cascade for assoc beans and collections.
     * @param value
     *            the value to validate
     * @return the list of errors that occurred.
     */
    public final List<InvalidValue> validate(boolean cascade, Object value) {

        if (!isValueLoaded(value)) {
            return null;
        }

        ArrayList<InvalidValue> list = null;
        for (int i = 0; i < validators.length; i++) {
            if (!validators[i].isValid(value)) {
                if (list == null) {
                    list = new ArrayList<InvalidValue>();
                }
                Validator v = validators[i];
                list.add(new InvalidValue(v.getKey(), v.getAttributes(), descriptor.getFullName(), name, value));
            }
        }

        if (list == null && cascade && cascadeValidate) {
            // cascade the validation for assoc beans
            InvalidValue recursive = validateCascade(value);
            if (recursive != null) {
                return InvalidValue.toList(recursive);

            }
        }
        return list;
    }

    public BeanProperty getBeanProperty() {
        return this;
    }

    /**
     * Return the getter method.
     */
    public Method getReadMethod() {
        return readMethod;
    }

    /**
     * Return the setter method.
     */
    public Method getWriteMethod() {
        return writeMethod;
    }

    /**
     * Return true if this object is part of an inheritance hierarchy.
     */
    public boolean isInherited() {
        return inherited;
    }

    /**
     * Return true is this type is not from a super type.
     */
    public boolean isLocal() {
        return local;
    }

    public Attribute createAttribute(Object bean) {
        Object v = getValue(bean);
        if (v == null) {
            return null;
        }
        if (ldapAttributeAdapter != null) {
            return ldapAttributeAdapter.createAttribute(v);
        }
        Object ldapValue = scalarType.toJdbcType(v);
        return new BasicAttribute(dbColumn, ldapValue);
    }

    public void setAttributeValue(Object bean, Attribute attr) {
        try {
            if (attr != null) {
                Object beanValue;
                if (ldapAttributeAdapter != null) {
                    beanValue = ldapAttributeAdapter.readAttribute(attr);
                } else {
                    beanValue = scalarType.toBeanType(attr.get());
                }

                setValue(bean, beanValue);
            }
        } catch (NamingException e) {
            throw new LdapPersistenceException(e);
        }
    }

    /**
     * Set the value of the property without interception or
     * PropertyChangeSupport.
     */
    public void setValue(Object bean, Object value) {
        try {
            if (bean instanceof EntityBean) {
                setter.set(bean, value);
            } else {
                Object[] args = new Object[1];
                args[0] = value;
                writeMethod.invoke(bean, args);
            }
        } catch (Exception ex) {
            String beanType = bean == null ? "null" : bean.getClass().getName();
            String msg = "set " + name + " on [" + descriptor + "] arg[" + value + "] type[" + beanType
                    + "] threw error";
            throw new RuntimeException(msg, ex);
        }
    }

    /**
     * Set the value of the property.
     */
    public void setValueIntercept(Object bean, Object value) {
        try {
            if (bean instanceof EntityBean) {
                setter.setIntercept(bean, value);
            } else {
                Object[] args = new Object[1];
                args[0] = value;
                writeMethod.invoke(bean, args);
            }
        } catch (Exception ex) {
            String beanType = bean == null ? "null" : bean.getClass().getName();
            String msg = "setIntercept " + name + " on [" + descriptor + "] arg[" + value + "] type[" + beanType
                    + "] threw error";
            throw new RuntimeException(msg, ex);
        }
    }

    private static Object[] NO_ARGS = new Object[0];

    /**
     * Return the property value taking inheritance into account.
     */
    public Object getValueWithInheritance(Object bean) {
        if (dynamicSubclassWithInheritance) {
            return descriptor.getBeanPropertyWithInheritance(bean, name);
        }
        return getValue(bean);
    }

    /**
     * Return the value of the property method.
     */
    public Object getValue(Object bean) {
        try {
            if (bean instanceof EntityBean) {
                return getter.get(bean);
            } else {
                return readMethod.invoke(bean, NO_ARGS);
            }
        } catch (Exception ex) {
            String beanType = bean == null ? "null" : bean.getClass().getName();
            String msg = "get " + name + " on [" + descriptor + "] type[" + beanType + "] threw error.";
            throw new RuntimeException(msg, ex);
        }
    }

    public Object getValueIntercept(Object bean) {
        try {
            if (bean instanceof EntityBean) {
                return getter.getIntercept(bean);
            } else {
                return readMethod.invoke(bean, NO_ARGS);
            }
        } catch (Exception ex) {
            String beanType = bean == null ? "null" : bean.getClass().getName();
            String msg = "getIntercept " + name + " on [" + descriptor + "] type[" + beanType + "] threw error.";
            throw new RuntimeException(msg, ex);
        }
    }

    public Object elConvertType(Object value) {
        if (value == null) {
            return null;
        }
        return convertToLogicalType(value);
    }

    public void elSetReference(Object bean) {
        throw new RuntimeException("Should not be called");
    }

    public void elSetValue(Object bean, Object value, boolean populate, boolean reference) {
        if (bean != null) {
            setValueIntercept(bean, value);
        }
    }

    public Object elGetValue(Object bean) {
        if (bean == null) {
            return null;
        }
        return getValueIntercept(bean);
    }

    public Object elGetReference(Object bean) {
        throw new RuntimeException("Not expected to call this");
    }

    /**
     * Return the name of the property.
     */
    public String getName() {
        return name;
    }

    public String getElName() {
        return name;
    }

    /**
     * This is a full ElGetValue.
     */
    public boolean isDeployOnly() {
        return false;
    }

    public boolean containsManySince(String sinceProperty) {
        return containsMany();
    }

    public boolean containsMany() {
        return false;
    }

    public Object[] getAssocOneIdValues(Object bean) {
        // Returns null as not an AssocOne.
        return null;
    }

    public String getAssocOneIdExpr(String prefix, String operator) {
        // Returns null as not an AssocOne.
        return null;
    }

    public String getAssocIdInExpr(String prefix) {
        // Returns null as not an AssocOne.
        return null;
    }

    public String getAssocIdInValueExpr(int size) {
        // Returns null as not an AssocOne.
        return null;
    }

    public boolean isAssocId() {
        // Returns false - override in BeanPropertyAssocOne.
        return false;
    }

    public boolean isAssocProperty() {
        // Returns false - override in BeanPropertyAssocOne.
        return false;
    }

    public String getElPlaceholder(boolean encrypted) {
        return encrypted ? elPlaceHolderEncrypted : elPlaceHolder;
    }

    public String getElPrefix() {
        return secondaryTableJoinPrefix;
    }

    /**
     * Return the full name of this property.
     */
    public String getFullBeanName() {
        return descriptor.getFullName() + "." + name;
    }

    /**
     * Return the scalarType.
     */
    public ScalarType<?> getScalarType() {
        return scalarType;
    }

    public StringFormatter getStringFormatter() {
        return scalarType;
    }

    public StringParser getStringParser() {
        return scalarType;
    }

    public boolean isDateTimeCapable() {
        return scalarType != null && scalarType.isDateTimeCapable();
    }

    public Object parseDateTime(long systemTimeMillis) {
        return scalarType.parseDateTime(systemTimeMillis);
    }

    /**
     * Return the DB max length (varchar) or precision (decimal).
     */
    public int getDbLength() {
        return dbLength;
    }

    /**
     * Return the DB scale for numeric columns.
     */
    public int getDbScale() {
        return dbScale;
    }

    /**
     * Return a specific column DDL definition if specified (otherwise null).
     */
    public String getDbColumnDefn() {
        return dbColumnDefn;
    }

    /**
     * Return the DB constraint expression (can be null).
     * <p>
     * For an Enum returns IN expression for the set of Enum values.
     * </p>
     */
    public String getDbConstraintExpression() {
        return dbConstraintExpression;
    }

    /**
     * Return the DB column type definition.
     */
    public String renderDbType(DbType dbType) {
        if (dbColumnDefn != null) {
            return dbColumnDefn;
        }
        return dbType.renderType(dbLength, dbScale);
    }

    /**
     * Return the bean Field associated with this property.
     */
    public Field getField() {
        return field;
    }

    /**
     * Return the GeneratedValue. Used to generate update timestamp etc.
     */
    public GeneratedProperty getGeneratedProperty() {
        return generatedProperty;
    }

    /**
     * Return true if this property is mandatory.
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * Return true if DDL Not NULL constraint should be defined for this column
     * based on it being a version column or having a generated property.
     */
    public boolean isDDLNotNull() {
        return isVersion() || (generatedProperty != null && generatedProperty.isDDLNotNullable());
    }

    /**
     * Return true if the DB column should be unique.
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Return true if the property is transient.
     */
    public boolean isTransient() {
        return isTransient;
    }

    /**
     * Return true if this is a version column used for concurrency checking.
     */
    public boolean isVersion() {
        return version;
    }

    public String getDeployProperty() {
        return dbColumn;
    }

    /**
     * The database column name this is mapped to.
     */
    public String getDbColumn() {
        return dbColumn;
    }

    /**
     * Return the database jdbc data type this is mapped to.
     */
    public int getDbType() {
        return dbType;
    }

    /**
     * Perform DB to Logical type conversion (if necessary).
     */
    public Object convertToLogicalType(Object value) {
        if (scalarType != null) {
            return scalarType.toBeanType(value);
        }
        return value;
    }

    private ArrayList<LuceneIndex> luceneIndexes;

    public void registerLuceneIndex(LuceneIndex luceneIndex) {
        if (luceneIndexes == null) {
            luceneIndexes = new ArrayList<LuceneIndex>();
        }
        luceneIndexes.add(luceneIndex);
    }

    public boolean isDeltaRequired() {
        return luceneIndexes != null;
    }

    /**
     * Return true if this is mapped to a Clob Blob LongVarchar or
     * LongVarbinary.
     */
    public boolean isLob() {
        return lob;
    }

    private boolean isLobType(int type) {
        switch (type) {
        case Types.CLOB:
            return true;
        case Types.BLOB:
            return true;
        case Types.LONGVARBINARY:
            return true;
        case Types.LONGVARCHAR:
            return true;

        default:
            return false;
        }
    }

    /**
     * Return the DB bind parameter. Typically is "?" but different for
     * encrypted bind.
     */
    public String getDbBind() {
        return dbBind;
    }

    /**
     * Returns true if DB encrypted.
     */
    public boolean isLocalEncrypted() {
        return localEncrypted;
    }

    /**
     * Return true if this property is stored encrypted.
     */
    public boolean isDbEncrypted() {
        return dbEncrypted;
    }

    public int getDbEncryptedType() {
        return dbEncryptedType;
    }

    /**
     * Return true if this property should be included in an Insert.
     */
    public boolean isDbInsertable() {
        return dbInsertable;
    }

    /**
     * Return true if this property should be included in an Update.
     */
    public boolean isDbUpdatable() {
        return dbUpdatable;
    }

    /**
     * Return true if this property is included in database queries.
     */
    public boolean isDbRead() {
        return dbRead;
    }

    /**
     * Return true if this property is based on a secondary table (not the base
     * table).
     */
    public boolean isSecondaryTable() {
        return secondaryTable;
    }

    /**
     * Return the property type.
     */
    public Class<?> getPropertyType() {
        return propertyType;
    }

    /**
     * Return true if this is included in the unique id.
     */
    public boolean isId() {
        return id;
    }

    /**
     * Return true if this is an Embedded property. In this case it shares the
     * table and primary key of its owner object.
     */
    public boolean isEmbedded() {
        return embedded;
    }

    /**
     * Return an extra attribute set on this property.
     */
    public String getExtraAttribute(String key) {
        return extraAttributeMap.get(key);
    }

    /**
     * Return the default value.
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    public String toString() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public void jsonWrite(WriteJsonContext ctx, Object bean) {

        Object value = getValueIntercept(bean);
        if (value == null) {
            ctx.appendNull(name);
        } else {
            String jv = scalarType.jsonToString(value, ctx.getValueAdapter());
            ctx.appendKeyValue(name, jv);
        }
    }

    public void jsonRead(ReadJsonContext ctx, Object bean) {

        String jsonValue = ctx.readScalarValue();

        Object objValue;
        if (jsonValue == null) {
            objValue = null;
        } else {
            objValue = scalarType.jsonFromString(jsonValue, ctx.getValueAdapter());
        }
        setValue(bean, objValue);
    }
}
