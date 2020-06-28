package org.srg.smartclient.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.srg.smartclient.DSDispatcher;

/**
 * Configuration properties for SmartClient.
*/
@ConfigurationProperties(prefix = "spring.smartclient")
public class SmartClientProperties {

    private String dispatcherPath = "/dispatcher";
    private String sharedDirectory = DSDispatcher.DEFAULT_DS_PATH;

    public String getDispatcherPath() {
        return dispatcherPath;
    }

    public void setDispatcherPath(String dispatcherPath) {
        this.dispatcherPath = dispatcherPath;
    }

    public String getSharedDirectory() {
        return sharedDirectory;
    }

    public void setSharedDirectory(String sharedDirectory) {
        this.sharedDirectory = sharedDirectory;
    }
}
