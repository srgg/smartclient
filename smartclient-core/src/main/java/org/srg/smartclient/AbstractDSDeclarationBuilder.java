package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.DSField;

import java.util.Collection;

/**
 *
 * @author srg
 */
abstract class AbstractDSDeclarationBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDSDeclarationBuilder.class);

    private class BuilderContext {
        private String dsName;
        private int qntGeneratedFields;
        private StringBuilder builder;

        public BuilderContext() {
            clear();
            builder = null;
        }

        final public void clear() {
            dsName = null;
            qntGeneratedFields = 0;
            builder = new StringBuilder();
        }

        public void write(String fmt, Object... args) {
            if (args == null || args.length == 0) {
                builder.append(fmt);
            } else {
                builder.append(String.format(fmt, args));
            }
        }

        public void write_if(Boolean flag, String fmt, Object... args) {
            if (Boolean.TRUE.equals(flag)) {
                write(fmt, args);
            }
        }

        public void write_if_notBlank(String str, String fmt, Object... args) {
            if (str != null && !str.isEmpty() ) {
                write(fmt, args);
            }
        }
    }
    private BuilderContext context = new BuilderContext();

    public String build(String name, String dispatcherUrl, Collection<DSField> fields) throws ClassNotFoundException {
        context.clear();

        context.dsName = name;

        context.write(
                "isc.RestDataSource.create({\n"
                + "  ID:\"%s\",\n"
                + "  dataFormat:\"json\",\n"
                + "  dataURL:\"%s\",\n"
                + "  dataProtocol: \"postMessage\",\n"
                + "  dataTransport: \"xmlHttpRequest\",\n"
                + "  disableQueuing: false,\n"
                + "  useStrictJSON: true,\n"
                + "  operationBindings:[\n"
                + "    {operationType:\"fetch\", dataProtocol:\"postMessage\"},\n"
                + "    {operationType:\"add\", dataProtocol:\"postMessage\"},\n"
                + "    {operationType:\"remove\", dataProtocol:\"postMessage\"},\n"
                + "    {operationType:\"update\", dataProtocol:\"postMessage\"}\n"
                + "  ],\n"
                + "  fields:[\n",
                name,
                dispatcherUrl);

        buildFields(fields);

        context.write("]});\n");
        return context.builder.toString();
    }

    protected static boolean isNotBlank( String value) {
        return value != null && !value.isEmpty();
    }

    protected void buildField(DSField f) throws ClassNotFoundException {

//        final Class javaClass = Utils.classForName(f.getJavaClass());
        final boolean isSubEntity = /*(f.getFields() != null && !f.getFields().isEmpty() ) ||*/ isNotBlank(f.getForeignKey());

        DSField.FieldType ft;
        if (isSubEntity) {
            ft = f.getType();
        } else {
            try {
                ft = f.getType();

                if( ft == null ){
//                    ft = javatype2smartclient(javaClass);
                    throw new IllegalStateException();
                }
            } catch (Exception ex) {
                logger.warn(
                        String.format("FIELD '%s' WON'T EXPOSED through the Datasource '%s' due type mapping error.",
                        f.getName(),
                        context.dsName),
                        ex);
                return;
            }
        }

        if (context.qntGeneratedFields > 0) {
            context.write(",\n");
        } else {
            context.write("\n");
        }
        ++context.qntGeneratedFields;

        context.write(
                "\t\t{\n"
                + "\t\t\tname:\"%s\"\n",
                f.getName());

        context.write_if_notBlank(f.getTitle(),
                "\t\t\t, title:'%s'\n", f.getTitle());

//        context.write_if(f.canEdit() != null && !f.canEdit(),
//                "\t\t\t, canEdit:false\n" );
//
//        context.write_if(f.getCanFilter() !=null && !f.getCanFilter(),
//                "\t\t\t, canFilter:false\n");
//
//        if(f.getValueMap() != null ){
//            final Field.ValueMap vm = f.getValueMap();
//            context.write("\t\t\t, valueMap:{\n");
//            buildValueMapContent(context.builder, vm, "\t\t\t\t");
//            context.write("\t\t\t }\n");
//        }

        if (!isSubEntity) {

            context.write("\t\t\t,type:\"%s\"\n",
                    ft.name());

        } else {
            if( isNotBlank(f.getForeignKey())){
                context.write(
                        "\t\t\t,foreignKey:\"%s\"\n",
                        f.getForeignKey());
            } else {
                throw new IllegalStateException();
//                final Set<DataSourceField> pks = DataSourceField.findPK(f.getFields());
//                final boolean isComposite = pks.size() > 1;
//
//                if (isComposite) {
//                    throw new RuntimeException(
//                            String.format("Composite FK FIELD doesn't supported yet!!! (ds '%s',field: %s ).",
//                            context.dsName,
//                            f.getName()));
//                }
//
//                context.write_if(isComposite, "{");
//
//                final String dsName = getDataSourceNameByEntityType(javaClass);
//                if( dsName == null ){
//                    logger.warn(
//                            String.format("FK FIELD '%s' in the Datasource '%s' WON'T MARKED as foreignKey.",
//                            f.getName(),
//                            context.dsName));
//                }
//                for (DataSourceField dsf : pks) {
//                    context.write_if(dsName != null,
//                            "\t\t\t,foreignKey:\"%s.%s\"\n",
//                            dsName,
//                            dsf.getName());
//                }
//
//                context.write_if(isComposite, "}");
            }

            context.write_if_notBlank(f.getDisplayField(),
                    "\t\t\t,displayField:\"%s\"\n",
                    f.getDisplayField()
            );

            context.write_if_notBlank(f.getForeignDisplayField(),
                    "\t\t\t,foreignDisplayField:\"%s\"\n",
                    f.getForeignDisplayField()
            );
        }

            context.write_if_notBlank(f.getIncludeFrom(),
                    "\t\t\t,includeFrom:\"%s\"\n",
                    f.getIncludeFrom()
            );

            context.write_if_notBlank(f.getIncludeVia(),
                    "\t\t\t,includeVia:\"%s\"\n",
                    f.getIncludeVia()
            );

//        context.write_if(f.getMultiple(), "\t\t\t,multiple:true\n");

        context.write_if(f.isPrimaryKey(),
                "\t\t\t,hidden:true\n"
                + "\t\t\t,primaryKey:true\n"
                + "\t\t\t,canEdit:false\n");

        context.write_if(!f.isPrimaryKey(),
                "\t\t\t,canEdit:%b\n",
                f.isCanEdit()
        );

        context.write_if(!f.isPrimaryKey(),
                "\t\t\t,hidden:%b\n",
                f.isHidden()
        );

        context.write_if(f.getRootValue() != null, ", rootValue: %s\n", f.getRootValue());

        context.write_if(f.isCustomSQL(),
                "\t\t\t,customSQL:%b\n",
                    f.isCustomSQL()
        );

        context.write_if_notBlank(f.getSql(),
                "\t\t\t,sql:\"%s\"\n",
                f.getSql()
        );

        context.write("\t\t}");
    }

    protected void buildFields(Iterable<DSField> fields) throws ClassNotFoundException {
        for (DSField f : fields) {
            buildField(f);
        }
    }

