package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;

public class RelationSupport {
    public static record ForeignKeyRelation(
            DataSource dataSource,
            DSField sourceField,

            ForeignRelation foreign
    ){
        @Override
        public String toString() {
            return "ForeignKeyRelation{" +
                    "dataSource=" + dataSource.getId() +
                    ", sourceField=" + sourceField.getName() +
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
    }

    protected static record ForeignRelation(
        String dataSourceId,
        DataSource dataSource,

        String fieldName,
        DSField field


    ){
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
                .orElseThrow( () -> {
                    throw new IllegalStateException(("Can't determine a sourceField for importFromField '%s.%s': " +
                            "datasource '%s' has no field.displayField == '%s'.")
                            .formatted(
                                    foreignRelation.dataSourceId,
                                    importFromField.getName(),
                                    dataSource.getId(),
                                    importFromField

                            )
                    );
                });

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
                                    sourceField.getName(),
                                    foreignRelation.dataSourceId,
                                    parsedForeignKey[1]
                            )
                    );
                });


        return new ImportFromRelation(dataSource,
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
                                "foreign datasource '%s' nothing known about field name '%s'.")
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

        return new ForeignKeyRelation(dataSource, foreignKeyField, foreignKeyRelation);
    }
}
