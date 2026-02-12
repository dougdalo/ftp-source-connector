# Configuration Examples

## Basic Configuration (Minimal)

```json
{
  "name": "ftp-source-basic",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    "ftp.protocol": "sftp",
    "ftp.host": "ftp.example.com",
    "ftp.port": "22",
    "ftp.username": "ftpuser",
    "ftp.password": "secret123",
    "ftp.directory": "/data/input",
    "ftp.directory.stage": "/data/staging",
    "ftp.directory.archive": "/data/archive",
    "topic": "ftp-data",
    "tasks.max": "1"
  }
}
```

---

## Production Configuration (Recommended)

```json
{
  "name": "ftp-source-production",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "sftp",
    "ftp.host": "ftp.example.com",
    "ftp.port": "22",
    "ftp.username": "ftpuser",
    "ftp.password": "secret123",
    
    "ftp.directory": "/data/input",
    "ftp.directory.stage": "/data/staging",
    "ftp.directory.archive": "/data/archive",
    "ftp.file.pattern": "sales_.*\\.csv",
    
    "ftp.file.encoding": "UTF-8",
    "ftp.file.output.format": "json",
    "ftp.file.tokenizer": ",",
    "ftp.file.headers": "transaction_id,customer_id,product_id,quantity,price,timestamp",
    "ftp.kafka.key.field": "transaction_id",
    
    "ftp.retry.max.attempts": "3",
    "ftp.retry.backoff.ms": "1000",
    "ftp.retry.max.backoff.ms": "30000",
    
    "ftp.validation.enabled": "true",
    "ftp.validation.rules": "transaction_id:not_empty,quantity:integer,price:numeric",
    "ftp.validation.mode": "strict",
    
    "ftp.dlq.enabled": "true",
    "ftp.dlq.topic": "sales-errors",
    
    "ftp.max.records.per.poll": "1000",
    "ftp.poll.interval.ms": "10000",
    "ftp.buffer.size.bytes": "32768",
    
    "topic": "sales-data",
    "tasks.max": "1"
  }
}
```

---

## CSV with Headers to Skip

```json
{
  "name": "ftp-source-csv-headers",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "ftp",
    "ftp.host": "ftp.example.com",
    "ftp.port": "21",
    "ftp.username": "ftpuser",
    "ftp.password": "secret123",
    
    "ftp.directory": "/csv",
    "ftp.directory.stage": "/csv/staging",
    "ftp.directory.archive": "/csv/archive",
    "ftp.file.pattern": ".*\\.csv",
    
    "ftp.file.skip.header.lines": "1",
    "ftp.file.skip.footer.lines": "0",
    "ftp.file.empty.lines.skip": "true",
    
    "ftp.file.output.format": "json",
    "ftp.file.tokenizer": ",",
    "ftp.file.headers": "id,name,email,age,city",
    
    "topic": "csv-data",
    "tasks.max": "1"
  }
}
```

---

## Log Files with Comments

```json
{
  "name": "ftp-source-logs",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "sftp",
    "ftp.host": "log-server.example.com",
    "ftp.port": "22",
    "ftp.username": "loguser",
    "ftp.password": "secret123",
    
    "ftp.directory": "/logs",
    "ftp.directory.stage": "/logs/staging",
    "ftp.directory.archive": "/logs/archive",
    "ftp.file.pattern": "app_.*\\.log",
    
    "ftp.file.comment.prefix": "#",
    "ftp.file.empty.lines.skip": "true",
    "ftp.file.output.format": "string",
    
    "topic": "application-logs",
    "tasks.max": "1"
  }
}
```

---

## Compressed Files (.gz)

```json
{
  "name": "ftp-source-compressed",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "sftp",
    "ftp.host": "ftp.example.com",
    "ftp.port": "22",
    "ftp.username": "ftpuser",
    "ftp.password": "secret123",
    
    "ftp.directory": "/compressed",
    "ftp.directory.stage": "/compressed/staging",
    "ftp.directory.archive": "/compressed/archive",
    "ftp.file.pattern": ".*\\.gz",
    
    "ftp.file.compression.auto.detect": "true",
    
    "ftp.file.output.format": "json",
    "ftp.file.tokenizer": ";",
    "ftp.file.headers": "timestamp,level,message",
    
    "topic": "compressed-data",
    "tasks.max": "1"
  }
}
```