//    protected static DSField.FieldType javatype2smartclient(Class clazz) {
//        if (clazz.equals(Integer.class)
//                || clazz.equals(Short.class)
//                || clazz.equals(int.class)
//                || clazz.equals(short.class)) {
//            return DSField.FieldType.INTEGER;
//        }
//
//        if(clazz.equals(Long.class)
//                || clazz.equals(long.class) ){
////            return DSField.FieldType.LONG;
//            return DSField.FieldType.INTEGER;
//        }
//
//        if (clazz.equals(String.class)) {
//            return DSField.FieldType.TEXT;
//        }
//
//        if (clazz.equals(java.sql.Date.class)) {
//            return DSField.FieldType.DATE;
//        }
//
//        if( clazz.equals(Boolean.class)
//                || clazz.equals(boolean.class) ){
//            return DSField.FieldType.BOOLEAN;
//        }
//
//        throw new RuntimeException(String.format("SmartClient -- Unmapped field type %s.", clazz.getName()));
//    }

//    protected static StringBuilder buildValueMapContent(StringBuilder builder, Field.ValueMap vm, String delimiter ){
//        if(delimiter == null){
//            delimiter = "\t";
//        }
//
//        boolean isFirst = true;
//        for(Field.ValueMap.Value v: vm.getValue()){
//            builder.append(delimiter);
//            if(!isFirst){
//                builder.append(',');
//            } else {
//                isFirst = false;
//            }
//
//            builder.append(String.format("%s: '%s'\n",
//                    v.getID(),
//                    v.getContent().replaceAll("'", "\'")));
//        }
//        return builder;
//    }

}
