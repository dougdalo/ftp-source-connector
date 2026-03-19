package br.com.datastreambrasil.kafka.connector.ftp.validation;

import br.com.datastreambrasil.kafka.connector.ftp.model.ValidationResult;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurableValidatorTest {

    @Test
    void testNotEmptyValidation() {
        ConfigurableValidator validator = new ConfigurableValidator("name:not_empty");
        
        Schema schema = SchemaBuilder.struct()
                .field("name", Schema.STRING_SCHEMA)
                .build();
        
        Struct validRecord = new Struct(schema).put("name", "John");
        Struct invalidRecord = new Struct(schema).put("name", "");
        
        assertTrue(validator.validate(validRecord).isValid());
        assertFalse(validator.validate(invalidRecord).isValid());
    }

    @Test
    void testNumericValidation() {
        ConfigurableValidator validator = new ConfigurableValidator("age:numeric");
        
        Schema schema = SchemaBuilder.struct()
                .field("age", Schema.STRING_SCHEMA)
                .build();
        
        Struct validRecord1 = new Struct(schema).put("age", "25");
        Struct validRecord2 = new Struct(schema).put("age", "25.5");
        Struct invalidRecord = new Struct(schema).put("age", "abc");
        
        assertTrue(validator.validate(validRecord1).isValid());
        assertTrue(validator.validate(validRecord2).isValid());
        assertFalse(validator.validate(invalidRecord).isValid());
    }

    @Test
    void testRangeValidation() {
        ConfigurableValidator validator = new ConfigurableValidator("value:range(0,100)");
        
        Schema schema = SchemaBuilder.struct()
                .field("value", Schema.STRING_SCHEMA)
                .build();
        
        Struct validRecord = new Struct(schema).put("value", "50");
        Struct invalidRecordLow = new Struct(schema).put("value", "-5");
        Struct invalidRecordHigh = new Struct(schema).put("value", "150");
        
        assertTrue(validator.validate(validRecord).isValid());
        assertFalse(validator.validate(invalidRecordLow).isValid());
        assertFalse(validator.validate(invalidRecordHigh).isValid());
    }

    @Test
    void testEmailValidation() {
        ConfigurableValidator validator = new ConfigurableValidator("email:email");
        
        Schema schema = SchemaBuilder.struct()
                .field("email", Schema.STRING_SCHEMA)
                .build();
        
        Struct validRecord = new Struct(schema).put("email", "user@example.com");
        Struct invalidRecord = new Struct(schema).put("email", "invalid-email");
        
        assertTrue(validator.validate(validRecord).isValid());
        assertFalse(validator.validate(invalidRecord).isValid());
    }

    @Test
    void testDateValidation() {
        ConfigurableValidator validator = new ConfigurableValidator("date:date(yyyyMMdd)");
        
        Schema schema = SchemaBuilder.struct()
                .field("date", Schema.STRING_SCHEMA)
                .build();
        
        Struct validRecord = new Struct(schema).put("date", "20250211");
        Struct invalidRecord = new Struct(schema).put("date", "2025-02-11");
        
        assertTrue(validator.validate(validRecord).isValid());
        assertFalse(validator.validate(invalidRecord).isValid());
    }

    @Test
    void testMultipleRules() {
        ConfigurableValidator validator = new ConfigurableValidator(
                "name:not_empty,age:numeric,age:range(18,100),email:email");
        
        Schema schema = SchemaBuilder.struct()
                .field("name", Schema.STRING_SCHEMA)
                .field("age", Schema.STRING_SCHEMA)
                .field("email", Schema.STRING_SCHEMA)
                .build();
        
        Struct validRecord = new Struct(schema)
                .put("name", "John")
                .put("age", "30")
                .put("email", "john@example.com");
        
        Struct invalidRecord = new Struct(schema)
                .put("name", "")
                .put("age", "15")
                .put("email", "invalid");
        
        ValidationResult validResult = validator.validate(validRecord);
        ValidationResult invalidResult = validator.validate(invalidRecord);
        
        assertTrue(validResult.isValid());
        assertFalse(invalidResult.isValid());
        assertTrue(invalidResult.getErrors().size() > 1);
    }

    @Test
    void testLengthValidation() {
        ConfigurableValidator validator = new ConfigurableValidator(
                "code:length_min(3),code:length_max(10)");
        
        Schema schema = SchemaBuilder.struct()
                .field("code", Schema.STRING_SCHEMA)
                .build();
        
        Struct validRecord = new Struct(schema).put("code", "ABC123");
        Struct tooShort = new Struct(schema).put("code", "AB");
        Struct tooLong = new Struct(schema).put("code", "ABCDEFGHIJK");
        
        assertTrue(validator.validate(validRecord).isValid());
        assertFalse(validator.validate(tooShort).isValid());
        assertFalse(validator.validate(tooLong).isValid());
    }

    @Test
    void testEmptyRulesAlwaysValid() {
        ConfigurableValidator validator = new ConfigurableValidator("");
        
        Schema schema = SchemaBuilder.struct()
                .field("field", Schema.STRING_SCHEMA)
                .build();
        
        Struct record = new Struct(schema).put("field", "anything");
        
        assertTrue(validator.validate(record).isValid());
    }

    @Test
    void testNonExistentFieldInValidation() {
        ConfigurableValidator validator = new ConfigurableValidator("missing_field:not_empty");
        
        Schema schema = SchemaBuilder.struct()
                .field("existing_field", Schema.STRING_SCHEMA)
                .build();
        
        Struct record = new Struct(schema).put("existing_field", "value");
        
        ValidationResult result = validator.validate(record);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("does not exist"));
    }
}
