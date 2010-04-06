package com.avaje.ebeaninternal.server.deploy.id;

import java.sql.SQLException;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.core.DefaultSqlUpdate;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebeaninternal.server.deploy.DbReadContext;
import com.avaje.ebeaninternal.server.deploy.DbSqlContext;
import com.avaje.ebeaninternal.server.type.DataBind;

/**
 * Bind an Id that is an Embedded bean.
 */
public final class IdBinderEmbedded implements IdBinder {

    private final BeanPropertyAssocOne<?> embIdProperty;

    private BeanProperty[] props;

    private BeanDescriptor<?> idDesc;

    private String idInValueSql;
    
    public IdBinderEmbedded(BeanPropertyAssocOne<?> embIdProperty) {

        this.embIdProperty = embIdProperty;
    }

    public void initialise() {
        this.idDesc = embIdProperty.getTargetDescriptor();
        this.props = embIdProperty.getProperties();

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < props.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");

        this.idInValueSql = sb.toString();
    }
    
    public void createLdapNameById(LdapName name, Object id) throws InvalidNameException {

        for (int i = 0; i < props.length; i++) {
            Object v = props[i].getValue(id);
            Rdn rdn = new Rdn(props[i].getDbColumn(), v);
            name.add(rdn);
        }
    }
    
    

    public void createLdapNameByBean(LdapName name, Object bean) throws InvalidNameException {
        Object id = embIdProperty.getValue(bean);
        createLdapNameById(name, id);
    }

    public BeanDescriptor<?> getIdBeanDescriptor() {
        return idDesc;
    }

    public int getPropertyCount() {
        return props.length;
    }

    public String getIdProperty() {
        return embIdProperty.getName();
    }

    public BeanProperty findBeanProperty(String dbColumnName) {
        for (int i = 0; i < props.length; i++) {
            if (dbColumnName.equalsIgnoreCase(props[i].getDbColumn())) {
                return props[i];
            }
        }
        return null;
    }

    public boolean isComplexId() {
        return true;
    }

    public String getDefaultOrderBy() {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < props.length; i++) {
            if (i > 0) {
                sb.append(",");
            }

            sb.append(embIdProperty.getName());
            sb.append(".");
            sb.append(props[i].getName());
        }

        return sb.toString();
    }

    public BeanProperty[] getProperties() {
        return props;
    }

    public void addIdInBindValue(SpiExpressionRequest request, Object value) {
        for (int i = 0; i < props.length; i++) {
            request.addBindValue(props[i].getValue(value));
        }
    }

    public void addIdInValueSql(SpiExpressionRequest request) {
        request.append(idInValueSql);
    }

    public Object[] getIdValues(Object bean) {
        bean = embIdProperty.getValue(bean);
        Object[] bindvalues = new Object[props.length];
        for (int i = 0; i < props.length; i++) {
            bindvalues[i] = props[i].getValue(bean);
        }
        return bindvalues;
    }

    public Object[] getBindValues(Object value) {

        Object[] bindvalues = new Object[props.length];
        for (int i = 0; i < props.length; i++) {
            bindvalues[i] = props[i].getValue(value);
        }
        return bindvalues;
    }

    public void bindId(DefaultSqlUpdate sqlUpdate, Object value) {
        for (int i = 0; i < props.length; i++) {
            Object embFieldValue = props[i].getValue(value);
            sqlUpdate.addParameter(embFieldValue);
        }
    }

    public void bindId(DataBind dataBind, Object value) throws SQLException {

        for (int i = 0; i < props.length; i++) {
            Object embFieldValue = props[i].getValue(value);
            props[i].bind(dataBind, embFieldValue);
        }
    }

    public void loadIgnore(DbReadContext ctx) {
        for (int i = 0; i < props.length; i++) {
            props[i].loadIgnore(ctx);
        }
    }

    public Object read(DbReadContext ctx) throws SQLException {

        Object embId = idDesc.createVanillaBean();
        boolean notNull = true;

        for (int i = 0; i < props.length; i++) {
            Object value = props[i].readSet(ctx, embId, null);
            if (value == null) {
                notNull = false;
            }
        }

        if (notNull) {
            return embId;
        } else {
            return null;
        }
    }

    public Object readSet(DbReadContext ctx, Object bean) throws SQLException {

        Object embId = read(ctx);
        if (embId != null) {
            embIdProperty.setValue(bean, embId);
            return embId;
        } else {
            return null;
        }
    }

    public void appendSelect(DbSqlContext ctx) {
        for (int i = 0; i < props.length; i++) {
            props[i].appendSelect(ctx);
        }
    }
        
    public String getAssocIdInValueExpr() {
        return idInValueSql;
    }
        
    public String getAssocIdInExpr(String prefix) {

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < props.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            if (prefix != null) {
                sb.append(prefix);
                sb.append(".");
            }
            sb.append(props[i].getName());
        }
        sb.append(")");
        return sb.toString();
    }

    public String getAssocOneIdExpr(String prefix, String operator) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < props.length; i++) {
            if (i > 0) {
                sb.append(" and ");
            }
            if (prefix != null) {
                sb.append(prefix);
                sb.append(".");
            }

            sb.append(embIdProperty.getName());
            sb.append(".");            
            sb.append(props[i].getName());
            sb.append(" ").append(operator);
            sb.append(" ? ");
        }
        return sb.toString();
    }

    public String getBindIdSql(String baseTableAlias) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < props.length; i++) {
            if (i > 0) {
                sb.append(" and ");
            }
            if (baseTableAlias != null) {
                sb.append(baseTableAlias);
                sb.append(".");
            }
            sb.append(props[i].getDbColumn());
            sb.append(" = ? ");
        }
        return sb.toString();
    }

    public String getBindIdInSql(String baseTableAlias) {

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < props.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(baseTableAlias);
            sb.append(".");
            sb.append(props[i].getDbColumn());
        }
        sb.append(")");
        return sb.toString();
    }

    public Object convertSetId(Object idValue, Object bean) {

        // can not cast/convert if it is embedded
        if (bean != null) {
            // support PropertyChangeSupport
            embIdProperty.setValueIntercept(bean, idValue);
        }

        return idValue;
    }
}
