package org.srg.smartclient.isomorphic;

import org.apache.commons.collections.map.LinkedMap;

import java.util.*;

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
    private TextMatchStyle textMatchStyle;
    private List<String> sortBy;
//    private Map<String,Object> data;
    private IDSRequestData data;
    private Map<String,Object>  oldValues;


    public Boolean getUseStrictJSON() {
        return useStrictJSON;
    }

    public void setUseStrictJSON(Boolean useStrictJSON) {
        this.useStrictJSON = useStrictJSON;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public int getEndRow() {
        return endRow;
    }

    public void setEndRow(int endRow) {
        this.endRow = endRow;
    }

    public TextMatchStyle getTextMatchStyle() {
        return textMatchStyle;
    }

    public void setTextMatchStyle(TextMatchStyle textMatchStyle) {
        this.textMatchStyle = textMatchStyle;
    }

    public List<String> getSortBy() {
        return sortBy;
    }

    public void setSortBy(List<String> sortBy) {
        this.sortBy = sortBy;
    }

    public IDSRequestData getData() {
        return data;
    }

    public void setData(IDSRequestData data) {
        this.data = data;
    }

    public void wrapAndSetData(Map<String,Object> data) {
        this.data = new MapData();
        if(this.data instanceof Map m){
            m.putAll(data);
        }
    }

    public Map<String, Object> getOldValues() {
        return oldValues;
    }

    public void setOldValues(Map<String, Object> oldValues) {
        this.oldValues = oldValues;
    }

    public static class MapData extends LinkedMap implements IDSRequestData {


    }
}
