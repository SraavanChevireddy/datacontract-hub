package com.sraavan.dchub.compat;

/** Thrown when a schema document cannot be parsed into a {@link Schema}. */
public class SchemaParseException extends RuntimeException {
    public SchemaParseException(String message) {
        super(message);
    }
}
