package com.omnicare.emr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an action cannot be performed because an Encounter has been cancelled.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EncounterCancelledException extends RuntimeException {

    public EncounterCancelledException(String message) {
        super(message);
    }

    public EncounterCancelledException(String message, Throwable cause) {
        super(message, cause);
    }
}
