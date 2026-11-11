package com.studentgrading.service;

import com.studentgrading.annotations.Auditable;
import com.studentgrading.annotations.Author;
import com.studentgrading.annotations.Column;
import com.studentgrading.annotations.Entity;
import com.studentgrading.annotations.GradeRange;
import com.studentgrading.annotations.Id;
import com.studentgrading.annotations.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reflection-based Annotation Processor.
 * Demonstrates:
 * 1. Discovering annotations at runtime using Java Reflection API.
 * 2. Reading custom annotation values (@Author, @Table, @Column, @GradeRange, @Auditable).
 * 3. Validating object instances based on @GradeRange annotations.
 * 4. Generating SQL schema dynamically from annotations (mini ORM engine).
 */
public class AnnotationProcessor {

    /**
     * Inspects a class and returns a comprehensive report of all discovered annotations.
     */
    public Map<String, Object> inspectClass(Class<?> clazz) {
        Map<String, Object> report = new HashMap<>();
        report.put("className", clazz.getName());
        report.put("simpleName", clazz.getSimpleName());

        // 1. Inspect Class-Level Annotations
        Map<String, Object> classAnnotations = new HashMap<>();

        if (clazz.isAnnotationPresent(Author.class)) {
            Author author = clazz.getAnnotation(Author.class);
            Map<String, String> authorInfo = new HashMap<>();
            authorInfo.put("name", author.name());
            authorInfo.put("version", author.version());
            authorInfo.put("institution", author.institution());
            authorInfo.put("description", author.description());
            classAnnotations.put("@Author", authorInfo);
        }

        if (clazz.isAnnotationPresent(Entity.class)) {
            Entity entity = clazz.getAnnotation(Entity.class);
            classAnnotations.put("@Entity", entity.description());
        }

        if (clazz.isAnnotationPresent(Table.class)) {
            Table table = clazz.getAnnotation(Table.class);
            Map<String, String> tableInfo = new HashMap<>();
            tableInfo.put("tableName", table.name());
            tableInfo.put("schema", table.schema());
            classAnnotations.put("@Table", tableInfo);
        }

        report.put("classAnnotations", classAnnotations);

        // 2. Inspect Field-Level Annotations
        List<Map<String, Object>> fieldReports = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Map<String, Object> fieldData = new HashMap<>();
            fieldData.put("fieldName", field.getName());
            fieldData.put("fieldType", field.getType().getSimpleName());

            Map<String, Object> annotationsMap = new HashMap<>();

            if (field.isAnnotationPresent(Id.class)) {
                Id id = field.getAnnotation(Id.class);
                annotationsMap.put("@Id", "autoIncrement = " + id.autoIncrement());
            }

            if (field.isAnnotationPresent(Column.class)) {
                Column col = field.getAnnotation(Column.class);
                Map<String, Object> colDetails = new HashMap<>();
                colDetails.put("columnName", col.name());
                colDetails.put("nullable", col.nullable());
                colDetails.put("length", col.length());
                colDetails.put("unique", col.unique());
                annotationsMap.put("@Column", colDetails);
            }

            if (field.isAnnotationPresent(GradeRange.class)) {
                GradeRange range = field.getAnnotation(GradeRange.class);
                Map<String, Object> rangeDetails = new HashMap<>();
                rangeDetails.put("min", range.min());
                rangeDetails.put("max", range.max());
                rangeDetails.put("message", range.message());
                annotationsMap.put("@GradeRange", rangeDetails);
            }

            if (!annotationsMap.isEmpty()) {
                fieldData.put("annotations", annotationsMap);
                fieldReports.add(fieldData);
            }
        }
        report.put("fieldAnnotations", fieldReports);

        // 3. Inspect Method-Level Annotations
        List<Map<String, Object>> methodReports = new ArrayList<>();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            Map<String, Object> methodData = new HashMap<>();
            methodData.put("methodName", method.getName());

            Map<String, Object> annotationsMap = new HashMap<>();

            if (method.isAnnotationPresent(Auditable.class)) {
                Auditable auditable = method.getAnnotation(Auditable.class);
                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put("action", auditable.action());
                auditDetails.put("level", auditable.level());
                auditDetails.put("logToDatabase", auditable.logToDatabase());
                annotationsMap.put("@Auditable", auditDetails);
            }

            if (method.isAnnotationPresent(Deprecated.class)) {
                Deprecated dep = method.getAnnotation(Deprecated.class);
                annotationsMap.put("@Deprecated", "since = " + dep.since() + ", forRemoval = " + dep.forRemoval());
            }

            if (!annotationsMap.isEmpty()) {
                methodData.put("annotations", annotationsMap);
                methodReports.add(methodData);
            }
        }
        report.put("methodAnnotations", methodReports);

        // 4. Generate Dynamic DDL from Annotations
        if (clazz.isAnnotationPresent(Table.class)) {
            Table tbl = clazz.getAnnotation(Table.class);
            StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tbl.name() + " (\n");
            List<String> colDefs = new ArrayList<>();

            for (Field f : fields) {
                if (f.isAnnotationPresent(Column.class)) {
                    Column col = f.getAnnotation(Column.class);
                    String sqlType = "VARCHAR(" + col.length() + ")";
                    if (f.getType() == Integer.class || f.getType() == int.class) {
                        sqlType = "INTEGER";
                    } else if (f.getType() == Double.class || f.getType() == double.class) {
                        sqlType = "DOUBLE";
                    }

                    StringBuilder colDef = new StringBuilder("  " + col.name() + " " + sqlType);
                    if (f.isAnnotationPresent(Id.class)) {
                        colDef.append(" PRIMARY KEY");
                        if (f.getAnnotation(Id.class).autoIncrement()) {
                            colDef.append(" AUTOINCREMENT");
                        }
                    } else {
                        if (!col.nullable()) colDef.append(" NOT NULL");
                        if (col.unique()) colDef.append(" UNIQUE");
                    }
                    colDefs.add(colDef.toString());
                }
            }
            ddl.append(String.join(",\n", colDefs)).append("\n);");
            report.put("generatedDDL", ddl.toString());
        }

        return report;
    }

    /**
     * Validates an object against @GradeRange annotations on its fields.
     */
    public List<String> validateGradeRanges(Object target) {
        List<String> violations = new ArrayList<>();
        if (target == null) return violations;

        Class<?> clazz = target.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(GradeRange.class)) {
                GradeRange range = field.getAnnotation(GradeRange.class);
                field.setAccessible(true);
                try {
                    Object val = field.get(target);
                    if (val instanceof Number) {
                        double num = ((Number) val).doubleValue(); // Unboxing number
                        if (num < range.min() || num > range.max()) {
                            violations.add(String.format("Field '%s' with value %.2f violates @GradeRange (min=%.1f, max=%.1f): %s",
                                    field.getName(), num, range.min(), range.max(), range.message()));
                        }
                    }
                } catch (IllegalAccessException e) {
                    violations.add("Failed to access field: " + field.getName());
                }
            }
        }
        return violations;
    }
}

