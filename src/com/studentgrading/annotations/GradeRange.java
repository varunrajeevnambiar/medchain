package com.studentgrading.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Validation Annotation to ensure marks/grades fall within valid numerical bounds.
 * Demonstrates: Annotation with parameters, default values, and runtime reflection validation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface GradeRange {
    double min() default 0.0;
    double max() default 100.0;
    String message() default "Score must be between 0 and 100 marks";
}

