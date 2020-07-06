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
//        final String parsedIncludeFrom[] = importFromField.getIncludeFrom().split("\\.");
//        if (parsedIncludeFrom.length != 2) {
//            throw new IllegalStateException("DataSource '%s' field '%s', all 'includeFrom' fields must be prefixed with DataSource ID, but actual value is '%s'."
//                    .formatted(
//                            dataSource.getId(),
//                            importFromField.getName(),
//                            importFromField.getIncludeFrom()
//                    )
//            );
//        }
//        final DataSource foreignDS = idsRegistry.getDataSourceById(parsedIncludeFrom[0]);
//
//        // -- foreign Display field
//        final DSField foreignDisplayField = foreignDS.getFields().stream()
//                .filter( f -> f.getName().equals( parsedIncludeFrom[1] ))
//                .reduce( (d1, d2) -> {
//                    throw new IllegalStateException("DataSource '%s' has  not unique field name '%s'."
//                            .formatted(
//                                    foreignDS.getId(),
//                                    parsedIncludeFrom[1]
//                            )
//                    );
//                })
//                .get();


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
                .get();

        // -- foreign key
        final String parsedForeignKey[] = sourceField.getForeignKey().split("\\.");
        if (parsedForeignKey.length != 2) {
            throw new IllegalStateException("DataSource '%s' field '%s', 'foreignKey' field should be prefixed with DataSource ID, but the actual value is '%s'."
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

        final DSField foreignKey = foreignRelation.dataSource.getFields().stream()
                .filter( f -> f.getDbName().equals( parsedForeignKey[1] ))
                .reduce( (d1, d2) -> {
                    throw new IllegalStateException("DataSource '%s' has  not unique field name '%s'."
                            .formatted(
                                    foreignRelation.dataSourceId,
                                    parsedForeignKey[1]
                            )
                    );
                })
                .get();

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
            throw new IllegalStateException();
//            throw new IllegalStateException("DataSource '%s' field '%s', all 'includeFrom' fields must be prefixed with DataSource ID, but actual value is '%s'."
//                    .formatted(
//                            dataSource.getId(),
//                            importFromField.getName(),
//                            importFromField.getIncludeFrom()
//                    )
//            );
        }

        final String foreignDsId = parsedIncludeFrom[0].trim();
        final String foreignDsFieldName = parsedIncludeFrom[1].trim();

        final DataSource foreignDS = dsRegistry.getDataSourceById(foreignDsId);

        final DSField foreignField;
        if (foreignDS != null) {
            // -- foreign field
            foreignField = foreignDS.getFields().stream()
                    .filter( f -> f.getName().equals( foreignDsFieldName ))
                    .reduce( (d1, d2) -> {
                        throw new IllegalStateException("Foreign DataSource '%s' does not have unique field name '%s'."
                                .formatted(
                                        foreignDsId,
                                        foreignDsFieldName
                                )
                        );
                    })
                    .get();

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
