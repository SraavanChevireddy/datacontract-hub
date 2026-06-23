package com.sraavan.dchub.compat;

import java.util.List;
import java.util.Objects;

/**
 * A single field in a data-contract schema.
 *
 * @param name     field name (the identity used to match fields across versions)
 * @param type     logical type
 * @param nullable whether the field may be null / absent
 * @param hasDefault whether the field declares a default value (matters for adding
 *                   required fields without breaking old readers)
 * @param children nested fields when {@code type == STRUCT}; empty otherwise
 */
public record SchemaField(
        String name,
        FieldType type,
        boolean nullable,
        boolean hasDefault,
        List<SchemaField> children) {

    public SchemaField {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        children = children == null ? List.of() : List.copyOf(children);
    }

    public static SchemaField of(String name, FieldType type, boolean nullable) {
        return new SchemaField(name, type, nullable, false, List.of());
    }

    public static SchemaField of(String name, FieldType type, boolean nullable, boolean hasDefault) {
        return new SchemaField(name, type, nullable, hasDefault, List.of());
    }

    public static SchemaField struct(String name, boolean nullable, List<SchemaField> children) {
        return new SchemaField(name, FieldType.STRUCT, nullable, false, children);
    }
}
