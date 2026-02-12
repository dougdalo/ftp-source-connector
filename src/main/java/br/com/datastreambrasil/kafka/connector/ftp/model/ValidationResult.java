package br.com.datastreambrasil.kafka.connector.ftp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a record validation operation
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, Collections.singletonList(error));
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public String getErrorMessage() {
        return String.join("; ", errors);
    }

    @Override
    public String toString() {
        return valid ? "Valid" : "Invalid: " + getErrorMessage();
    }
}
