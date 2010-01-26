package com.avaje.ebean.server.deploy;

import com.avaje.ebean.server.el.ElPropertyValue;
import com.avaje.ebean.text.StringParser;

/**
 * Used to evaluate imported foreign keys so as to avoid unnecessary joins.
 */
public final class BeanFkeyProperty implements ElPropertyValue {

    private final String placeHolder;
    private final String prefix;
    private final String name;
    private final String dbColumn;

    public BeanFkeyProperty(String name, String dbColumn) {
        this(null, name, dbColumn);
    }

    public BeanFkeyProperty(String prefix, String name, String dbColumn) {
        this.prefix = prefix;
        this.name = name;
        this.dbColumn = dbColumn;
        this.placeHolder = calcPlaceHolder(prefix, dbColumn);

    }

    private String calcPlaceHolder(String prefix, String dbColumn) {
        if (prefix != null) {
            return "${" + prefix + "}" + dbColumn;
        } else {
            return ROOT_ELPREFIX + dbColumn;
        }
    }

    public BeanFkeyProperty create(String expression) {
        int len = expression.length() - name.length() - 1;
        String prefix = expression.substring(0, len);

        return new BeanFkeyProperty(prefix, name, dbColumn);
    }

    /**
     * Returns false for keys.
     */
    public boolean isDbEncrypted() {
        return false;
    }

    /**
     * Returns false for keys.
     */
    public boolean isLocalEncrypted() {
        return false;
    }

    /**
     * Only usable as ElPropertyDeploy.
     */
    public boolean isDeployOnly() {
        return true;
    }

    /**
     * Returns false.
     */
    public boolean containsMany() {
        return false;
    }

    public String getDbColumn() {
        return dbColumn;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns null as not an AssocOne.
     */
    public Object[] getAssocOneIdValues(Object value) {
        return null;
    }

    /**
     * Returns null as not an AssocOne.
     */
    public String getAssocOneIdExpr(String prefix, String operator) {
        return null;
    }

    /**
     * Returns false as not an AssocOne.
     */
    public boolean isAssocOneId() {
        return false;
    }

    public String getElPlaceholder() {
        return placeHolder;
    }

    public String getElPrefix() {
        return prefix;
    }

    public boolean isDateTimeCapable() {
        return false;
    }

    public Object parseDateTime(long systemTimeMillis) {
        throw new RuntimeException("ElPropertyDeploy only - not implemented");
    }

    public StringParser getStringParser() {
        throw new RuntimeException("ElPropertyDeploy only - not implemented");
    }

    public void elSetReference(Object bean) {
        throw new RuntimeException("ElPropertyDeploy only - not implemented");
    }

    public Object elConvertType(Object value) {
        throw new RuntimeException("ElPropertyDeploy only - not implemented");
    }

    public void elSetValue(Object bean, Object value, boolean populate, boolean reference) {
        throw new RuntimeException("ElPropertyDeploy only - not implemented");
    }

    public Object elGetValue(Object bean) {
        throw new RuntimeException("ElPropertyDeploy only - not implemented");
    }

    public Object elGetReference(Object bean) {
        throw new RuntimeException("ElPropertyDeploy only - not implemented");
    }

    public BeanProperty getBeanProperty() {
        throw new RuntimeException("ElPropertyDeploy only - not implemented");
    }

    public String getDeployProperty() {
        throw new RuntimeException("ElPropertyDeploy only - not implemented");
    }

}
