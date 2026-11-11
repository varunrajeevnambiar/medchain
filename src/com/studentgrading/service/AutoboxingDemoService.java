package com.studentgrading.service;

import com.studentgrading.model.Grade;
import com.studentgrading.model.Subject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service dedicated to demonstrating Autoboxing, Unboxing, and Wrapper Class behaviors in Java.
 * Demonstrates:
 * 1. Primitive <-> Wrapper transitions.
 * 2. Autoboxing in Collections (List<Integer>, Map<String, Double>).
 * 3. Implicit unboxing during arithmetic calculations (+, -, *, /).
 * 4. Wrapper Equality (== vs .equals() & Integer Cache -128 to 127).
 * 5. Null safety when unboxing wrappers.
 */
public class AutoboxingDemoService {

    /**
     * Executes live demonstrations and returns structured explanation items.
     */
    public Map<String, Object> runAutoboxingDemos() {
        Map<String, Object> results = new HashMap<>();

        // DEMO 1: Basic Autoboxing & Unboxing
        int primitiveScore = 88;
        // Autoboxing: primitive 'int' is automatically converted to 'Integer' object
        Integer boxedScore = primitiveScore; 

        // Unboxing: 'Integer' object is automatically converted back to primitive 'int'
        int unboxedScore = boxedScore;

        Map<String, Object> demo1 = new HashMap<>();
        demo1.put("primitiveScore", primitiveScore);
        demo1.put("boxedType", boxedScore.getClass().getName());
        demo1.put("boxedValue", boxedScore);
        demo1.put("unboxedValue", unboxedScore);
        demo1.put("explanation", "Compiler implicitly calls Integer.valueOf(primitiveScore) during boxing, and boxedScore.intValue() during unboxing.");
        results.put("basicDemo", demo1);

        // DEMO 2: Collections Autoboxing
        // Java Collections CANNOT store primitives (e.g. List<int> is invalid syntax)
        List<Double> marksList = new ArrayList<>();
        double m1 = 92.5, m2 = 84.0, m3 = 78.5, m4 = 95.0, m5 = 89.0;
        
        // Autoboxing happens on every .add() call
        marksList.add(m1);
        marksList.add(m2);
        marksList.add(m3);
        marksList.add(m4);
        marksList.add(m5);

        // DEMO 3: Unboxing in Loop & Arithmetic
        double sum = 0.0;
        for (Double markWrapper : marksList) {
            // Unboxing: markWrapper is unboxed to double to execute '+' operator
            sum += markWrapper; 
        }
        double average = sum / marksList.size();

        Map<String, Object> demo2 = new HashMap<>();
        demo2.put("storedValues", marksList);
        demo2.put("calculatedSum", sum);
        demo2.put("calculatedAverage", average);
        demo2.put("explanation", "Primitive marks are boxed into List<Double>. During total calculation, each Double wrapper is unboxed for '+'.");
        results.put("collectionsDemo", demo2);

        // DEMO 4: Wrapper Equality & Integer Cache Trap
        Integer numA = 100;
        Integer numB = 100;
        boolean equalRefSmall = (numA == numB); // TRUE because of Integer Cache (-128 to 127)

        Integer numC = 250;
        Integer numD = 250;
        boolean equalRefLarge = (numC == numD); // FALSE: separate heap objects
        boolean equalsMethodLarge = numC.equals(numD); // TRUE: compares primitive values

        Map<String, Object> demo3 = new HashMap<>();
        demo3.put("cacheComparison", "numA(100) == numB(100) -> " + equalRefSmall + " (Cached in -128 to 127)");
        demo3.put("nonCacheComparison", "numC(250) == numD(250) -> " + equalRefLarge + " (Different object references)");
        demo3.put("equalsMethodComparison", "numC.equals(numD) -> " + equalsMethodLarge + " (Proper content comparison)");
        demo3.put("takeaway", "Always use .equals() or unboxed primitives (numC.intValue() == numD.intValue()) when comparing wrappers!");
        results.put("equalityDemo", demo3);

        // DEMO 5: Null Safety Demonstration
        Double nullableScore = null;
        boolean threwNpe = false;
        try {
            @SuppressWarnings("unused")
            double unsafeUnbox = nullableScore; // Throws NullPointerException!
        } catch (NullPointerException npe) {
            threwNpe = true;
        }

        // Safe unboxing pattern
        double safeUnbox = (nullableScore != null) ? nullableScore : 0.0;

        Map<String, Object> demo4 = new HashMap<>();
        demo4.put("threwNpeOnNullUnbox", threwNpe);
        demo4.put("safeFallbackValue", safeUnbox);
        demo4.put("explanation", "Attempting to unbox null automatically triggers NullPointerException. Defensive null check is essential.");
        results.put("nullSafetyDemo", demo4);

        return results;
    }
}

