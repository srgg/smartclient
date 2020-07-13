package org.srg.smartclient.isomorphic;

import java.util.ArrayList;
import java.util.Collections;
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

//    /**
//     * https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=attr..DataSource.schemaBean
//     *
//     * For DataSources that specify autoDeriveSchema, this property indicates the name of the Spring bean,
//     * Hibernate mapping or fully-qualified Java class to use as parent schema.
//     *
//     */
//    private String schemaBean;
//
//    /**
//     * https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=attr..DataSource.autoDeriveSchema
//     *
//     * This property allows you to specify that your DataSource's schema (field definitions)
//     * should be automatically derived from some kind of metadata.
//     */
//    private boolean autoDeriveSchema;

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
        /**
         * It is highly imported:
         *      PKs fields MUST BE teh first ones in the field list
         */
        final List<DSField> pksList = new ArrayList<>(fields.size());
        final List<DSField> othersList = new ArrayList<>(fields.size());

        for (DSField dsf: fields) {
            if (dsf.isPrimaryKey()) {
                pksList.add(dsf);
            } else {
                othersList.add(dsf);
            }
        }

        pksList.addAll(othersList);
        this.fields = Collections.unmodifiableList(pksList);
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

//    public String getSchemaBean() {
//        return schemaBean;
//    }
//
//    public void setSchemaBean(String schemaBean) {
//        this.schemaBean = schemaBean;
//    }
//
//    public boolean isAutoDeriveSchema() {
//        return autoDeriveSchema;
//    }
//
//    public void setAutoDeriveSchema(boolean autoDeriveSchema) {
//        this.autoDeriveSchema = autoDeriveSchema;
//    }

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
