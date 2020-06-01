package org.srg.smartclient.spring.autoconfiguration;

import org.springframework.transaction.PlatformTransactionManager;
import org.srg.smartclient.IDSDispatcher;

/**
 * Strategy interface for users to provide as a factory for custom components needed by a Smart Client system.
 *
 */
public interface SmartClientConfigurer {
        PlatformTransactionManager getTransactionManager() throws Exception;
        IDSDispatcher getDsDispatcher() throws Exception;
}
