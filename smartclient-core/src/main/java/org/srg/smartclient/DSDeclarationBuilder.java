package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author srg
 */
abstract class DSDeclarationBuilder {

    private DSDeclarationBuilder() {}

    private static final Logger logger = LoggerFactory.getLogger(DSDeclarationBuilder.class);

    private static class BuilderContext {
        private String dsName;
        private int qntGeneratedFields;
        private StringBuilder builder;

        public BuilderContext() {
            clear();
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

    public static String build(String dispatcherUrl, DSHandler dsHandler) throws ClassNotFoundException {
        return build(dispatcherUrl, dsHandler.dataSource(), dsHandler.allowAdvancedCriteria());
    }

    public static String build(String dispatcherUrl, DataSource dataSource, boolean allowAdvancedCriteria) throws ClassNotFoundException {

        final BuilderContext ctx = new BuilderContext();

        ctx.dsName = dataSource.getId();
        final Collection<DSField> allFields = dataSource.getFields();


        ctx.write("""                        
                isc.RestDataSource.create({
                  ID: "%s",
                  dataFormat:"json",
                  dataURL:"%s",
                  dataProtocol:"postMessage",
                  dataTransport:"xmlHttpRequest",
                  disableQueuing:false,
                  useStrictJSON:true,
                  allowAdvancedCriteria: %b,
                  operationBindings:[
                    {operationType:"fetch", dataProtocol:"postMessage"},
                    {operationType:"add", dataProtocol:"postMessage"},
                    {operationType:"remove", dataProtocol:"postMessage"},
                    {operationType:"update", dataProtocol:"postMessage"}
                  ],
                  fields:[""",
                ctx.dsName,
                dispatcherUrl,
                allowAdvancedCriteria
        );

        for (DSField f : dataSource.getFields()) {
            buildField(ctx, f);
        }

        ctx.write("]});\n");
        return ctx.builder.toString();
    }

    protected static boolean isNotBlank( String value) {
        return value != null && !value.isEmpty();
    }

    protected static void buildField(BuilderContext ctx, DSField f) throws ClassNotFoundException {

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
                        ctx.dsName),
                        ex);
                return;
            }
        }

        if (ctx.qntGeneratedFields > 0) {
            ctx.write(",\n");
        } else {
            ctx.write("\n");
        }
        ++ctx.qntGeneratedFields;

        ctx.write(
                "\t\t{\n"
                + "\t\t\tname:\"%s\"\n",
                f.getName());

        ctx.write_if_notBlank(f.getTitle(),
                "\t\t\t, title:'%s'\n", f.getTitle());

//        context.write_if(f.canEdit() != null && !f.canEdit(),
//                "\t\t\t, canEdit:false\n" );
//
//        context.write_if(f.getCanFilter() !=null && !f.getCanFilter(),
//                "\t\t\t, canFilter:false\n");
//

        if (!isSubEntity) {

            ctx.write("\t\t\t,type:\"%s\"\n",
                    ft.name());

        } else {
            if( isNotBlank(f.getForeignKey())){
                ctx.write(
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

            ctx.write_if_notBlank(f.getDisplayField(),
                    "\t\t\t,displayField:\"%s\"\n",
                    f.getDisplayField()
            );

            ctx.write_if_notBlank(f.getForeignDisplayField(),
                    "\t\t\t,foreignDisplayField:\"%s\"\n",
                    f.getForeignDisplayField()
            );
        }

            ctx.write_if_notBlank(f.getIncludeFrom(),
                    "\t\t\t,includeFrom:\"%s\"\n",
                    f.getIncludeFrom()
            );

            ctx.write_if_notBlank(f.getIncludeVia(),
                    "\t\t\t,includeVia:\"%s\"\n",
                    f.getIncludeVia()
            );

//        context.write_if(f.getMultiple(), "\t\t\t,multiple:true\n");

        ctx.write_if(f.isPrimaryKey(),
                "\t\t\t,hidden:true\n"
                + "\t\t\t,primaryKey:true\n"
                + "\t\t\t,canEdit:false\n");

        ctx.write_if(!f.isPrimaryKey(),
                "\t\t\t,canEdit:%b\n",
                f.isCanEdit()
        );

        ctx.write_if(!f.isPrimaryKey(),
                "\t\t\t,hidden:%b\n",
                f.isHidden()
        );

        ctx.write_if(f.getRootValue() != null, ", rootValue: %s\n", f.getRootValue());

        ctx.write_if(f.isCustomSQL(),
                "\t\t\t,customSQL:%b\n",
                    f.isCustomSQL()
        );

//        context.write_if_notBlank(f.getCustomSelectExpression(),
//                "\t\t\t,customSelectExpression:\"%s\"\n",
//                f.getCustomSelectExpression()
//        );

//        if(f.getValueMap() != null ){
//            final Field.ValueMap vm = f.getValueMap();
//            context.write("\t\t\t, valueMap:{\n");
//            buildValueMapContent(context.builder, vm, "\t\t\t\t");
//            context.write("\t\t\t }\n");
//        }

        if (f.getValueMap() != null) {
            final String s = ((Map<Object, Object>)f.getValueMap()).entrySet()
                    .stream()
//                    .map( entry -> "{ %s:'%s'}"
//                            .formatted(entry.getKey(),
//                                    entry.getValue()
//                                            .toString()
//                                            .replaceAll("'", "\'")
//                            )
//                    )
                    .map( entry -> "'%s'"
                            .formatted(
                                    entry.getValue()
                                            .toString()
                                            .replaceAll("'", "\'")
                            )
                    )
                    .collect(Collectors.joining("\n\t\t\t   ,"));


            ctx.write("\t\t\t,valueMap:[\n\t\t\t   %s\n\t\t\t]\n", s);
        }

        ctx.write("\t\t}");
    }
}
