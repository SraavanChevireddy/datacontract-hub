package com.sraavan.dchub.compat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityCheckerTest {

    private final CompatibilityChecker checker = new CompatibilityChecker();

    private Schema schema(SchemaField... fields) {
        return new Schema(List.of(fields));
    }

    @Test
    void identicalSchemasAreCompatibleEverywhere() {
        Schema s = schema(
                SchemaField.of("id", FieldType.LONG, false),
                SchemaField.of("email", FieldType.STRING, true));
        for (CompatibilityMode mode : CompatibilityMode.values()) {
            CompatibilityResult r = checker.check(s, s, mode);
            assertTrue(r.compatible(), "identical schemas under " + mode);
            assertTrue(r.changes().isEmpty());
        }
    }

    @Test
    void addingOptionalFieldIsFullyCompatible() {
        Schema current = schema(SchemaField.of("id", FieldType.LONG, false));
        Schema candidate = schema(
                SchemaField.of("id", FieldType.LONG, false),
                SchemaField.of("nickname", FieldType.STRING, true));
        CompatibilityResult r = checker.check(current, candidate, CompatibilityMode.FULL);
        assertTrue(r.compatible());
        assertEquals(1, r.changes().size());
        assertEquals(Change.Kind.FIELD_ADDED, r.changes().get(0).kind());
    }

    @Test
    void addingRequiredFieldBreaksForwardNotBackward() {
        Schema current = schema(SchemaField.of("id", FieldType.LONG, false));
        Schema candidate = schema(
                SchemaField.of("id", FieldType.LONG, false),
                SchemaField.of("region", FieldType.STRING, false)); // required, no default

        assertTrue(checker.check(current, candidate, CompatibilityMode.BACKWARD).compatible());
        assertFalse(checker.check(current, candidate, CompatibilityMode.FORWARD).compatible());
        assertFalse(checker.check(current, candidate, CompatibilityMode.FULL).compatible());
    }

    @Test
    void addingRequiredFieldWithDefaultIsForwardCompatible() {
        Schema current = schema(SchemaField.of("id", FieldType.LONG, false));
        Schema candidate = schema(
                SchemaField.of("id", FieldType.LONG, false),
                SchemaField.of("region", FieldType.STRING, false, true)); // has default
        assertTrue(checker.check(current, candidate, CompatibilityMode.FULL).compatible());
    }

    @Test
    void removingRequiredFieldBreaksBackwardNotForward() {
        Schema current = schema(
                SchemaField.of("id", FieldType.LONG, false),
                SchemaField.of("email", FieldType.STRING, false));
        Schema candidate = schema(SchemaField.of("id", FieldType.LONG, false));

        assertFalse(checker.check(current, candidate, CompatibilityMode.BACKWARD).compatible());
        assertTrue(checker.check(current, candidate, CompatibilityMode.FORWARD).compatible());
    }

    @Test
    void removingOptionalFieldDoesNotBreakBackward() {
        Schema current = schema(
                SchemaField.of("id", FieldType.LONG, false),
                SchemaField.of("note", FieldType.STRING, true)); // optional
        Schema candidate = schema(SchemaField.of("id", FieldType.LONG, false));
        assertTrue(checker.check(current, candidate, CompatibilityMode.BACKWARD).compatible());
    }

    @Test
    void wideningTypeIsBackwardCompatible() {
        Schema current = schema(SchemaField.of("amount", FieldType.INT, false));
        Schema candidate = schema(SchemaField.of("amount", FieldType.LONG, false));
        // old INT data fits new LONG reader => backward ok; new LONG data may not fit old INT => forward breaks
        assertTrue(checker.check(current, candidate, CompatibilityMode.BACKWARD).compatible());
        assertFalse(checker.check(current, candidate, CompatibilityMode.FORWARD).compatible());
    }

    @Test
    void narrowingTypeBreaksBackward() {
        Schema current = schema(SchemaField.of("amount", FieldType.LONG, false));
        Schema candidate = schema(SchemaField.of("amount", FieldType.INT, false));
        assertFalse(checker.check(current, candidate, CompatibilityMode.BACKWARD).compatible());
    }

    @Test
    void makingFieldNullableBreaksForward() {
        Schema current = schema(SchemaField.of("email", FieldType.STRING, false));
        Schema candidate = schema(SchemaField.of("email", FieldType.STRING, true));
        assertFalse(checker.check(current, candidate, CompatibilityMode.FORWARD).compatible());
        assertTrue(checker.check(current, candidate, CompatibilityMode.BACKWARD).compatible());
    }

    @Test
    void makingFieldRequiredBreaksBackward() {
        Schema current = schema(SchemaField.of("email", FieldType.STRING, true));
        Schema candidate = schema(SchemaField.of("email", FieldType.STRING, false));
        assertFalse(checker.check(current, candidate, CompatibilityMode.BACKWARD).compatible());
    }

    @Test
    void nestedStructChangesAreDetectedWithDottedPath() {
        Schema current = schema(SchemaField.struct("address", false, List.of(
                SchemaField.of("zip", FieldType.STRING, false))));
        Schema candidate = schema(SchemaField.struct("address", false, List.of(
                SchemaField.of("zip", FieldType.STRING, false),
                SchemaField.of("country", FieldType.STRING, false)))); // required nested add
        CompatibilityResult r = checker.check(current, candidate, CompatibilityMode.FORWARD);
        assertFalse(r.compatible());
        assertEquals("address.country", r.violations().get(0).path());
    }

    @Test
    void structToScalarBreaksBothDirections() {
        Schema current = schema(SchemaField.struct("payload", false, List.of(
                SchemaField.of("x", FieldType.INT, false))));
        Schema candidate = schema(SchemaField.of("payload", FieldType.STRING, false));
        CompatibilityResult r = checker.check(current, candidate, CompatibilityMode.BACKWARD);
        assertFalse(r.compatible());
        assertEquals(Change.Kind.STRUCT_TO_SCALAR, r.changes().get(0).kind());
    }

    @Test
    void noneModeAllowsBreakingChanges() {
        Schema current = schema(SchemaField.of("id", FieldType.LONG, false));
        Schema candidate = schema(SchemaField.of("id", FieldType.STRING, false));
        CompatibilityResult r = checker.check(current, candidate, CompatibilityMode.NONE);
        assertTrue(r.compatible());
        assertFalse(r.changes().isEmpty(), "changes still recorded under NONE");
    }
}
