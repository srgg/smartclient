package org.srg.smartclient.annotations;

import org.srg.smartclient.isomorphic.DSField;

import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SmartClientField {
    String name() default "";
    String displayField() default "";
    String foreignDisplayField() default "";
    DSField.FieldType type() default DSField.FieldType.ANY;
}
