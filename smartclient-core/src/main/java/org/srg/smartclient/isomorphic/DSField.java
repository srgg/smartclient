package org.srg.smartclient.isomorphic;

import java.util.Objects;

// https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSourceField.html
public class DSField {

    // https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/types/FieldType.html
    public enum FieldType {
        ANY,
        BINARY,
        BOOLEAN,
        CREATOR,
        CREATORTIMESTAMP,
        CUSTOM,
        DATE,
        DATETIME,
        ENUM,
        FLOAT,
        IMAGE,
        IMAGEFILE,
        INTEGER,
        INTENUM,
        LINK,
        LOCALECURRENCY,
        LOCALEFLOAT,
        LOCALEINT,
        MODIFIER,
        MODIFIERTIMESTAMP,
        NTEXT,
        PASSWORD,
        PHONENUMBER,
        SEQUENCE,
        TEXT,
        TIME
    }
    private String name;
    private String title;
    private boolean required;
    private boolean primaryKey;
    private boolean hidden;
    private FieldType type;
    private String foreignKey;
    private String foreignDisplayField;
    private String includeFrom;
    private String includeVia;
    private String displayField;
    private String rootValue;
    private String dbName;
    private String sql;

    private boolean canEdit;

    // https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/CustomQuerying.html
    private boolean customSQL;

    public String getTitle() {
        return title;
    }

    public DSField setTitle(String title) {
        this.title = title;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public DSField setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public DSField setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public FieldType getType() {
        return type;
    }

    public DSField setType(FieldType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public DSField setName(String name) {
        this.name = name;
        return this;
    }

    public String getForeignKey() {
        return foreignKey;
    }

    public DSField setForeignKey(String foreignKey) {
        this.foreignKey = foreignKey;
        return this;
    }

    public String getForeignDisplayField() {
        return foreignDisplayField;
    }

    public void setForeignDisplayField(String foreignDisplayField) {
        this.foreignDisplayField = foreignDisplayField;
    }

    public String getIncludeFrom() {
        return includeFrom;
    }

    public void setIncludeFrom(String includeFrom) {
        this.includeFrom = includeFrom;
    }

    public String getIncludeVia() {
        return includeVia;
    }

    public void setIncludeVia(String includeVia) {
        this.includeVia = includeVia;
    }

    public String getDisplayField() {
        return displayField;
    }
    public void setDisplayField(String displayField) {
        this.displayField = displayField;
    }

    public String getRootValue() {
        return rootValue;
    }

    public DSField setRootValue(String rootValue) {
        this.rootValue = rootValue;
        return this;
    }

    public String getDbName() {
        if (dbName == null || dbName.isBlank()) {
            return getName();
        }
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }


    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCustomSQL() {
        return getSql() != null && !getSql().isBlank();
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DSField)) return false;
        DSField dsField = (DSField) o;
        return getName().equals(dsField.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    public boolean isIncludeField() {
        return getIncludeFrom() != null && !getIncludeFrom().isBlank();
    }
}
