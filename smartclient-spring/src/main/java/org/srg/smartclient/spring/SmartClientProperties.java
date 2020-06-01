package org.srg.smartclient.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.srg.smartclient.DSDispatcher;

/**
 * Configuration properties for SmartClient.
*/
@ConfigurationProperties(prefix = "spring.smartclient")
public class SmartClientProperties {

    private String dispatcherUrl;
    private String sharedDirectory = DSDispatcher.DEFAULT_DS_PATH;

    public String getDispatcherUrl() {
        return dispatcherUrl;
    }

    public void setDispatcherUrl(String dispatcherUrl) {
        this.dispatcherUrl = dispatcherUrl;
    }

    public String getSharedDirectory() {
        return sharedDirectory;
    }

    public void setSharedDirectory(String sharedDirectory) {
        this.sharedDirectory = sharedDirectory;
    }
}
