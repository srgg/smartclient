package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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

    protected static class ForeignRelation {
        private static record FRlData(
            String dataSourceId,
            DataSource dataSource,
            String fieldName,
            DSField field,
            String sqlFieldAlias
        ) {};

        private final List<FRlData> foreignRelations;

        private ForeignRelation(List<FRlData> foreignRelations) {
            if (foreignRelations == null || foreignRelations.isEmpty()) {
                throw new IllegalStateException("Can't instantiate ForeignRelation: foreignRelations can't be null or empty");
            }
            this.foreignRelations = foreignRelations;
        }

        protected ForeignRelation(String dataSourceId, DataSource dataSource, String fieldName, DSField field) {
            this(dataSourceId, dataSource, fieldName, field, null);
        }

        protected ForeignRelation(String dataSourceId, DataSource dataSource, String fieldName, DSField field, String sqlFieldAlias) {
            this(Collections.singletonList(new FRlData(dataSourceId, dataSource, fieldName, field, sqlFieldAlias)));
        }

        private FRlData get1st() {
            final FRlData fRlData = foreignRelations.get(0);
            return fRlData;
        }

        public String dataSourceId() {
            return get1st().dataSourceId;
        }

        public DataSource dataSource() {
            return get1st().dataSource;
        }

        public String fieldName() {
            return get1st().fieldName;
        }

        public DSField field() {
            return get1st().field;
        }

        public String getSqlFieldAlias() {
            return get1st().sqlFieldAlias();
        }

        public ForeignRelation createWithSqlFieldAlias(String sqlFieldAlias) {
            final ForeignRelation effective = new ForeignRelation(
                    this.dataSourceId(),
                    this.dataSource(),
                    this.fieldName(),
                    this.field(),
                    sqlFieldAlias
            );

            return effective;
        }

        public String formatAsSQL() {
            return formatAsSQL(dataSource().getTableName());
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
                    "dataSourceId='" + dataSourceId() + '\'' +
                    ", fieldName='" + fieldName() + '\'' +
                    '}';
        }

        public static ForeignRelation describeForeignRelation(DataSource ds, DSField field, IDSLookup dsRegistry, String relation) {
            final String parsedIncludeFrom[] = relation.trim().split("\\.");

            if (parsedIncludeFrom.length % 2 != 0) {
                throw new IllegalStateException(
                        ("Can't determine ForeignRelation for relation  '%s.%s': '%s', " +
                                "'foreignKey' field MUST be prefixed with a DataSource ID."
                        ).formatted(ds.getId(), field.getName(), relation)
                );
            }

//        if (parsedIncludeFrom.length != 2) {
//            throw new RuntimeException(
//                    ("Can't determine ForeignRelation for relation '%s': " +
//                        "'foreignKey' field MUST be prefixed with a DataSource ID."
//                    ).formatted(relation)
//            );
//        }

            final int depth = parsedIncludeFrom.length / 2;
            final List<FRlData> foreignRelations = new ArrayList<>(depth);


            for(int i=0; i<depth; ++i) {
                final String foreignDsId = parsedIncludeFrom[i].trim();
                final String foreignDsFieldName = parsedIncludeFrom[i+1].trim();

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
                                        "foreign datasource '%s' nothing known about the field with name '%s'.")
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

                final FRlData frlData = new FRlData(foreignDsId, foreignDS, foreignDsFieldName, foreignField, null);
                foreignRelations.add(frlData);
            }

            return new ForeignRelation(foreignRelations);
        }
    }

    private static DSField  determineSourceField(DataSource dataSource, DSField importFromField, ForeignRelation foreignRelation) {
        DSField sourceField = null;

        if ( importFromField.getIncludeVia() != null && !importFromField.getIncludeVia().isBlank() ) {
            // Lookup source field by "IncludeVia"
            sourceField = dataSource.getFields().stream()
                    .filter(f -> f.getName().equals(importFromField.getIncludeVia()))
                    .reduce((d1, d2) -> {
                        throw new IllegalStateException(("DataSource '%s' can't determine a sourceField for importFromField  '%s' by IncludeVia '%s': " +
                                "there are multiple fields with the name '%s'.")
                                .formatted(
                                        foreignRelation.dataSourceId(),
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
                                        foreignRelation.dataSourceId(),
                                        importFromField.getName(),
                                        importFromField.getIncludeVia(),
                                        importFromField.getIncludeVia()
                                )
                        );
                    });
        } else {
            // Lookup source field by "DisplayField"
            sourceField = dataSource.getFields().stream()
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

        if (sourceField == null) {
            // Search for any field with foreignKey pointed to the foreign data source

            final List<DSField> foreignFields =  dataSource.getFields().stream()
                    .filter( f -> f.getForeignKey() != null
                            && f.getForeignKey().startsWith(foreignRelation.dataSourceId())
                    ).collect(Collectors.toList());

            final int size = foreignFields.size();

            switch (size) {
                case 1: sourceField = foreignFields.get(0);

                case 0: new IllegalStateException(("DataSource '%s' can't determine a sourceField for importFromField  '%s' by foreign relation: " +
                        "there is no fields with foreignKey pointed out to the foreignDataSource '%s'.")
                        .formatted(
                                foreignRelation.dataSourceId(),
                                importFromField.getName(),
                                foreignRelation.dataSourceId()
                        ));

                default:
                    new IllegalStateException(("DataSource '%s' can't determine a sourceField for importFromField  '%s' by foreign relation: " +
                            "there are multiple fields with foreignKey pointed out to the foreignDataSource '%s', consider to use includeVia to point it oit to the exact field.")
                            .formatted(
                                    foreignRelation.dataSourceId(),
                                    importFromField.getName(),
                                    foreignRelation.dataSourceId()
                            ));
            }
        }

        return sourceField;
    }

    public static ImportFromRelation describeImportFrom(IDSLookup idsRegistry, DataSource dataSource, DSField importFromField) {
        if (!importFromField.isIncludeField()) {
            throw new IllegalStateException();
        }

        // -- foreign DS
        final ForeignRelation foreignRelation = ForeignRelation.describeForeignRelation(dataSource, importFromField, idsRegistry, importFromField.getIncludeFrom());

        // -- source field that linked to the foreign datasource
        final DSField sourceField = determineSourceField(dataSource, importFromField, foreignRelation);



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

        if (!foreignRelation.dataSourceId().equals( parsedForeignKey[0])) {
            throw new IllegalStateException("");
        }

        if (foreignRelation.dataSource() == null) {
            throw new RuntimeException(
                    ("Can't determine ImportFromRelation for '%s.%s': " +
                        "nothing known about a foreign data source '%s'."
                    ).formatted(
                            dataSource.getId(),
                            sourceField.getName(),
                            foreignRelation.dataSourceId()
                    )
            );
        }

        final DSField foreignKey = foreignRelation.dataSource().getFields().stream()
                .filter( f -> f.getName().equals( parsedForeignKey[1] ))
                .reduce( (d1, d2) -> {
                    throw new RuntimeException("Can't determine ImportFromRelation for '%s.%s': " +
                            "foreign key field name '%s.%s' is not unique within  the data source."
                            .formatted(
                                    dataSource.getId(),
//                                    effectiveSourceField.getName(),
                                    sourceField.getName(),
                                    foreignRelation.dataSourceId(),
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
                                    foreignRelation.dataSourceId(),
                                    parsedForeignKey[1]
                            )
                    );
                });


        return new ImportFromRelation(dataSource,
//                effectiveSourceField,
//                isReverse,
                sourceField,
                foreignRelation.dataSource(),
                foreignKey,
                foreignRelation.field()
        );
    }

//    protected static ForeignRelation describeForeignRelation(DataSource ds, DSField field, IDSLookup dsRegistry, String relation) {
//        final String parsedIncludeFrom[] = relation.trim().split("\\.");
//
//        if (parsedIncludeFrom.length % 2 != 0) {
//            throw new IllegalStateException(
//                    ("Can't determine ForeignRelation for relation  '%s.%s': '%s', " +
//                            "'foreignKey' field MUST be prefixed with a DataSource ID."
//                    ).formatted(ds.getId(), field.getName(), relation)
//            );
//        }
//
////        if (parsedIncludeFrom.length != 2) {
////            throw new RuntimeException(
////                    ("Can't determine ForeignRelation for relation '%s': " +
////                        "'foreignKey' field MUST be prefixed with a DataSource ID."
////                    ).formatted(relation)
////            );
////        }
//
//        final int depth = parsedIncludeFrom.length / 2;
//        final List<ForeignRelation> foreignRelations = new ArrayList<>(depth);
//
//
//        for(int i=0; i<depth; ++i) {
//            final String foreignDsId = parsedIncludeFrom[i].trim();
//            final String foreignDsFieldName = parsedIncludeFrom[i+1].trim();
//
//            final DataSource foreignDS = dsRegistry.getDataSourceById(foreignDsId);
//
//            final DSField foreignField;
//            if (foreignDS != null) {
//                // -- foreign field
//                foreignField = foreignDS.getFields().stream()
//                        .filter(f -> f.getName().equals(foreignDsFieldName))
//                        .reduce((d1, d2) -> {
//                            throw new IllegalStateException(("Can't determine ForeignRelation for relation '%s': " +
//                                    "foreign datasource '%s' does not have unique field name '%s'.")
//                                    .formatted(
//                                            relation,
//                                            foreignDsId,
//                                            foreignDsFieldName
//                                    )
//                            );
//                        })
//                        .orElseThrow(() -> {
//                            throw new IllegalStateException(("Can't determine ForeignRelation for relation '%s': " +
//                                    "foreign datasource '%s' nothing known about the field with name '%s'.")
//                                    .formatted(
//                                            relation,
//                                            foreignDsId,
//                                            foreignDsFieldName
//                                    )
//                            );
//                        });
//
//            } else {
//                foreignField = null;
//            }
//
//            final ForeignRelation frl = new ForeignRelation(foreignDsId, foreignDS, foreignDsFieldName, foreignField);
//
//            foreignRelations.add(frl);
//        }
//
//        return foreignRelations;
//    }

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
