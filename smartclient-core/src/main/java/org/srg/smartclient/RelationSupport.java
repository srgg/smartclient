package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;

import java.util.List;
import java.util.stream.Collectors;

public class RelationSupport {
    public static record ForeignKeyRelation(
            DataSource dataSource,
            DSField sourceField,
            boolean isInverse,

            ForeignRelation foreign
    ){
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


    public static record ImportFromRelation(
            DataSource dataSource,
            DSField sourceField,

            DataSource foreignDataSource,
            DSField foreignKey,
            DSField foreignDisplay
    ){
        @Override
        public String toString() {
            return "ImportFromRelation{" +
                    "dataSource=" + dataSource.getId() +
                    ", sourceField=" + sourceField.getName() +
                    ", foreignDataSource=" + foreignDataSource.getId() +
                    ", foreignKey=" + foreignKey.getName() +
                    ", foreignDisplay=" + foreignDisplay.getName() +
                    '}';
        }

        /**
         * Converts {@code ImportFromRelation} to {@code ForeignKeyRelation} as is.
         */
        public ForeignKeyRelation toForeignKeyRelation() {
            return new ForeignKeyRelation(
                    this.dataSource,
                    this.sourceField,
                    false, // no chance to determine this at this point/level
                    new ForeignRelation(
                            this.foreignDataSource.getId(),
                            this.foreignDataSource,
                            this.foreignKey.getName(),
                            this.foreignKey
                    )
            );
        }

        /**
         * Converts {@code ImportFromRelation} to {@code ForeignKeyRelation} but replacing
         * {@code #foreignKey} by the {@foreignDisplay}.
         */
        public ForeignKeyRelation toForeignDisplayKeyRelation() {
            assert foreignDisplay != null;

            return new ForeignKeyRelation(
                    this.dataSource,
                    this.sourceField,
                    false, // no chance to determine this at this point/level
                    new ForeignRelation(
                            this.foreignDataSource.getId(),
                            this.foreignDataSource,
                            this.foreignDisplay.getName(),
                            this.foreignDisplay
                    )
            );
        }
    }

    protected static record ForeignRelation(
        String dataSourceId,
        DataSource dataSource,

        String fieldName,
        DSField field


    ){
        public String formatAsSQL() {
            return "%s.%s".formatted(
                    dataSource().getTableName(),
                    field().getDbName()
            );

        }

        @Override
        public String toString() {
            return "ForeignRelation{" +
                    "dataSourceId='" + dataSourceId + '\'' +
                    ", fieldName='" + fieldName + '\'' +
                    '}';
        }
    }

    public static ImportFromRelation describeImportFrom(IDSRegistry idsRegistry, DataSource dataSource, DSField importFromField) {
        if (!importFromField.isIncludeField()) {
            throw new IllegalStateException();
        }

        // -- foreign DS
        final ForeignRelation foreignRelation = describeForeignRelation(idsRegistry, importFromField.getIncludeFrom());

        // -- source field
        final DSField sourceField = dataSource.getFields().stream()
                .filter( f -> f.getDisplayField() != null
                        && f.getDisplayField().equals(importFromField.getName())
                )
                .reduce((d1, d2) -> {
                    throw new IllegalStateException("DataSource '%s' can't determine a sourceField for importFromField  '%s'."
                            .formatted(
                                    foreignRelation.dataSourceId,
                                    importFromField.getName()
                            )
                    );
                })
                /*
                 * It is possible that 'includeFrom' will be used w/o a related 'displayField'
                 */
//                .orElseThrow( () -> {
//                    throw new IllegalStateException(("Can't determine a sourceField for importFromField '%s.%s': " +
//                            "datasource '%s' has no field.displayField == '%s'.")
//                            .formatted(
//                                    foreignRelation.dataSourceId,
//                                    importFromField.getName(),
//                                    dataSource.getId(),
//                                    importFromField
//
//                            )
//                    );
//                });
                .orElse(importFromField);

//        // by srg: this part must be re-written after ManyToMany will be implemented
//        final boolean isReverse;
//        final DSField effectiveSourceField;
//        if (DSField.FieldType.ENTITY.equals(sourceField.getType())
//            && sourceField.isMultiple()) {
//            isReverse = true;
//
//            // -- find PKs
//            final List<DSField> pks = dataSource.getFields().stream()
//                    .filter(dsf -> dsf.isPrimaryKey())
//                    .collect(Collectors.toList());
//
//            switch (pks.size()) {
//                case 0 -> throw new RuntimeException();
//                case 1 -> effectiveSourceField = pks.get(0);
//                default -> throw new RuntimeException();
//            }
//        } else {
//            isReverse = false;
//            effectiveSourceField = sourceField;
//        }

        // -- foreign key
        final String parsedForeignKey[] = sourceField.getForeignKey().split("\\.");
        if (parsedForeignKey.length != 2) {
            throw new RuntimeException(("Can't determine ImportFromRelation for '%s.%s': " +
                    "invalid foreignKey value '%s'; 'foreignKey' field MUST be prefixed with a DataSource ID.")
                    .formatted(
                            dataSource.getId(),
                            sourceField.getName(),
                            sourceField.getForeignKey()
                    )
            );
        }

        if (!foreignRelation.dataSourceId.equals( parsedForeignKey[0])) {
            throw new IllegalStateException("");
        }

        if (foreignRelation.dataSource == null) {
            throw new RuntimeException(
                    ("Can't determine ImportFromRelation for '%s.%s': " +
                        "nothing known about a foreign data source '%s'."
                    ).formatted(
                            dataSource.getId(),
                            sourceField.getName(),
                            foreignRelation.dataSourceId
                    )
            );
        }

        final DSField foreignKey = foreignRelation.dataSource.getFields().stream()
                .filter( f -> f.getName().equals( parsedForeignKey[1] ))
                .reduce( (d1, d2) -> {
                    throw new RuntimeException("Can't determine ImportFromRelation for '%s.%s': " +
                            "foreign key field name '%s.%s' is not unique within  the data source."
                            .formatted(
                                    dataSource.getId(),
//                                    effectiveSourceField.getName(),
                                    sourceField.getName(),
                                    foreignRelation.dataSourceId,
                                    parsedForeignKey[1]
                            )
                    );
                })
                .orElseThrow( () -> {
                    throw new IllegalStateException(("Can't determine ImportFromRelation for '%s.%s': " +
                            "foreign datasource '%s' nothing known about field name '%s'.")
                            .formatted(
                                    dataSource.getId(),
//                                    effectiveSourceField.getName(),
                                    sourceField.getName(),
                                    foreignRelation.dataSourceId,
                                    parsedForeignKey[1]
                            )
                    );
                });


        return new ImportFromRelation(dataSource,
//                effectiveSourceField,
//                isReverse,
                sourceField,
                foreignRelation.dataSource,
                foreignKey,
                foreignRelation.field
        );
    }

    protected static ForeignRelation describeForeignRelation(IDSRegistry dsRegistry, String relation) {
        final String parsedIncludeFrom[] = relation.trim().split("\\.");
        if (parsedIncludeFrom.length != 2) {
            throw new RuntimeException(
                    ("Can't determine ForeignRelation for relation '%s': " +
                        "'foreignKey' field MUST be prefixed with a DataSource ID."
                    ).formatted(relation)
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
                    .orElseThrow( () -> {
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

    static public ForeignKeyRelation describeForeignKey(IDSRegistry dsRegistry, DataSource dataSource, DSField foreignKeyField) {
        if (foreignKeyField.getForeignKey() == null || foreignKeyField.getForeignKey().isBlank()) {
            throw new IllegalStateException();
        }

        final ForeignRelation foreignKeyRelation = describeForeignRelation(dsRegistry, foreignKeyField.getForeignKey());

        // by srg: this part must be re-written after ManyToMany will be implemented
        final boolean isInverse;
        final DSField effectiveSourceField;
        if (DSField.FieldType.ENTITY.equals(foreignKeyField.getType())
                && foreignKeyField.isMultiple()) {
            isInverse = true;

            // -- find PKs
            final List<DSField> pks = dataSource.getFields().stream()
                    .filter(dsf -> dsf.isPrimaryKey())
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
