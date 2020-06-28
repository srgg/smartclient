package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.DataSource;

import java.util.*;

public abstract class AbstractDSHandler implements DSHandler {
    private static Logger logger = LoggerFactory.getLogger(AbstractDSHandler.class);

    private final IDSRegistry dsRegistry;
    private final DataSource datasource;
    private transient Map<String, DSField> fieldMap;

    public AbstractDSHandler(IDSRegistry dsRegistry, DataSource datasource) {
        this.dsRegistry = dsRegistry;
        this.datasource = datasource;
    }

    protected Map<String, DSField> getFieldMap() {
        if (fieldMap == null) {
            final Map<String, DSField> m = new LinkedHashMap<>();

            for (DSField f: getDataSource().getFields()) {
                m.put(f.getName(), f);
            }

            fieldMap = Collections.unmodifiableMap(m);
        }

        return fieldMap;
    }

    protected DSField getField(String fieldName) {
        return getFieldMap().get(fieldName);
    }

    protected Collection<DSField> getFields() {
        return getFieldMap().values();
    }

    protected DataSource getDataSource() {
        assert datasource != null;
        return datasource;
    }

    @Override
    public String id() {
        return getDataSource().getId();
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
        if (!getDataSource().getId().equalsIgnoreCase(request.getDataSource())) {
            // TODO: Add proper error handling
            throw new IllegalStateException();
        }

        if (request.getOperationType() == null) {
            logger.warn("DSHandler '%s': incoming request does not have operation specified, 'FETCH' operation will be performed by default.".formatted(this.getDataSource().getId()));
            request.setOperationType(DSRequest.OperationType.FETCH);
        }

        switch (request.getOperationType()) {
            case FETCH:
                return handleFetch(request);

            default:
                return failureDueToUnsupportedOperation(request);
        }
    }

    protected DataSource getDataSourceById(String dsId) {
        assert dsRegistry != null;
        return dsRegistry.getDataSourceById(dsId);
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
                            this.getDataSource().getId(),
                            importFromField.getName(),
                            importFromField.getIncludeFrom()
                    )
            );
        }
        final DataSource foreignDS = getDataSourceById(parsedIncludeFrom[0]);

        // -- foreign Display field
        final DSField foreignDisplayField = foreignDS.getFields().stream()
                .filter( f -> f.getName().equals( parsedIncludeFrom[1] ))
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
                            this.getDataSource().getId(),
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
