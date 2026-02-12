package br.com.datastreambrasil.kafka.connector.ftp.validation;

import br.com.datastreambrasil.kafka.connector.ftp.model.ValidationResult;
import org.apache.kafka.connect.data.Struct;

/**
 * Interface for record validators
 */
public interface RecordValidator {
    /**
     * Validate a record
     * @param record the record to validate
     * @return validation result
     */
    ValidationResult validate(Struct record);
}
