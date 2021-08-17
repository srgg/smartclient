package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.*;
import org.srg.smartclient.runtime.IDSRuntime;

import java.util.*;

public abstract class AbstractDSHandler extends RelationSupport implements DSHandler {
    private static final String META_DATA_PREFIX = "_";
    private static final Logger logger = LoggerFactory.getLogger(AbstractDSHandler.class);

    private final IDSRuntime dsRuntime;
    private final DataSource datasource;
    private transient Map<DSRequest.OperationType, List<OperationBinding>> bindingsMap;

    public AbstractDSHandler(IDSRuntime dsRuntime, DataSource datasource) {
        this.dsRuntime = dsRuntime;
        this.datasource = datasource;
    }

    /**
     *
     * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/data/RestDataSource.html#getMetaDataPrefix--
     */
    protected String getMetaDataPrefix() {
        return META_DATA_PREFIX;
    }

    protected static boolean isSubEntityFetchRequired(DSField dsf){

        return (dsf.isMultiple() && !dsf.isIncludeField())
                || DSField.FieldType.ENTITY.equals(dsf.getType());
    }

    protected static boolean isIncludeSummaryRequired(DSField dsf) {
        return dsf.isIncludeField()
                && dsf.getIncludeSummaryFunction() != null;
    }

    protected Map<DSRequest.OperationType, List<OperationBinding>> getBindingsMap() {
        if (bindingsMap == null) {

            if (dataSource().getOperationBindings() == null) {
                bindingsMap = Map.of();
            } else {
                final Map<DSRequest.OperationType, List<OperationBinding>> m = new LinkedHashMap<>();

                for (OperationBinding b : dataSource().getOperationBindings()) {
                    final List<OperationBinding> bindings = m.computeIfAbsent(b.getOperationType(), k -> new LinkedList<>());
                    bindings.add(b);
                }

                bindingsMap = Collections.unmodifiableMap(m);
            }
        }

        return bindingsMap;
    }

    protected DSField getField(String fieldName) {
        return this.dataSource().getField(fieldName);
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

    protected DSResponse handleUpdate(DSRequest request) throws Exception {
        return failureDueToUnsupportedOperation(request);
    }

    protected DSResponse handleAdd(DSRequest request) throws Exception {
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

        return switch (request.getOperationType()) {
            case FETCH -> handleFetch(request);
            case UPDATE -> handleUpdate(request);
            case ADD -> handleAdd(request);
            default -> failureDueToUnsupportedOperation(request);
        };
    }

    private DSResponse failureDueToUnsupportedOperation(DSRequest request) {
        return DSResponse.failure("Can't handle request: operation '%s' is not supported by '%s' data source.",
                request.getOperationType(),
                request.getDataSource()
        );
    }

    protected IDSRuntime getDsRuntime() {
        return dsRuntime;
    }

    protected DSHandler getDataSourceHandlerById(String id) {
        assert dsRuntime != null;
        return dsRuntime.getDataSourceHandlerById(id);
    }

    protected ImportFromRelation getImportFromRelation(DSField importFromField){
        return getDsRuntime().getImportFromRelation(this.id(), importFromField.getName());
    }

    protected ForeignKeyRelation getForeignKeyRelation(DSField importFromField){
        return getDsRuntime().getForeignKeyRelation(this.id(), importFromField.getName());
    }

    @Deprecated(since ="Use getImportFromRelation() instead", forRemoval = true)
    protected ImportFromRelation describeImportFrom(DSField importFromField) {
        return RelationSupport.describeImportFrom(this::getDataSourceHandlerById, this.getDataSource(), importFromField);
    }

    @Deprecated(since ="Use getForeignKeyRelation() instead", forRemoval = true)
    protected ForeignKeyRelation describeForeignKey(DSField foreignKeyField) {
        return RelationSupport.describeForeignKey(this::getDataSourceHandlerById, this.getDataSource(), foreignKeyField);
    }

    protected ForeignRelation describeForeignRelation(DataSource dataSource, DSField field, String relation) {
        return RelationSupport.ForeignRelation.describeForeignRelation(dataSource, field, this::getDataSourceHandlerById, relation);
    }

    protected ForeignRelation determineEffectiveField(DSField dsf) {
        final DataSource effectiveDS;
        final DSField effectiveField;
        final String effectiveRelatedTableAlias;

        if (dsf.isIncludeField()) {
            final ImportFromRelation relation = getImportFromRelation(dsf);
            effectiveDS = relation.getLast().foreign().dataSource();
            effectiveField = relation.foreignDisplay();
            effectiveRelatedTableAlias = relation.getLast().foreign().getRelatedTableAlias();
        } else {
            effectiveDS = getDataSource();
            effectiveField = dsf;
            // TODO: not sure what effectiveRelatedTableAlias should be there, figure this out later, as have more usecses
            effectiveRelatedTableAlias = null;
        }

        return new ForeignRelation(effectiveDS.getId(), effectiveDS, effectiveField.getName(), effectiveField, null, effectiveRelatedTableAlias);
    }

    protected OperationBinding getEffectiveOperationBinding(DSRequest.OperationType operationType, String operationId) {
        final Map<DSRequest.OperationType, List<OperationBinding>> bm = getBindingsMap();

        if (bm == null) {
            return null;
        }

        final List<OperationBinding> bindings = bm.get(operationType);

        if (bindings == null || bindings.isEmpty()) {
            return null;
        }

        final String effectiveOperationId = operationId == null ? "" : operationId;

        // Search correspondent operation binding by operationId
        final OperationBinding effectiveBinding = bindings.stream()
                .filter( b -> b.getOperationId().equals(effectiveOperationId) )
                .reduce( (d1, d2) -> {
                    throw new RuntimeException( "Can't determine effective operation binding: Data source '%s' " +
                            "has  multiple bindings for operation '%s', it seems that operationId '%s' is not unique."
                                    .formatted(
                                            dataSource().getId(),
                                            operationType,
                                            operationId
                                    ));
                        }).orElse(null);

        return effectiveBinding;
    }
}
