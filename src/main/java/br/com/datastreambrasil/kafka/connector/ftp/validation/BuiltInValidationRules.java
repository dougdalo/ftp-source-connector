package br.com.datastreambrasil.kafka.connector.ftp.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Built-in validation rules
 */
public class BuiltInValidationRules {

    public static ValidationRule notEmpty() {
        return new ValidationRule() {
            @Override
            public String getName() {
                return "not_empty";
            }

            @Override
            public boolean isValid(Object value) {
                return value != null && !value.toString().trim().isEmpty();
            }

            @Override
            public String getMessage() {
                return "Field must not be empty";
            }
        };
    }

    public static ValidationRule numeric() {
        return new ValidationRule() {
            @Override
            public String getName() {
                return "numeric";
            }

            @Override
            public boolean isValid(Object value) {
                if (value == null) return false;
                return value.toString().matches("-?\\d+(\\.\\d+)?");
            }

            @Override
            public String getMessage() {
                return "Field must be numeric";
            }
        };
    }

    public static ValidationRule integer() {
        return new ValidationRule() {
            @Override
            public String getName() {
                return "integer";
            }

            @Override
            public boolean isValid(Object value) {
                if (value == null) return false;
                return value.toString().matches("-?\\d+");
            }

            @Override
            public String getMessage() {
                return "Field must be an integer";
            }
        };
    }

    public static ValidationRule date(String pattern) {
        return new ValidationRule() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

            @Override
            public String getName() {
                return "date";
            }

            @Override
            public boolean isValid(Object value) {
                if (value == null) return false;
                try {
                    LocalDate.parse(value.toString(), formatter);
                    return true;
                } catch (DateTimeParseException e) {
                    return false;
                }
            }

            @Override
            public String getMessage() {
                return "Field must be a valid date with format: " + pattern;
            }
        };
    }

    public static ValidationRule lengthMin(int minLength) {
        return new ValidationRule() {
            @Override
            public String getName() {
                return "length_min";
            }

            @Override
            public boolean isValid(Object value) {
                if (value == null) return false;
                return value.toString().length() >= minLength;
            }

            @Override
            public String getMessage() {
                return "Field must have minimum length of " + minLength;
            }
        };
    }

    public static ValidationRule lengthMax(int maxLength) {
        return new ValidationRule() {
            @Override
            public String getName() {
                return "length_max";
            }

            @Override
            public boolean isValid(Object value) {
                if (value == null) return false;
                return value.toString().length() <= maxLength;
            }

            @Override
            public String getMessage() {
                return "Field must have maximum length of " + maxLength;
            }
        };
    }

    public static ValidationRule range(double min, double max) {
        return new ValidationRule() {
            @Override
            public String getName() {
                return "range";
            }

            @Override
            public boolean isValid(Object value) {
                if (value == null) return false;
                try {
                    double val = Double.parseDouble(value.toString());
                    return val >= min && val <= max;
                } catch (NumberFormatException e) {
                    return false;
                }
            }

            @Override
            public String getMessage() {
                return "Field must be between " + min + " and " + max;
            }
        };
    }

    public static ValidationRule pattern(String regex) {
        return new ValidationRule() {
            @Override
            public String getName() {
                return "pattern";
            }

            @Override
            public boolean isValid(Object value) {
                if (value == null) return false;
                return value.toString().matches(regex);
            }

            @Override
            public String getMessage() {
                return "Field must match pattern: " + regex;
            }
        };
    }

    public static ValidationRule email() {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return new ValidationRule() {
            @Override
            public String getName() {
                return "email";
            }

            @Override
            public boolean isValid(Object value) {
                if (value == null) return false;
                return value.toString().matches(emailRegex);
            }

            @Override
            public String getMessage() {
                return "Field must be a valid email address";
            }
        };
    }
}
