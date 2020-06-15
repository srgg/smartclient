package org.srg.smartclient;

import com.fasterxml.jackson.core.type.TypeReference;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;

import java.util.List;

public abstract class AbstractJDBCHandlerTest<H extends JDBCHandler> {
    protected enum ExtraField {
        IncludeFrom("""
            [
                {
                    name: 'location'
                    ,dbName: 'location_id'
                    ,type: "INTEGER"
                    ,foreignKey:"LocationDS.id"
                    ,displayField:"locationCity"
                },
                {
                    name:"locationCity"
                    ,type:"TEXT"
                    ,includeFrom:"LocationDS.city"
                }
            ]"""),

        Email("""
            [
                {
                    name: "email",
                    type: "text"
                }
            ]
            """),

        FiredAt("""
            [
                {
                    name: "firedAt",
                    type: "datetime"
                }
            ]"""),

        Calculated("""
            [
                {
                    name: "calculated",
                    type: "text",
                    customSelectExpression: "CONCAT(users.id, '_', users.name)"                    
                }
            ]
            """);

        final String fieldDefinition;


        ExtraField(String fieldDefinition) {
            this.fieldDefinition = fieldDefinition;
        }
    }

    private static String LocationDSDescription = """
        {
          id: "LocationDS",
          tableName: "locations",
          fields: [
            {
              name: "id",
              type: "integer",
              required: true,
              primaryKey: true
            },
            {
              name: "country",
              type: "text"
            },
            {
              name: "city",
              type: "text"
            }
          ]
        }""";

    private static String UserDSDescription = """
        {
           id: 'usersDS',
           serverType: 'sql',
           tableName: 'users',
           fields: [
               {
                   name: 'id',
                   type: 'integer',
                   primaryKey: true
               },
               {
                   name: 'name',
                   type: 'text',
                   required: true
               }
           ]
        }""";

    protected JdbcDataSource jdbcDataSource = new JdbcDataSource();
    protected DataSource locationDS;
    protected DataSource userDS;

    protected H handler;

    @BeforeAll
    public static void setupDB() {
        JsonTestSupport.defaultMapper = JsonSerde.createMapper();
    }

    @AfterAll
    public static void shutdownDB() {
    }

    @BeforeEach
    public void setupDataSources() throws Exception {
        jdbcDataSource.setURL("jdbc:h2:mem:test:˜/test;INIT=RUNSCRIPT FROM 'classpath:test-data.sql'");
        jdbcDataSource.setUser("sa");
        jdbcDataSource.setPassword("sa");

        locationDS = JsonTestSupport.fromJSON(DataSource.class, LocationDSDescription);
        userDS = JsonTestSupport.fromJSON(DataSource.class, UserDSDescription);

        handler = initHandler(jdbcDataSource);
    }

    abstract protected H initHandler(JdbcDataSource dataSource) throws Exception;

    protected void withExtraFields(ExtraField... extraFields) {
        for (ExtraField ef: extraFields) {
            final List<DSField> efs = JsonTestSupport.fromJSON(new TypeReference<>() {}, ef.fieldDefinition);
            handler.getDataSource()
                    .getFields()
                    .addAll(efs);
        }
    }

//    protected void addExtraFields(String...jsonFieldDescription) throws IOException {
//        final String asJson = JsonTestSupport.asStrictJSON(jsonFieldDescription);
//        addExtraFields(asJson);
//    }
}
