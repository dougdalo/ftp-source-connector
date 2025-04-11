package br.com.datastreambrasil.kafka.connector.ftp;

import org.apache.kafka.connect.data.Schema;

public class RecordModel {
    final Object value;
    final Schema schema;

    RecordModel(Object value, Schema schema) {
        this.value = value;
        this.schema = schema;
    }
}
