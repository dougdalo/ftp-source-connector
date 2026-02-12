package br.com.datastreambrasil.kafka.connector.ftp.validation;

/**
 * Represents a single validation rule that can be applied to a field value
 */
public interface ValidationRule {
    /**
     * Get the name of this validation rule
     */
    String getName();

    /**
     * Check if the value is valid according to this rule
     * @param value the value to validate
     * @return true if valid, false otherwise
     */
    boolean isValid(Object value);

    /**
     * Get a descriptive error message for when validation fails
     */
    String getMessage();
}
