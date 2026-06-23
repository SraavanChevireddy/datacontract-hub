package com.sraavan.dchub.web;

import com.sraavan.dchub.compat.SchemaParseException;
import com.sraavan.dchub.service.ServiceExceptions.ConflictException;
import com.sraavan.dchub.service.ServiceExceptions.IncompatibleSchemaException;
import com.sraavan.dchub.service.ServiceExceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain exceptions into RFC 7807 {@link ProblemDetail} responses with
 * appropriate status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail conflict(ConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(SchemaParseException.class)
    public ProblemDetail badSchema(SchemaParseException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IncompatibleSchemaException.class)
    public ProblemDetail incompatible(IncompatibleSchemaException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        pd.setProperty("compatibility", e.getCompatibility());
        return pd;
    }
}
