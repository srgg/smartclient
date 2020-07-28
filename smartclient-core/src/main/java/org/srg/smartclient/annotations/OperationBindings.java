package org.srg.smartclient.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies multiple operation bindings.
 * @see OperationBinding
 *
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface OperationBindings {
    OperationBinding[] value();
}
