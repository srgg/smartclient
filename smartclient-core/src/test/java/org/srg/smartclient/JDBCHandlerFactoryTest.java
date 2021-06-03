package org.srg.smartclient;

import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.annotations.SmartClientField;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;
import org.srg.smartclient.utils.Serde;

import java.lang.reflect.Field;

class JDBCHandlerFactoryTest {

    private JDBCHandlerFactory jdbcHandlerFactory;

    @BeforeAll
    public static void setupJsonMapper() {
        JsonTestSupport.defaultMapper = Serde.createMapper();
    }

    @BeforeEach
    public void setupJPADispatcher() throws Exception {
        jdbcHandlerFactory = new JDBCHandlerFactory();
    }

    public static class BaseEntity {
        @SmartClientField(type = DSField.FieldType.TEXT)
        private int id;

        private String data;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    public static class DescendantEntity extends BaseEntity {

        @SmartClientField(name = "DescendantValue")
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    public void smartClientFieldAnnotation_onFieldFromTheBaseClass() throws Exception {
        final String dsId = "TestDS";

        {
            final Field f = FieldUtils.getField(BaseEntity.class, "id", true);
            final DSField dsf = jdbcHandlerFactory.describeField(dsId, BaseEntity.class, f);

            Assertions.assertEquals(f.getType(), int.class);

            JsonTestSupport.assertJsonEquals("""
      		{
                name: 'id',
                type: 'TEXT'
      		}""", dsf, Option.IGNORING_EXTRA_FIELDS);

        }
        {
            final DataSource ds = jdbcHandlerFactory.describeEntity(DescendantEntity.class);
            ds.getField("id");

            final DSField dsf = ds.getField("id");

            JsonTestSupport.assertJsonEquals("""
      		{
                name: 'id',
                type: 'TEXT'
      		}""", dsf, Option.IGNORING_EXTRA_FIELDS);
        }
    }
}
