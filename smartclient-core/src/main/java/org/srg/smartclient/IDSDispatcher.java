package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.DataSource;
import org.srg.smartclient.isomorphic.IDSRequest;

import java.util.Collection;

/**
 * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/DateFormatAndStorage.html
 *
 */
public interface IDSDispatcher {

    /**
     * Default path that will be used to look for  datasource description files in application resources
     */
    String DEFAULT_DS_PATH = "shared/ds";

    DataSource dataSource(String dsId);
    Collection<DSResponse> dispatch(IDSRequest request);
    <A extends Appendable> A generateDSJavaScript(A out, String dispatcherUrl, String... dsId) throws Exception;
    void registerDatasource(DSHandler handler);
    void loadFromResource(String path);

    default void loadFromResource() {
        loadFromResource(DEFAULT_DS_PATH);
    }
}
