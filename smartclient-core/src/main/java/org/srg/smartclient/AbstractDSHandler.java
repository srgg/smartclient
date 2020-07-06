package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.DataSource;

import java.util.*;

public abstract class AbstractDSHandler extends RelationSupport implements DSHandler {
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

    protected DSHandler getDataSourceHandlerById(String id) {
        final DSDispatcher dispatcher = (DSDispatcher) dsRegistry;
        final DSHandler dsHandler = dispatcher.getHandlerByName(id);
        return dsHandler;
    }

    protected DataSource getDataSourceById(String dsId) {
        assert dsRegistry != null;
        return dsRegistry.getDataSourceById(dsId);
    }

    protected ImportFromRelation describeImportFrom(DSField importFromField) {
        return RelationSupport.describeImportFrom(dsId -> this.getDataSourceById(dsId), this.getDataSource(), importFromField);
    }

    protected ForeignKeyRelation describeForeignKey(DSField foreignKeyField) {
        return RelationSupport.describeForeignKey(dsId -> this.getDataSourceById(dsId), this.getDataSource(), foreignKeyField);
    }

    @Override
    public DataSource dataSource() {
        return this.datasource;
    }
}
