package org.srg.smartclient.spring;

import org.springframework.context.annotation.Import;
import org.srg.smartclient.spring.autoconfiguration.SimpleSmartClientConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Enable Smart Client features and provide a base configuration for setting up batch jobs in an &#064;Configuration
 * class, roughly equivalent to using the {@code <batch:*>} XML namespace.</p>
*/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SimpleSmartClientConfiguration.class)
public @interface EnableSmartClient {
}
