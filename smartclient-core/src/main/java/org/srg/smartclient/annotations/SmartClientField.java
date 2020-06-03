package org.srg.smartclient.annotations;

import org.srg.smartclient.isomorphic.DSField;

import java.lang.annotation.*;
import java.util.Optional;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SmartClientField {
    String name() default "";
    boolean hidden() default false;

    /**
     * Custom sql Column clause to make a calculated field for SQL and JPA Data Source
     * @return
     */
    String customSelectExpression() default "";
    String displayField() default "";
    String foreignDisplayField() default "";
    DSField.FieldType type() default DSField.FieldType.ANY;
}
