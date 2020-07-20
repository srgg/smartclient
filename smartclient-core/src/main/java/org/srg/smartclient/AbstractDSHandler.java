package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.*;

import java.util.*;

public abstract class AbstractDSHandler extends SmartClientRelationSupport implements DSHandler {
    private static Logger logger = LoggerFactory.getLogger(AbstractDSHandler.class);

    private final IDSRegistry dsRegistry;
    private final DataSource datasource;
    private transient Map<String, DSField> fieldMap;
    private transient Map<DSRequest.OperationType, List<OperationBinding>> bindingsMap;

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

    protected Map<DSRequest.OperationType, List<OperationBinding>> getBindingsMap() {
        if (bindingsMap == null) {

            if (dataSource().getOperationBindings() == null) {
                bindingsMap = Map.of();
            } else {
                final Map<DSRequest.OperationType, List<OperationBinding>> m = new LinkedHashMap<>();

                for (OperationBinding b : dataSource().getOperationBindings()) {
                    List<OperationBinding> bindings = m.get(b.getOperationType());
                    if (bindings == null) {
                        bindings = new LinkedList<>();
                        m.put(b.getOperationType(), bindings);
                    }

                    bindings.add(b);
                }

                bindingsMap = Collections.unmodifiableMap(m);
            }
        }

        return bindingsMap;
    }

    protected DSField getField(String fieldName) {
        return getFieldMap().get(fieldName);
    }

    public List<DSField> getFields() {
        return datasource.getFields();
    }

    protected DataSource getDataSource() {
        assert datasource != null;
        return datasource;
    }

    @Override
    public String id() {
        return getDataSource().getId();
    }

    @Override
    public DataSource dataSource() {
        return this.datasource;
    }

    protected DSResponse handleFetch(DSRequest request) throws Exception {
        return failureDueToUnsupportedOperation(request);
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

    private DSResponse failureDueToUnsupportedOperation(DSRequest request) {
        return DSResponse.failure("Can't handle request: operation '%s' is not supported by '%s' data source.",
                request.getOperationType(),
                request.getDataSource()
        );
    }

    protected DSHandler getDataSourceHandlerById(String id) {
        final DSHandler dsHandler = dsRegistry.getHandlerByName(id);
        return dsHandler;
    }

    protected DataSource getDataSourceById(String dsId) {
        assert dsRegistry != null;
        return dsRegistry.getDataSourceById(dsId);
    }

    protected ImportFromRelation describeImportFrom(DSField importFromField) {
        return SmartClientRelationSupport.describeImportFrom(dsId -> this.getDataSourceHandlerById(dsId), this.getDataSource(), importFromField);
    }

    protected ForeignKeyRelation describeForeignKey(DSField foreignKeyField) {
        return SmartClientRelationSupport.describeForeignKey(dsId -> this.getDataSourceHandlerById(dsId), this.getDataSource(), foreignKeyField);
    }

    protected ForeignRelation describeForeignRelation(String relation) {
        return SmartClientRelationSupport.describeForeignRelation( dsId -> this.getDataSourceHandlerById(dsId), relation);
    }

    protected ForeignRelation determineEffectiveField(DSField dsf) {
        final DataSource effectiveDS;
        final DSField effectiveField;

        if (dsf.isIncludeField()) {
            final ImportFromRelation relation = describeImportFrom(dsf);
            effectiveDS = relation.foreignDataSource();
            effectiveField = relation.foreignDisplay();
        } else {
            effectiveDS = getDataSource();
            effectiveField = dsf;
        }

        return new ForeignRelation(effectiveDS.getId(), effectiveDS, effectiveField.getName(), effectiveField);
    }

    public OperationBinding getEffectiveOperationBinding(DSRequest.OperationType operationType) {
        final Map<DSRequest.OperationType, List<OperationBinding>> bm = getBindingsMap();

        if (bm == null) {
            return null;
        }

        final List<OperationBinding> bindings = bm.get(operationType);

        if (bindings == null || bindings.isEmpty()) {
            return null;
        }

        final OperationBinding b;
        if (bindings.size() >1) {
            throw new IllegalStateException("Data source '%s': multiple bindings have not been supported yet, operation type '%s'."
                    .formatted(
                            dataSource().getId(),
                            operationType
                    )
            );
        } else {
            b = bindings.get(0);
        }

        return b;
    }
}
