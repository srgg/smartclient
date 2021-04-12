package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;

public interface IHandler {
    String id();
    DSResponse handle(DSRequest request) throws Exception;

}
