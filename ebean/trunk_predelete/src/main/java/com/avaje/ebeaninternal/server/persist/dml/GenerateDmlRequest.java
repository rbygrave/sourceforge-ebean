package com.avaje.ebeaninternal.server.persist.dml;

import java.util.Set;

import com.avaje.ebeaninternal.server.deploy.BeanProperty;

public class GenerateDmlRequest {

    private static final String IS_NULL = " is null";

    private final boolean emptyStringAsNull;

    private final StringBuilder sb = new StringBuilder(100);
    private final StringBuilder insertBindBuffer = new StringBuilder(100);

    private final Set<String> includeProps;

    private final Object oldValues;

    private String prefix;
    private String prefix2;

    private int insertMode;

    /**
     * Create from a PersistRequestBean.
     */
    public GenerateDmlRequest(boolean emptyStringAsNull, Set<String> includeProps, Object oldValues) {
        this.emptyStringAsNull = emptyStringAsNull;
        this.includeProps = includeProps;
        this.oldValues = oldValues;
    }

    /**
     * Create for generating standard all properties DML/SQL.
     */
    public GenerateDmlRequest(boolean emptyStringAsNull) {
        this(emptyStringAsNull, null, null);
    }

    public GenerateDmlRequest append(String s) {
        sb.append(s);
        return this;
    }

    public boolean isDbNull(Object v) {
        return v == null || (emptyStringAsNull && (v instanceof String) && ((String) v).length() == 0);
    }

    public boolean isIncluded(BeanProperty prop) {
        return (includeProps == null || includeProps.contains(prop.getName()));
    }

    public void appendColumnIsNull(String column) {
        appendColumn(column, IS_NULL);
    }

    public void appendColumn(String column) {
        String bind = (insertMode > 0) ? "?" : "=?";
        appendColumn(column, bind);
    }

    public void appendColumn(String column, String suffik) {
        appendColumn(column, "", suffik);
    }

    public void appendColumn(String column, String expr, String suffik) {
        sb.append(prefix);
        sb.append(column);
        sb.append(expr);
        if (insertMode > 0) {
            if (insertMode++ > 1) {
                insertBindBuffer.append(",");
            }
            insertBindBuffer.append(suffik);
        } else {
            sb.append(suffik);
        }

        if (prefix2 != null) {
            prefix = prefix2;
            prefix2 = null;
        }
    }

    public String getInsertBindBuffer() {
        return insertBindBuffer.toString();
    }

    public String toString() {
        return sb.toString();
    }

    public void setWhereMode() {
        this.prefix = " and ";
        this.prefix2 = " and ";
    }

    public void setWhereIdMode() {
        this.prefix = "";
        this.prefix2 = " and ";
    }

    public void setInsertSetMode() {
        this.insertMode = 1;
        this.prefix = "";
        this.prefix2 = ", ";
    }

    public void setUpdateSetMode() {
        this.prefix = "";
        this.prefix2 = ", ";
    }

    public Object getOldValues() {
        return oldValues;
    }

}
