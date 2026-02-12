package br.com.datastreambrasil.kafka.connector.ftp.validation;

import br.com.datastreambrasil.kafka.connector.ftp.model.ValidationResult;
import org.apache.kafka.connect.data.Struct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Configurable validator that applies rules to struct fields
 * Configuration format: "field1:not_empty,field2:numeric,field3:range(0,100)"
 */
public class ConfigurableValidator implements RecordValidator {
    private static final Logger log = LoggerFactory.getLogger(ConfigurableValidator.class);

    private final Map<String, List<ValidationRule>> fieldRules;

    public ConfigurableValidator(String rulesConfig) {
        this.fieldRules = parseRules(rulesConfig);
    }

    @Override
    public ValidationResult validate(Struct record) {
        if (fieldRules.isEmpty()) {
            return ValidationResult.valid();
        }

        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, List<ValidationRule>> entry : fieldRules.entrySet()) {
            String fieldName = entry.getKey();
            Object value = null;

            try {
                value = record.get(fieldName);
            } catch (Exception e) {
                errors.add(String.format("Field '%s' does not exist in record", fieldName));
                continue;
            }

            for (ValidationRule rule : entry.getValue()) {
                if (!rule.isValid(value)) {
                    errors.add(String.format("Field '%s' validation '%s' failed: %s (value='%s')",
                            fieldName, rule.getName(), rule.getMessage(), value));
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    private Map<String, List<ValidationRule>> parseRules(String rulesConfig) {
        Map<String, List<ValidationRule>> rules = new HashMap<>();

        if (rulesConfig == null || rulesConfig.trim().isEmpty()) {
            return rules;
        }

        // Format: "field1:rule1,field2:rule2(param1,param2),field3:rule3"
        String[] fieldConfigs = rulesConfig.split(",");

        for (String fieldConfig : fieldConfigs) {
            String[] parts = fieldConfig.trim().split(":", 2);
            if (parts.length != 2) {
                log.warn("Invalid validation rule format: {}", fieldConfig);
                continue;
            }

            String fieldName = parts[0].trim();
            String ruleSpec = parts[1].trim();

            ValidationRule rule = parseRule(ruleSpec);
            if (rule != null) {
                rules.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(rule);
            }
        }

        return rules;
    }

    private ValidationRule parseRule(String ruleSpec) {
        // Check for parameterized rules: rule_name(param1,param2)
        int parenIndex = ruleSpec.indexOf('(');
        String ruleName;
        String[] params = new String[0];

        if (parenIndex > 0) {
            ruleName = ruleSpec.substring(0, parenIndex);
            String paramsStr = ruleSpec.substring(parenIndex + 1, ruleSpec.length() - 1);
            params = paramsStr.split(",");
            for (int i = 0; i < params.length; i++) {
                params[i] = params[i].trim();
            }
        } else {
            ruleName = ruleSpec;
        }

        try {
            switch (ruleName.toLowerCase()) {
                case "not_empty":
                    return BuiltInValidationRules.notEmpty();
                case "numeric":
                    return BuiltInValidationRules.numeric();
                case "integer":
                    return BuiltInValidationRules.integer();
                case "email":
                    return BuiltInValidationRules.email();
                case "date":
                    if (params.length > 0) {
                        return BuiltInValidationRules.date(params[0]);
                    } else {
                        return BuiltInValidationRules.date("yyyyMMdd");
                    }
                case "length_min":
                    if (params.length > 0) {
                        return BuiltInValidationRules.lengthMin(Integer.parseInt(params[0]));
                    }
                    break;
                case "length_max":
                    if (params.length > 0) {
                        return BuiltInValidationRules.lengthMax(Integer.parseInt(params[0]));
                    }
                    break;
                case "range":
                    if (params.length >= 2) {
                        return BuiltInValidationRules.range(
                                Double.parseDouble(params[0]),
                                Double.parseDouble(params[1])
                        );
                    }
                    break;
                case "pattern":
                    if (params.length > 0) {
                        return BuiltInValidationRules.pattern(params[0]);
                    }
                    break;
                default:
                    log.warn("Unknown validation rule: {}", ruleName);
            }
        } catch (Exception e) {
            log.error("Error parsing validation rule: {}", ruleSpec, e);
        }

        return null;
    }

    public Map<String, List<ValidationRule>> getFieldRules() {
        return Collections.unmodifiableMap(fieldRules);
    }
}