---

## High-Throughput Large Files

```json
{
  "name": "ftp-source-large-files",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "sftp",
    "ftp.host": "ftp.example.com",
    "ftp.port": "22",
    "ftp.username": "ftpuser",
    "ftp.password": "secret123",
    
    "ftp.directory": "/bigdata",
    "ftp.directory.stage": "/bigdata/staging",
    "ftp.directory.archive": "/bigdata/archive",
    "ftp.file.pattern": "large_.*\\.txt",
    
    "ftp.max.records.per.poll": "500",
    "ftp.buffer.size.bytes": "131072",
    "ftp.poll.interval.ms": "30000",
    "ftp.metrics.interval.lines": "50000",
    
    "ftp.retry.max.attempts": "5",
    "ftp.retry.max.backoff.ms": "60000",
    
    "ftp.file.output.format": "string",
    
    "topic": "large-files",
    "tasks.max": "1"
  }
}
```

---

## Data Quality with Strict Validation

```json
{
  "name": "ftp-source-validated",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "sftp",
    "ftp.host": "ftp.example.com",
    "ftp.port": "22",
    "ftp.username": "ftpuser",
    "ftp.password": "secret123",
    
    "ftp.directory": "/quality",
    "ftp.directory.stage": "/quality/staging",
    "ftp.directory.archive": "/quality/archive",
    "ftp.file.pattern": "users_.*\\.csv",
    
    "ftp.file.output.format": "json",
    "ftp.file.tokenizer": ",",
    "ftp.file.headers": "user_id,username,email,age,join_date",
    
    "ftp.validation.enabled": "true",
    "ftp.validation.mode": "strict",
    "ftp.validation.rules": "user_id:not_empty,user_id:integer,username:not_empty,username:length_min(3),email:email,age:range(13,120),join_date:date(yyyy-MM-dd)",
    
    "ftp.dlq.enabled": "true",
    "ftp.dlq.topic": "users-validation-errors",
    
    "topic": "users",
    "tasks.max": "1"
  }
}
```

---

## Financial Data with Complex Validation

```json
{
  "name": "ftp-source-financial",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "sftp",
    "ftp.host": "secure-ftp.bank.com",
    "ftp.port": "22",
    "ftp.username": "bankuser",
    "ftp.password": "verysecret",
    
    "ftp.directory": "/transactions",
    "ftp.directory.stage": "/transactions/staging",
    "ftp.directory.archive": "/transactions/archive",
    "ftp.file.pattern": "trans_\\d{8}\\.csv",
    
    "ftp.file.skip.header.lines": "1",
    "ftp.file.output.format": "json",
    "ftp.file.tokenizer": ",",
    "ftp.file.headers": "trans_id,account_from,account_to,amount,currency,timestamp,type",
    "ftp.kafka.key.field": "trans_id",
    
    "ftp.validation.enabled": "true",
    "ftp.validation.mode": "strict",
    "ftp.validation.rules": "trans_id:not_empty,account_from:pattern(\\d{10}),account_to:pattern(\\d{10}),amount:numeric,amount:range(0.01,1000000),currency:pattern([A-Z]{3}),type:not_empty",
    
    "ftp.dlq.enabled": "true",
    "ftp.dlq.topic": "transactions-errors",
    
    "ftp.retry.max.attempts": "5",
    "ftp.retry.backoff.ms": "2000",
    
    "topic": "financial-transactions",
    "tasks.max": "1"
  }
}
```

---

## IoT Sensor Data (Semicolon Delimited)

