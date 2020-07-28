package org.srg.smartclient.annotations;

import org.srg.smartclient.isomorphic.DSRequest;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * https://www.smartclient.com/smartgwtee-release/javadoc/com/smartgwt/client/docs/serverds/OperationBinding.html
 *
 * An operationBinding tells a DataSource how to execute one of the basic DS operations: fetch, add, update, remove.
 */
@Repeatable(OperationBindings.class)
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface OperationBinding {
    DSRequest.OperationType operationType();

    /**
     * https://www.smartclient.com/smartgwtee-release/javadoc/com/smartgwt/client/docs/serverds/OperationBinding.html#whereClause
     *
     * This property can be specified on an operationBinding to provide the server with a bespoke WHERE clause to use when constructing the SQL query to perform this operation. The property should be a valid expression in the syntax of the underlying database.
     *
     * https://www.smartclient.com/smartgwt-latest/javadoc/com/smartgwt/client/docs/CustomQuerying.html
     */
    String whereClause() default "";
}
