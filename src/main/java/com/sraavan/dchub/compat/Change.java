package com.sraavan.dchub.compat;

/**
 * A single difference detected between two schema versions, classified by the
 * direction(s) in which it breaks compatibility.
 *
 * @param path        dotted path to the field (e.g. {@code address.zipcode})
 * @param kind        what structurally changed
 * @param breaksBackward true if new readers could fail on old data
 * @param breaksForward  true if old readers could fail on new data
 * @param detail      human-readable explanation
 */
public record Change(
        String path,
        Kind kind,
        boolean breaksBackward,
        boolean breaksForward,
        String detail) {

    public enum Kind {
        FIELD_ADDED,
        FIELD_REMOVED,
        TYPE_CHANGED,
        NULLABILITY_CHANGED,
        STRUCT_TO_SCALAR
    }
}