```json
{
  "name": "ftp-source-iot",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "sftp",
    "ftp.host": "iot.example.com",
    "ftp.port": "22",
    "ftp.username": "iotuser",
    "ftp.password": "secret123",
    
    "ftp.directory": "/sensors",
    "ftp.directory.stage": "/sensors/staging",
    "ftp.directory.archive": "/sensors/archive",
    "ftp.file.pattern": "sensor_.*\\.txt",
    
    "ftp.file.output.format": "json",
    "ftp.file.tokenizer": ";",
    "ftp.file.headers": "sensor_id,timestamp,temperature,humidity,pressure",
    "ftp.kafka.key.field": "sensor_id+timestamp",
    
    "ftp.validation.enabled": "true",
    "ftp.validation.rules": "sensor_id:not_empty,temperature:numeric,temperature:range(-50,150),humidity:range(0,100),pressure:numeric",
    
    "ftp.max.records.per.poll": "5000",
    
    "topic": "iot-readings",
    "tasks.max": "1"
  }
}
```

---

## Simple String Output (No JSON)

```json
{
  "name": "ftp-source-simple",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "ftp",
    "ftp.host": "ftp.example.com",
    "ftp.port": "21",
    "ftp.username": "ftpuser",
    "ftp.password": "secret123",
    
    "ftp.directory": "/simple",
    "ftp.directory.stage": "/simple/staging",
    "ftp.directory.archive": "/simple/archive",
    "ftp.file.pattern": ".*\\.txt",
    
    "ftp.file.output.format": "string",
    
    "topic": "raw-text",
    "tasks.max": "1"
  }
}
```

---

## Development/Testing Configuration

```json
{
  "name": "ftp-source-dev",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "sftp",
    "ftp.host": "localhost",
    "ftp.port": "2222",
    "ftp.username": "testuser",
    "ftp.password": "test123",
    
    "ftp.directory": "/test/input",
    "ftp.directory.stage": "/test/staging",
    "ftp.directory.archive": "/test/archive",
    "ftp.file.pattern": "test_.*\\.csv",
    
    "ftp.file.output.format": "json",
    "ftp.file.tokenizer": ",",
    "ftp.file.headers": "id,value",
    
    "ftp.validation.enabled": "true",
    "ftp.validation.mode": "lenient",
    "ftp.validation.rules": "id:integer,value:not_empty",
    
    "ftp.dlq.enabled": "true",
    "ftp.dlq.topic": "test-errors",
    
    "ftp.poll.interval.ms": "5000",
    "ftp.max.records.per.poll": "100",
    
    "topic": "test-topic",
    "tasks.max": "1"
  }
}
```

---

## Configuration for Migration from v1.0

```json
{
  "name": "ftp-source-migration",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "sftp",
    "ftp.host": "ftp.example.com",
    "ftp.port": "22",
    "ftp.username": "ftpuser",
    "ftp.password": "secret123",
    
    "ftp.directory": "/data",
    "ftp.directory.stage": "/staging",
    "ftp.directory.archive": "/archive",
    "ftp.file.pattern": ".*\\.txt",
    
    "ftp.file.encoding": "UTF-8",
    "ftp.file.output.format": "json",
    "ftp.file.tokenizer": ";",
    "ftp.file.headers": "type,date,time,code,value",
    "ftp.kafka.key.field": "type+code",
    
    "ftp.poll.interval.ms": "10000",
    "ftp.max.records.per.poll": "1000",
    
    "topic": "ftp-data-topic",
    "tasks.max": "1"
  }
}
```

---

## Deploying Configuration

### Using REST API

```bash
# Create connector
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @config.json

# Update connector
curl -X PUT http://localhost:8083/connectors/ftp-source/config \
  -H "Content-Type: application/json" \
  -d @config.json

# Delete connector
curl -X DELETE http://localhost:8083/connectors/ftp-source
```

### Using Confluent CLI

```bash
# Create
confluent local services connect connector load ftp-source --config config.json

# Update
confluent local services connect connector update ftp-source --config config.json

# Delete
confluent local services connect connector unload ftp-source
```

---

## Configuration Tips

1. **Always test in dev first** with smaller files
2. **Enable DLQ** for production to track errors
3. **Start with lenient validation** mode, then switch to strict
4. **Monitor metrics** and adjust buffer/poll settings
5. **Use retry** for unreliable networks
6. **Set meaningful topic names** for easier troubleshooting
7. **Document your validation rules** for future reference

---

## Next Steps

After configuration:
1. Deploy connector
2. Monitor logs
3. Check metrics
4. Validate output
5. Set up alerts
6. Document any customizations
