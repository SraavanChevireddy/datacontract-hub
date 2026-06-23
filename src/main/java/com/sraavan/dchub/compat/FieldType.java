package com.sraavan.dchub.compat;

/**
 * The set of logical field types a data contract can declare.
 *
 * <p>Widening rules (used by the compatibility engine) follow the same spirit as
 * Avro/Protobuf promotion: an {@code INT} can safely widen to a {@code LONG} or
 * {@code DOUBLE}, a {@code FLOAT} to a {@code DOUBLE}, etc. Narrowing is a breaking
 * change because existing data may not fit the smaller type.
 */
public enum FieldType {
    BOOLEAN,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    STRING,
    BYTES,
    TIMESTAMP,
    DATE,
    STRUCT;

    /**
     * @return true if a value of this type can be read as {@code target} without loss.
     */
    public boolean widensTo(FieldType target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case INT -> target == LONG || target == DOUBLE || target == FLOAT;
            case LONG -> target == DOUBLE;
            case FLOAT -> target == DOUBLE;
            case DATE -> target == TIMESTAMP;
            default -> false;
        };
    }
}
