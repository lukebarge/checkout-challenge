package org.checkout.models;

import java.util.List;

public sealed interface ValidationResult<T> {
    

    record Success<T>(T value) implements ValidationResult<T> {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public List<String> errors() {
            return List.of();
        }

        @Override
        public T getValue() {
            return value;
        }
    }

    record Failure<T>(List<String> errors) implements ValidationResult<T> {
        public Failure(String error) {
            this(List.of(error));
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public T getValue() throws ValidationException {
            throw new ValidationException("Validation failed", errors);
        }
    }

    static <T> ValidationResult<T> success(T value) {
        return new Success<>(value);
    }

    static <T> ValidationResult<T> failure(String error) {
        return new Failure<>(error);
    }

    static <T> ValidationResult<T> failure(List<String> errors) {
        return new Failure<>(errors);
    }

    class ValidationException extends RuntimeException {
        private final List<String> errors;

        public ValidationException(String message, List<String> errors) {
            super(message);
            this.errors = errors;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    boolean isSuccess();
    boolean isFailure();
    List<String> errors();
    T getValue() throws ValidationException;
}