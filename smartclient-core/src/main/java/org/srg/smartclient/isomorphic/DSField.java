package org.srg.smartclient.isomorphic;

import java.util.Map;
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
        TIME,

        /**
         * IS not originally supported by SmartClient.
         * Can be used ONLY in conjunction with foreignKey
         */
        ENTITY
    }

    private String name;
    private String title;
    private boolean required;
    private boolean primaryKey;
    private Boolean hidden;
    private FieldType type;
    private String foreignKey;
    private String foreignDisplayField;
    private String includeFrom;
    private String includeVia;
    private String displayField;
    private String rootValue;
    private String dbName;
    private boolean multiple;

    /**
     * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/CustomQuerying.html
     */
    private String customSelectExpression;

    /**
     * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSourceField.html#valueMap
     */
    private Map valueMap;

    /**
     * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSourceField.html#valueMapEnum
     */
    private String valueMapEnum;

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

    public Boolean isHidden() {
        return hidden;
    }

    public DSField setHidden(Boolean hidden) {
        this.hidden = hidden;
        return this;
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

    public DSField setForeignDisplayField(String foreignDisplayField) {
        this.foreignDisplayField = foreignDisplayField;
        return this;
    }

    public String getIncludeFrom() {
        return includeFrom;
    }

    public DSField setIncludeFrom(String includeFrom) {
        this.includeFrom = includeFrom;
        return this;
    }

    public String getIncludeVia() {
        return includeVia;
    }

    public DSField setIncludeVia(String includeVia) {
        this.includeVia = includeVia;
        return this;
    }

    public String getDisplayField() {
        return displayField;
    }
    public DSField setDisplayField(String displayField) {
        this.displayField = displayField;
        return this;
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

    public DSField setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }


    public boolean isCanEdit() {
        return canEdit;
    }

    public DSField setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
        return this;
    }

    public boolean isCustomSQL() {
        return getCustomSelectExpression() != null && !getCustomSelectExpression().isBlank();
    }

    public String getCustomSelectExpression() {
        return customSelectExpression;
    }

    public DSField setCustomSelectExpression(String customSelectExpression) {
        this.customSelectExpression = customSelectExpression;
        return this;
    }

    public Map getValueMap() {
        return valueMap;
    }

    public DSField setValueMap(Map valueMap) {
        this.valueMap = valueMap;
        return this;
    }

    public String getValueMapEnum() {
        return valueMapEnum;
    }

    public DSField setValueMapEnum(String valueMapEnum) {
        this.valueMapEnum = valueMapEnum;
        return this;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
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
