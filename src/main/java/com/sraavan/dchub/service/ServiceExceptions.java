package com.sraavan.dchub.service;

/** Domain exceptions mapped to HTTP status codes by the global exception handler. */
public final class ServiceExceptions {

    private ServiceExceptions() {
    }

    /** 404 — referenced contract or version does not exist. */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /** 409 — request conflicts with current state (e.g. duplicate contract name). */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    /** 422 — a candidate version violated the contract's compatibility mode. */
    public static class IncompatibleSchemaException extends RuntimeException {
        private final transient Object compatibility;

        public IncompatibleSchemaException(String message, Object compatibility) {
            super(message);
            this.compatibility = compatibility;
        }

        public Object getCompatibility() {
            return compatibility;
        }
    }
}
