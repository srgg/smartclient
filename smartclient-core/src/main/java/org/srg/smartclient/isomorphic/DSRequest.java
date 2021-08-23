package org.srg.smartclient.isomorphic;

import java.util.*;

/**
 * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/data/DSRequest.html
 */
public class DSRequest implements IDSRequest {
    @Override
    public Iterator<DSRequest> iterator() {
        return Collections.singleton(this).iterator();
    }

    /**
     * @see <a href="https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/types/DSOperationType.html">DSOperationType</a>
     */
    public enum OperationType {
        ADD,
        CLIENTEXPORT,
        CUSTOM,
        DOWNLOADFILE,
        FETCH,
        GETFILE,
        GETFILEVERSION,
        HASFILE,
        HASFILEVERSION,
        LISTFILES,
        LISTFILEVERSIONS,
        REMOVE,
        REMOVEFILE,
        REMOVEFILEVERSION,
        RENAMEFILE,
        SAVEFILE,
        STORETESTDATA,
        UPDATE,
        VALIDATE,
        VIEWFILE
    }

    /**
     * @see <a href="https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/types/TextMatchStyle.html">TextMatchStyle</a>
     */
    public enum TextMatchStyle {
        EXACT,
        EXACTCASE,
        SUBSTRING,
        STARTSWITH
    }

    private Boolean useStrictJSON;
    private String dataSource;
    private OperationType operationType;
    private String operationId;
    private String componentId;
    private int startRow;
    private int endRow;

    /**
     * https://www.smartclient.com/smartclient-latest/isomorphic/system/reference/?id=attr..DSRequest.outputs
     *
     *
     * The list of fields to return in the response, specified as a comma-separated string (eg, "foo, bar, baz"). You can use this property to indicate to the server that you are only interested in a subset of the fields that would normally be returned.
     * Note that you cannot use this property to request a superset of the fields that would normally be returned, because that would be a security hole. It is possible to configure individual OperationBindings to return extra fields, but this must be done in the server's DataSource descriptor; it cannot be altered on the fly from the client side.
     */
    private String outputs;

    /**
     * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/data/DSRequest.html#getAdditionalOutputs--
     *
     * For fetch, add or update operation, an optional comma separated list of fields to fetch from another, related DataSource.
     * Fields should be specified in the format "localFieldName!relatedDataSourceID.relatedDataSourceFieldName". where relatedDataSourceID is the ID of the related dataSource, and relatedDataSourceFieldName is the field for which you want to fetch related values. The returned field values will be stored on the data returned to the client under the specified localFieldName. Note that this will be applied in addition to any specified outputs.
     *
     * Note that as with DataSourceField.includeFrom, the related dataSource must be linked to the primary datasource via a foreignKey relationship.
     */
    private String additionalOutputs;

    private TextMatchStyle textMatchStyle;
    private List<String> sortBy;
//    private Map<String,Object> data;
    private IDSRequestData data;
    private Map<String,Object>  oldValues;


    public Boolean getUseStrictJSON() {
        return useStrictJSON;
    }

    public DSRequest setUseStrictJSON(Boolean useStrictJSON) {
        this.useStrictJSON = useStrictJSON;
        return this;
    }

    public String getDataSource() {
        return dataSource;
    }

    public DSRequest setDataSource(String dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public DSRequest setOperationType(OperationType operationType) {
        this.operationType = operationType;
        return this;
    }

    public String getOperationId() {
        return operationId;
    }

    public DSRequest setOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public String getComponentId() {
        return componentId;
    }

    public DSRequest setComponentId(String componentId) {
        this.componentId = componentId;
        return this;
    }

    public int getStartRow() {
        return startRow;
    }

    public DSRequest setStartRow(int startRow) {
        this.startRow = startRow;
        return this;
    }

    public int getEndRow() {
        return endRow;
    }

    public DSRequest setEndRow(int endRow) {
        this.endRow = endRow;
        return this;
    }

    public TextMatchStyle getTextMatchStyle() {
        return textMatchStyle;
    }

    public DSRequest setTextMatchStyle(TextMatchStyle textMatchStyle) {
        this.textMatchStyle = textMatchStyle;
        return this;
    }

    public List<String> getSortBy() {
        return sortBy;
    }

    public DSRequest setSortBy(List<String> sortBy) {
        this.sortBy = sortBy;
        return this;
    }

    public IDSRequestData getData() {
        return data;
    }

    public DSRequest setData(IDSRequestData data) {
        this.data = data;
        return this;
    }

    public DSRequest wrapAndSetData(Map<String,Object> data) {
        this.data = new MapData();
        if(this.data instanceof Map m){
            m.putAll(data);
        }

        return this;
    }

    public Map<String, Object> getOldValues() {
        return oldValues;
    }

    public DSRequest setOldValues(Map<String, Object> oldValues) {
        this.oldValues = oldValues;
        return this;
    }

    public String getOutputs() {
        return outputs;
    }

    public DSRequest setOutputs(String outputs) {
        this.outputs = outputs;
        return this;
    }

    public String getAdditionalOutputs() {
        return additionalOutputs;
    }

    public DSRequest setAdditionalOutputs(String additionalOutputs) {
        this.additionalOutputs = additionalOutputs;
        return this;
    }

    public static class MapData extends HashMap implements IDSRequestData {

    }
}
