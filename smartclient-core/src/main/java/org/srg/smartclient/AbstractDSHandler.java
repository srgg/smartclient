package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.DataSource;

import java.util.*;

public abstract class AbstractDSHandler implements DSHandler {
    private final DataSource datasource;
    private final IDSDispatcher dispatcher;

    private transient Map<String, DSField> fieldMap;
    private transient Map<String, String> dbFieldMap;

    protected Map<String, DSField> getFieldMap() {
        if (fieldMap == null) {
            final Map<String, DSField> m = new LinkedHashMap<>();

            for (DSField f: getDatasource().getFields()) {
                m.put(f.getName(), f);
            }

            fieldMap = Collections.unmodifiableMap(m);
        }

        return fieldMap;
    }

    protected Map<String, String> getDbFieldMap() {
        if (dbFieldMap == null) {
            final Map<String, String> m = new LinkedHashMap<>();

            // as a positive side effect will trigger fieldMap population
            for ( DSField dsf: getFieldMap().values()) {
                m.put(dsf.getDbName(), dsf.getName());
            }

            dbFieldMap = Collections.unmodifiableMap(m);
        }

        return dbFieldMap;
    }

    protected DSField getField(String fieldName) {
        final DSField retVal = getFieldMap().get(fieldName);
        return retVal;
    }

    protected Set<String> getFieldNames() {
        return getFieldMap().keySet();
    }

    protected Collection<String> getDBFieldNames() {
        return getDbFieldMap().keySet();
    }

    protected Collection<DSField> getFields() {
        return getFieldMap().values();
    }

    public AbstractDSHandler(IDSDispatcher dispatcher, DataSource datasource) {
        this.dispatcher = dispatcher;
        this.datasource = datasource;
    }

    protected DataSource getDatasource() {
        assert datasource != null;
        return datasource;
    }

    @Override
    public String id() {
        return getDatasource().getId();
    }

    protected DSResponse handleFetch(DSRequest request) throws Exception {
        return failureDueToUnsupportedOperation(request);
    }

    private DSResponse failureDueToUnsupportedOperation(DSRequest request) {
        return DSResponse.failure("Can't handle request: operation '%s' is not supported by '%s' data source.",
                request.getOperationType(),
                request.getDataSource()
        );
    }

    @Override
    final public DSResponse handle(DSRequest request) throws Exception {
        if (!getDatasource().getId().equalsIgnoreCase(request.getDataSource())) {
            // TODO: Add proper error handling
            throw new IllegalStateException();
        }

        switch (request.getOperationType()) {
            case FETCH:
                return handleFetch(request);

            default:
                return failureDueToUnsupportedOperation(request);
        }
    }

    protected DataSource getDataSource(String dsId) {
        assert dispatcher != null;
        return dispatcher.dataSource(dsId);
    }

    protected ImportFromRelation describeImportFrom(DSField importFromField) {
        if (!importFromField.isIncludeField()) {
            throw new IllegalStateException();
        }

        // -- foreign DS
        final String parsedIncludeFrom[] = importFromField.getIncludeFrom().split("\\.");
        if (parsedIncludeFrom.length != 2) {
            throw new IllegalStateException("DataSource '%s' field '%s', all 'includeFrom' fields must be prefixed with DataSource ID, but actual value is '%s'."
                    .formatted(
                            this.getDatasource().getId(),
                            importFromField.getName(),
                            importFromField.getIncludeFrom()
                    )
            );
        }
        final DataSource foreignDS = getDataSource(parsedIncludeFrom[0]);

        // -- foreign Display field
        final DSField foreignDisplayField = foreignDS.getFields().stream()
                .filter( f -> f.getDbName().equals( parsedIncludeFrom[1] ))
                .reduce( (d1, d2) -> {
                    throw new IllegalStateException("DataSource '%s' has  not unique field name '%s'."
                            .formatted(
                                    foreignDS.getId(),
                                    parsedIncludeFrom[1]
                            )
                    );
                })
                .get();


        // -- source field
        final DSField sourceField = getFields().stream()
                .filter( f -> f.getDisplayField() != null
                        && f.getDisplayField().equals(importFromField.getName())
                )
                .reduce((d1, d2) -> {
                    throw new IllegalStateException("DataSource '%s' can't determine a sourceField for importFromField  '%s'."
                            .formatted(
                                    foreignDS.getId(),
                                    importFromField.getName()
                            )
                    );
                })
                .get();

        // -- foreign key
        final String parsedForeignKey[] = sourceField.getForeignKey().split("\\.");
        if (parsedForeignKey.length != 2) {
            throw new IllegalStateException("DataSource '%s' field '%s', 'foreignKey' field should be prefixed with DataSource ID, but the actual value is '%s'."
                    .formatted(
                            this.getDatasource().getId(),
                            sourceField.getName(),
                            sourceField.getForeignKey()
                    )
            );
        }

        if (!foreignDS.getId().equals( parsedForeignKey[0])) {
            throw new IllegalStateException("");
        }

        final DSField foreignKey = foreignDS.getFields().stream()
                .filter( f -> f.getDbName().equals( parsedForeignKey[1] ))
                .reduce( (d1, d2) -> {
                    throw new IllegalStateException("DataSource '%s' has  not unique field name '%s'."
                            .formatted(
                                    foreignDS.getId(),
                                    parsedForeignKey[1]
                            )
                    );
                })
                .get();

        return new ImportFromRelation(sourceField, foreignDS, foreignKey, foreignDisplayField);
    }

    @Override
    public DataSource dataSource() {
        return this.datasource;
    }

    protected static record ImportFromRelation(DSField sourceField, DataSource foreignDataSource, DSField foreignKey, DSField foreignDisplay){}
}
