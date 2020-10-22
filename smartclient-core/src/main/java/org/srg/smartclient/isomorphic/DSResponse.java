package org.srg.smartclient.isomorphic;

import java.util.Arrays;
import java.util.List;

// http://gitlab.cserver.com.cn/cserverSoftware/smartclient/blob/abc2d32263e41c2429a7e4d31521524181836f95/WebContent/isomorphic/system/schema/SmartClientOperations.wsdl
public class DSResponse {
    // https://www.smartclient.com/smartgwt-3.0/javadoc/constant-values.html#com.smartgwt.client.rpc.RPCResponse.STATUS_SERVER_TIMEOUT
    public static final int STATUS_SUCCESS = 0;
    private static final int STATUS_FAILURE = -1;
    private static final int STATUS_VALIDATION_ERROR = -4;
    private static final int STATUS_LOGIN_INCORRECT = -5;
    private static final int STATUS_MAX_LOGIN_ATTEMPTS_EXCEEDED = -6;
    private static final int STATUS_LOGIN_REQUIRED = -7;
    private static final int STATUS_LOGIN_SUCCESS = -8;
    private static final int STATUS_TRANSPORT_ERROR = -90;
    private static final int STATUS_SERVER_TIMEOUT = -100;

    private int status;
    private Integer startRow;
    private Integer endRow;
    private Integer totalRows;

    private String operationId;
    private DSRequest.OperationType operationType;
    private Integer queueStatus;
    private Integer transactionNum;

    private DSResponseDataContainer data;
    private List<Object> errors;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Integer getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public Integer getEndRow() {
        return endRow;
    }

    public void setEndRow(int endRow) {
        this.endRow = endRow;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public DSResponseDataContainer getData() {
        return data;
    }

//    public void setData(Object data) {
//        this.data = data;
//    }

    public List<Object> getErrors() {
        return errors;
    }

//    public void setErrors(List<Object> errors) {
//        this.errors = errors;
//    }

    public static DSResponse failure(Throwable t) {
        return failure("%s", Arrays.toString(t.getStackTrace()));
    }

    public static DSResponse failure(String message, Object... args) {
        final DSResponse retVal = new DSResponse();
        retVal.setStatus(DSResponse.STATUS_FAILURE);
        retVal.data = DSResponseDataContainer.createFailure(message, args);
        return retVal;
    }

    public static DSResponse successFetch(int startRow, int endRow, Iterable<DSField> fields, Iterable<Object[]> data) {
        return successFetch(startRow, endRow, -1, fields, data);
    }

    public static DSResponse successFetch(int startRow, int endRow, int totalRows, Iterable<DSField> fields, Iterable<Object[]> data) {
        final DSResponse retVal = new DSResponse();
        retVal.setStartRow(startRow);
        retVal.setEndRow(endRow);
        retVal.setTotalRows(totalRows);
        retVal.data = DSResponseDataContainer.createRaw(fields, data);
        retVal.setStatus(DSResponse.STATUS_SUCCESS);
        return retVal;
    }

    public static DSResponse successUpdate(DSResponseDataContainer dsResponseDataContainer){
        final DSResponse retVal = new DSResponse();
//        retVal.setOperationType(DSRequest.OperationType.UPDATE);
        retVal.setStatus(DSResponse.STATUS_SUCCESS);
        retVal.data = dsResponseDataContainer;
        return retVal;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public DSRequest.OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(DSRequest.OperationType operationType) {
        this.operationType = operationType;
    }

    public Integer getTransactionNum() {
        return transactionNum;
    }

    public void setTransactionNum(Integer transactionNum) {
        this.transactionNum = transactionNum;
    }

    public Integer getQueueStatus() {
        return queueStatus;
    }

    public void setQueueStatus(int queueStatus) {
        this.queueStatus = queueStatus;
    }
}
