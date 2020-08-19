package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.IDSRequest;

import java.util.Collection;

/**
 * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/DateFormatAndStorage.html
 *
 */
public interface IDSDispatcher extends IDSRegistry {

    /**
     * Default path that will be used to look for  datasource description files in application resources
     */
    String DEFAULT_DS_PATH = "shared/ds";

    Collection<DSResponse> dispatch(IDSRequest request);
    <A extends Appendable> A generateDSJavaScript(A out, String dispatcherUrl, String... dsId) throws Exception;
    void registerDatasource(DSHandler handler);
    void loadFromResource(String path) throws Exception;

    default void loadFromResource() throws Exception {
        loadFromResource(DEFAULT_DS_PATH);
    }
}
