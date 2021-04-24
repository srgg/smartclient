package org.srg.smartclient.annotations;

import org.srg.smartclient.isomorphic.ServerObject;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SmartClientDMIHandler {
    String id();

    String className() default "";
    String methodName() default "";
    String bean() default "";

    ServerObject.LookupStyle lookupStyle() default ServerObject.LookupStyle.New;

    boolean crudOnly() default false;
    boolean dropExtraFields() default false;

}
