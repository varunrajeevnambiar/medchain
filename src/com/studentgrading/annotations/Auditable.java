package com.studentgrading.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Method Annotation used to mark grading and audit actions.
 * Demonstrates: Method-level interception/logging annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {
    String action();
    String level() default "INFO";
    boolean logToDatabase() default false;
}

