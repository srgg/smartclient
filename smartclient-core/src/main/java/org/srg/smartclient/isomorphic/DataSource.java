package org.srg.smartclient.isomorphic;

import java.util.List;
import java.util.Objects;

// https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSource.html
//https://www.smartclient.com/isomorphic/system/reference/?id=class..DataSource
public class DataSource {
    // https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/types/DSServerType.html
    public enum DSServerType {
        GENERIC,
        HIBERNATE,
        JPA,
        JPA1,
        PROJECTFILE,
        SQL,
        JSON
    }

    private String id;
    private DSServerType serverType;
    private String dbName;
    private String beanClassName;

    private String fileName;
    private List<DSField> fields;

    private String tableName;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public DSServerType getServerType() {
        return serverType;
    }

    public void setServerType(DSServerType serverType) {
        this.serverType = serverType;
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public void setBeanClassName(String beanClassName) {
        this.beanClassName = beanClassName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<DSField> getFields() {
        return fields;
    }

    public void setFields(List<DSField> fields) {
        this.fields = fields;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataSource)) return false;
        DataSource that = (DataSource) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
