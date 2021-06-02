package org.srg.smartclient;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;
import org.srg.smartclient.annotations.SmartClientDMIHandler;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.spring.EnableSmartClient;
import org.srg.smartclient.utils.Serde;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration( classes = {SpringDMITest.TestConfig.class})
@EnableAutoConfiguration(exclude = {HttpMessageConvertersAutoConfiguration.class})
@EnableSmartClient
public class SpringDMITest {

    @SmartClientDMIHandler(id = "dmi1", methodName = "handleDMI")
    public static class SpringDMIHandlerBean {

        public DSResponse handleDMI(DSRequest request) throws Exception {
            final DSField dsf = new DSField();
            dsf.setType(DSField.FieldType.INTEGER);
            dsf.setName("data");

            return DSResponse.successFetch(0, 1, 1, List.of(dsf), Collections.singletonList(new Object[] {24}));
        }
    }

    public static class TestConfig {
        @Bean
        public DataSource fakeDataSource() {
            return new SimpleDriverDataSource();
        }

        @Bean
        public SpringDMIHandlerBean dmiHandler1() {
            return new SpringDMIHandlerBean();
        }
    }

    @Autowired
    private IDSDispatcher dispatcher;

    @BeforeAll
    public static void setupMapper() {
        JsonTestSupport.defaultMapper = Serde.createMapper();
    }

    @Test
    public void test() throws Exception {

        final  IHandler handler = dispatcher.getHandlerByName("dmi1");
        Assert.notNull(handler, "DMI handler is not registered");

        final DSRequest request = new DSRequest();
        request.setDataSource("dmi1");
        final Collection<DSResponse> responses = dispatcher.dispatch(request);

        JsonTestSupport.assertJsonEquals("""
                 [{
                     status: 0,
                     startRow: 0,
                     endRow: 1,
                     totalRows: 1,
                     data:[
                        {
                            data: 24
                        }
                     ]
                }]""", responses);
    }
}
