package org.srg.smartclient;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RelationSupport {

    public interface IForeignKeyRelation {
        DataSource dataSource();
        DSField sourceField();
        boolean isInverse();

        ForeignRelation foreign();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static record ForeignKeyRelation (
            DataSource dataSource,
            DSField sourceField,
            boolean isInverse,

            ForeignRelation foreign
    ) implements IForeignKeyRelation {
        @Override
        public String toString() {
            return "ForeignKeyRelation{" +
                    "dataSource=" + dataSource.getId() +
                    ", sourceField=" + sourceField.getName() +
                    ", isInverse=" + isInverse +
                    ", foreign=" + foreign +
                    '}';
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static record ImportFromRelation(
            DataSource dataSource,
            DSField sourceField,

            List<ForeignKeyRelation> foreignKeyRelations,
            DSField foreignDisplay
    ) {
        protected ForeignKeyRelation getLast() {
            if (foreignKeyRelations.isEmpty()) {
                throw new IllegalStateException();
            }

            return foreignKeyRelations.get(foreignKeyRelations.size() - 1);
        }

        protected ForeignKeyRelation get1st() {
            if (foreignKeyRelations.isEmpty()) {
                throw new IllegalStateException();
            }

            return foreignKeyRelations.get(0);
        }

        @Override
        public String toString() {
            return "ImportFromRelation{ " +
                    "FOREIGN KEY " + dataSource.getId() + "(" + sourceField.getName() + ") \n" +
                    "  REFERENCES " + foreignKeyRelations.toString() + ")\n" +
                    "  DISPLAYS (" + foreignDisplay.getName() + ")" +
                    '}';
        }

        /**
         * Converts {@code ImportFromRelation} to {@code ForeignKeyRelation} as is.
         */
        public ForeignKeyRelation toForeignKeyRelation() {
            final ForeignKeyRelation fkrl = get1st();

            return new ForeignKeyRelation(
                    fkrl.dataSource,
                    fkrl.sourceField,
                    false, // no chance to determine this at this point/level
                    new ForeignRelation(
                            fkrl.foreign.dataSource().getId(),
                            fkrl.foreign.dataSource(),
                            fkrl.foreign.field().getName(),
                            fkrl.foreign.field()
                    )
            );
        }

        /**
         * Converts {@code ImportFromRelation} to {@code ForeignKeyRelation} but replacing
         * {@code #foreignKey} by the {@foreignDisplay}.
         */
        public ForeignKeyRelation toForeignDisplayKeyRelation() {
            assert foreignDisplay != null;
            final ForeignKeyRelation fkrl = get1st();

            return new ForeignKeyRelation(
                    fkrl.dataSource,
                    fkrl.sourceField,
                    false, // no chance to determine this at this point/level
                    new ForeignRelation(
                            fkrl.foreign.dataSource().getId(),
                            fkrl.foreign.dataSource(),
                            this.foreignDisplay.getName(),
                            this.foreignDisplay
                    )
            );
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class ForeignRelation {
        private final String dataSourceId;
        private final DataSource dataSource;
        private final String fieldName;
        private final DSField field;
        private final String relatedTableAlias;

        private String sqlFieldAlias;

        protected ForeignRelation(String dataSourceId, DataSource dataSource, String fieldName, DSField field) {
            this(dataSourceId, dataSource, fieldName, field, null, null);
        }

        protected ForeignRelation(String dataSourceId, DataSource dataSource, String fieldName, DSField field, String sqlFieldAlias, String relatedTableAlias) {
            this.dataSourceId = dataSourceId;
            this.dataSource = dataSource;
            this.fieldName = fieldName;
            this.field = field;
            this.sqlFieldAlias = sqlFieldAlias;
            this.relatedTableAlias = relatedTableAlias;
        }

        public String dataSourceId() {
            return dataSourceId;
        }

        public DataSource dataSource() {
            return dataSource;
        }

        public String fieldName() {
            return fieldName;
        }

        public DSField field() {
            return field;
        }

        public String getSqlFieldAlias() {
            return sqlFieldAlias;
        }

        public String getRelatedTableAlias() {
            return relatedTableAlias;
        }

        public ForeignRelation createWithSqlFieldAlias(String sqlFieldAlias) {
            final ForeignRelation effective = new ForeignRelation(
                    this.dataSourceId(),
                    this.dataSource(),
                    this.fieldName(),
                    this.field(),
                    sqlFieldAlias,
                    null
            );

            return effective;
        }

        public String formatAsSQL() {
            return formatAsSQL(this.getRelatedTableAlias() == null || this.getRelatedTableAlias().isBlank() ? dataSource().getTableName() : this.getRelatedTableAlias());
        }

        public String formatAsSQL(String aliasOrTable) {
            if (aliasOrTable == null || aliasOrTable.isBlank()) {
                return field().getDbName();
            }

            return "%s.%s".formatted(
                    aliasOrTable,
                    getSqlFieldAlias() != null ? getSqlFieldAlias() : field().getDbName()
            );

        }

        @Override
        public String toString() {
            return "ForeignRelation{" +
                    "dataSourceId='" + dataSourceId + '\'' +
                    ", fieldName='" + fieldName + '\'' +
                    '}';
        }

        public static ForeignRelation describeForeignRelation(DataSource ds, DSField field, IDSLookup dsRegistry, String relation) {
            final String parsedIncludeFrom[] = relation.trim().split("\\.");

            if (parsedIncludeFrom.length != 2) {
                throw new IllegalStateException(
                        ("Can't determine ForeignRelation for relation  '%s.%s': '%s', " +
                                "'foreignKey' field MUST be prefixed with a DataSource ID."
                        ).formatted(ds.getId(), field.getName(), relation)
                );
            }

            final String foreignDsId = parsedIncludeFrom[0].trim();
            final String foreignDsFieldName = parsedIncludeFrom[1].trim();

            final DataSource foreignDS = dsRegistry.getDataSourceById(foreignDsId);

            final DSField foreignField;
            if (foreignDS != null) {
                // -- foreign field
                foreignField = foreignDS.getFields().stream()
                        .filter(f -> f.getName().equals(foreignDsFieldName))
                        .reduce((d1, d2) -> {
                            throw new IllegalStateException(("Can't determine ForeignRelation for relation '%s': " +
                                    "foreign datasource '%s' does not have unique field name '%s'.")
                                    .formatted(
                                            relation,
                                            foreignDsId,
                                            foreignDsFieldName
                                    )
                            );
                        })
                        .orElseThrow(() -> {
                            throw new IllegalStateException(("Can't determine ForeignRelation for relation '%s': " +
                                    "foreign datasource '%s' nothing known about field with name '%s'.")
                                    .formatted(
                                            relation,
                                            foreignDsId,
                                            foreignDsFieldName
                                    )
                            );
                        });

            } else {
                foreignField = null;
            }

            return new ForeignRelation(foreignDsId, foreignDS, foreignDsFieldName, foreignField);
        }
    }

    private static DSField determineForeignKeyField(DataSource dataSource, DSField importFromField, ForeignRelation foreignRelation) {
        DSField fkField;

        if ( importFromField.getIncludeVia() != null && !importFromField.getIncludeVia().isBlank() ) {
            // Lookup source field by "IncludeVia"
            fkField = dataSource.getFields().stream()
                    .filter(f -> f.getName().equals(importFromField.getIncludeVia()))
                    .reduce((d1, d2) -> {
                        throw new IllegalStateException(("DataSource '%s' can't determine a sourceField for importFromField  '%s' by IncludeVia '%s': " +
                                "there are multiple fields with the name '%s'.")
                                .formatted(
                                        dataSource.getId(),
                                        importFromField.getName(),
                                        importFromField.getIncludeVia(),
                                        importFromField.getIncludeVia()
                                )
                        );
                    })
                    .orElseThrow(()-> {
                        throw new IllegalStateException(("DataSource '%s' can't determine a sourceField for importFromField  '%s' by IncludeVia '%s': " +
                                "there is no field with name '%s'.")
                                .formatted(
                                        dataSource.getId(),
                                        importFromField.getName(),
                                        importFromField.getIncludeVia(),
                                        importFromField.getIncludeVia()
                                )
                        );
                    });
        } else {
            // Lookup source field by "DisplayField"
            fkField = dataSource.getFields().stream()
                    .filter( f -> f.getDisplayField() != null
                            && f.getDisplayField().equals(importFromField.getName())
                    )
                    .reduce((d1, d2) -> {
                        throw new IllegalStateException(("DataSource '%s' can't determine a sourceField for importFromField  '%s' by DisplayField: " +
                                "there are multiple fields that refers to it.")
                                .formatted(
                                        foreignRelation.dataSourceId(),
                                        importFromField.getName()
                                )
                        );
                    })
                    .orElse(null);
        }

        if (fkField == null) {
            // Search for any field with foreignKey pointed to the foreign data source

            final List<DSField> foreignFields =  dataSource.getFields().stream()
                    .filter( f -> f.getForeignKey() != null
                            && f.getForeignKey().startsWith(foreignRelation.dataSourceId())
                    ).collect(Collectors.toList());

            final int size = foreignFields.size();

            switch (size) {
                case 1: fkField = foreignFields.get(0);
                break;

                case 0: throw new IllegalStateException(("DataSource '%s' can't determine a sourceField for importFromField  '%s' by foreign relation: " +
                        "there is no fields with foreignKey pointed out to the foreignDataSource '%s'.")
                        .formatted(
                                foreignRelation.dataSourceId(),
                                importFromField.getName(),
                                foreignRelation.dataSourceId()
                        ));

                default:
                    throw new IllegalStateException(("DataSource '%s' can't determine a sourceField for importFromField  '%s' by foreign relation: " +
                            "there are multiple fields with foreignKey pointed out to the foreignDataSource '%s', consider to use includeVia to point it oit to the exact field.")
                            .formatted(
                                    foreignRelation.dataSourceId(),
                                    importFromField.getName(),
                                    foreignRelation.dataSourceId()
                            ));
            }
        }

        return fkField;
    }

    public static ImportFromRelation describeImportFrom(IDSLookup idsRegistry, DataSource dataSource, DSField importFromField) {
        if (importFromField == null) {
            throw new IllegalArgumentException("Can't determine ImportFromRelation for '%s' datasource: importFrom field is NULL"
                    .formatted(dataSource.getId()));
        }

        if (!importFromField.isIncludeField()) {
            throw new IllegalStateException();
        }


        final String[] parsedIncludeFrom = importFromField.getIncludeFrom().trim().split("\\.");

        if (parsedIncludeFrom.length % 2 != 0) {
            throw new IllegalStateException(
                    ("Can't determine ForeignRelation for relation  '%s.%s': '%s', " +
                            "'foreignKey' field MUST be prefixed with a DataSource ID."
                    ).formatted(dataSource.getId(), importFromField.getName(), importFromField.getIncludeFrom())
            );
        }

        final int depth = parsedIncludeFrom.length / 2;
        final List<ForeignKeyRelation> foreignKeyRelations = new ArrayList<>(depth);

        DataSource currentDataSource = dataSource;

        DSField displayField = null;
        DSField currentSourceField = importFromField;
        for(int i=0; i<parsedIncludeFrom.length; i = i +2) {
            final DataSource ds = currentDataSource;
            final String foreignDsId = parsedIncludeFrom[i].trim();
            final String foreignDsFieldName = parsedIncludeFrom[i + 1].trim();

            final ForeignRelation foreignRelation = ForeignRelation.describeForeignRelation(
                    ds,
                    currentSourceField,
                    idsRegistry,
                    "%s.%s".formatted(foreignDsId, foreignDsFieldName)
            );

            displayField = foreignRelation.field;

            // -- source field that linked to the foreign datasource
            final DSField fkField = determineForeignKeyField(ds, currentSourceField, foreignRelation);

            // -- foreign key
            final String[] parsedForeignKey = fkField.getForeignKey().split("\\.");
            if (parsedForeignKey.length != 2) {
                throw new RuntimeException(("Can't determine ImportFromRelation for '%s.%s': " +
                        "invalid foreignKey value '%s'; 'foreignKey' field MUST be prefixed with a DataSource ID.")
                        .formatted(
                                ds.getId(),
                                fkField.getName(),
                                fkField.getForeignKey()
                        )
                );
            }

            if (!foreignRelation.dataSourceId().equals(parsedForeignKey[0])) {
                throw new IllegalStateException("");
            }

            if (foreignRelation.dataSource() == null) {
                throw new RuntimeException(
                        ("Can't determine ImportFromRelation for '%s.%s': " +
                                "nothing known about a foreign data source '%s'."
                        ).formatted(
                                ds.getId(),
                                fkField.getName(),
                                foreignRelation.dataSourceId()
                        )
                );
            }

            final DSField foreignKey = foreignRelation.dataSource().getField(parsedForeignKey[1]);
            if (foreignKey == null) {
                throw new IllegalStateException(("Can't determine ImportFromRelation for '%s.%s': " +
                        "foreign datasource '%s' nothing known about field name '%s'.")
                        .formatted(
                                ds.getId(),
//                                    effectiveSourceField.getName(),
                                fkField.getName(),
                                foreignRelation.dataSourceId(),
                                parsedForeignKey[1]
                        )
                );
            }

            final String  relatedTableAlias = fkField.getRelatedTableAlias() == null || fkField.getRelatedTableAlias().isBlank() ?
                    fkField.getName() + "_" + foreignRelation.dataSource.getTableName() : fkField.getRelatedTableAlias();

            final ForeignKeyRelation frl = new ForeignKeyRelation(
                    ds,
                    fkField,
                    false,
                    new ForeignRelation(foreignRelation.dataSource.getId(), foreignRelation.dataSource, foreignKey.getName(), foreignKey, null, relatedTableAlias)
            );

            foreignKeyRelations.add(frl);
            currentDataSource = foreignRelation.dataSource();
            currentSourceField = foreignRelation.field();
        }

        final ForeignKeyRelation frl1 = foreignKeyRelations.get(0);
        return new ImportFromRelation(
                frl1.dataSource(),
                importFromField,
                foreignKeyRelations,
                displayField
        );
    }

    static public ForeignKeyRelation describeForeignKey(IDSLookup dsRegistry, DataSource dataSource, DSField foreignKeyField) {
        if (foreignKeyField.getForeignKey() == null || foreignKeyField.getForeignKey().isBlank()) {
            throw new IllegalStateException("Can't determine ForeignKeyRelation for  field '%s.%s': field does not declare any foreignKey."
                    .formatted(
                            dataSource.getId(),
                            foreignKeyField.getName()
                    ));
        }

        final ForeignRelation foreignKeyRelation = ForeignRelation.describeForeignRelation( dataSource, foreignKeyField, dsRegistry, foreignKeyField.getForeignKey());

        // by srg: this part must be re-written after ManyToMany will be implemented
        final boolean isInverse;
        final DSField effectiveSourceField;
        if (DSField.FieldType.ENTITY.equals(foreignKeyField.getType())
                && foreignKeyField.isMultiple()) {
            isInverse = true;

            // -- find PKs
            final List<DSField> pks = dataSource.getFields().stream()
                    .filter(DSField::isPrimaryKey)
                    .collect(Collectors.toList());

            switch (pks.size()) {
                case 0 -> throw new RuntimeException();
                case 1 -> effectiveSourceField = pks.get(0);
                default -> throw new RuntimeException();
            }
        } else {
            isInverse = false;
            effectiveSourceField = foreignKeyField;
        }


        return new ForeignKeyRelation(dataSource, effectiveSourceField, isInverse, foreignKeyRelation);
    }
}
