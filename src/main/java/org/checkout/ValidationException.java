package org.checkout;

import java.util.List;

public class ValidationException extends Exception {
    private List<ValidationError> errors;

    public ValidationException(List<ValidationError> errors) {
        super("Validation failed with " + errors.size() + " error(s)");
        this.errors = errors;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}