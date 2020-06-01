package org.srg.smartclient.isomorphic;

public final class DSResponseDataContainer {
    public enum ResponseType {
        RAW,
        GENERAL_ERROR
    }

    private ResponseType responseType;
    private RawDataResponse rawDataResponse;
    private String generalFailure;

    public ResponseType getResponseType() {
        return responseType;
    }

    public RawDataResponse getRawDataResponse() {
        if (getResponseType() != ResponseType.RAW) {
            throw new IllegalStateException();
        }
        return rawDataResponse;
    }

    public String getGeneralFailureMessage() {
        if (getResponseType() != ResponseType.GENERAL_ERROR) {
            throw new IllegalStateException();
        }

        return generalFailure;
    }

    public static class RawDataResponse {
        private final Iterable<DSField> fields;
        private final Iterable<Object[]> data;

        public RawDataResponse(Iterable<DSField> fields, Iterable<Object[]> data) {
            this.fields = fields;
            this.data = data;
        }

        public Iterable<DSField> getFields() {
            return fields;
        }

        public Iterable<Object[]> getData() {
            return data;
        }
    }

    public static DSResponseDataContainer createRaw(Iterable<DSField> fields, Iterable<Object[]> data) {
        final DSResponseDataContainer retVal = new DSResponseDataContainer();
        retVal.responseType = ResponseType.RAW;

        retVal.rawDataResponse = new RawDataResponse(fields, data);
        return retVal;
    }

    public static DSResponseDataContainer createFailure(String message, Object...args) {
        final DSResponseDataContainer retVal = new DSResponseDataContainer();
        retVal.responseType = ResponseType.GENERAL_ERROR;
        retVal.generalFailure = String.format(message, args);
        return retVal;
    }
}
