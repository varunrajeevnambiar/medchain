package com.studentgrading.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Annotation to document author metadata of classes and modules.
 * Demonstrates: Custom Annotations, @Target, @Retention, @Documented.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Author {
    String name() default "Student Developer";
    String version() default "1.0";
    String institution() default "Department of Computer Science & Engineering";
    String description() default "";
}

