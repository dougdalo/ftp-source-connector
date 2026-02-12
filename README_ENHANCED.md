# FTP/SFTP Source Connector for Kafka Connect - ENHANCED v2.0

This **enhanced** Kafka Connect Source Connector allows ingestion of flat files (e.g., `.txt`, `.csv`) from remote FTP or SFTP servers into Apache Kafka with advanced features including:

- ✅ **Offset Management** - Resume processing from exact line after failures
- ✅ **Retry with Exponential Backoff** - Automatic retry for transient failures
- ✅ **Data Validation** - Configurable validation rules with DLQ support
- ✅ **Dead Letter Queue (DLQ)** - Send invalid records to separate topic
- ✅ **Schema Caching** - Optimized performance for large files
- ✅ **Compression Support** - Auto-detect and handle .gz files
- ✅ **Performance Metrics** - Detailed processing metrics and monitoring
- ✅ **Enhanced File Handling** - Skip headers, footers, empty lines, and comments

---

## 🎯 Key Features

### 1. **Offset Management**
- Tracks exact line number and file hash
- Automatically resumes from last processed line after restart
- Detects file changes and reprocesses if needed

### 2. **Retry & Resilience**
- Exponential backoff retry for network failures
- Configurable max attempts and backoff times
- Automatic recovery from transient errors

### 3. **Data Validation**
- Built-in validation rules: `not_empty`, `numeric`, `integer`, `email`, `date`, `range`, `pattern`
- Custom validation rules via configuration
- Strict or lenient validation modes

### 4. **Dead Letter Queue**
- Automatically send invalid/failed records to DLQ topic
- Includes original line, error type, error message, and metadata
- Enables error analysis and reprocessing

### 5. **Performance Optimizations**
- Schema caching to avoid repeated schema creation
- Configurable buffer size
- Auto-detect and handle compressed files (.gz)
- Processing metrics and monitoring

---

## 📋 Requirements

- **Build**: Java 11
- **Runtime**: Kafka Connect 2.6+
- **Protocols**: FTP, SFTP

---

## 🚀 Quick Start

### Basic Configuration

```json
{
  "name": "ftp-source-enhanced",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    "ftp.protocol": "sftp",
    "ftp.host": "ftp.example.com",
    "ftp.port": "22",
    "ftp.username": "username",
    "ftp.password": "password",
    "ftp.directory": "/input",
    "ftp.directory.stage": "/staging",
    "ftp.directory.archive": "/archive",
    "ftp.file.pattern": ".*\\.txt",
    "topic": "ftp-data-topic",
    "tasks.max": "1"
  }
}
```

### Advanced Configuration with All Features

```json
{
  "name": "ftp-source-enhanced-full",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced",
    
    "ftp.protocol": "sftp",
    "ftp.host": "ftp.example.com",
    "ftp.port": "22",
    "ftp.username": "username",
    "ftp.password": "password",
    
    "ftp.directory": "/input",
    "ftp.directory.stage": "/staging",
    "ftp.directory.archive": "/archive",
    "ftp.file.pattern": ".*\\.csv",
    
    "ftp.file.encoding": "UTF-8",
    "ftp.file.output.format": "json",
    "ftp.file.tokenizer": ";",
    "ftp.file.headers": "type,date,time,code,value",
    "ftp.kafka.key.field": "type+code",
    
    "ftp.file.skip.header.lines": "1",
    "ftp.file.skip.footer.lines": "0",
    "ftp.file.empty.lines.skip": "true",
    "ftp.file.comment.prefix": "#",
    
    "ftp.validation.enabled": "true",
    "ftp.validation.rules": "type:not_empty,code:numeric,value:range(0,1000)",
    "ftp.validation.mode": "strict",
    
    "ftp.dlq.enabled": "true",
    "ftp.dlq.topic": "ftp-errors",
    
    "ftp.retry.max.attempts": "3",
    "ftp.retry.backoff.ms": "1000",
    "ftp.retry.max.backoff.ms": "30000",
    
    "ftp.buffer.size.bytes": "32768",
    "ftp.file.compression.auto.detect": "true",
    "ftp.max.records.per.poll": "1000",
    "ftp.poll.interval.ms": "10000",
    "ftp.metrics.interval.lines": "10000",
    
    "topic": "ftp-data-topic",
    "tasks.max": "1"
  }
}
```

---

## ⚙️ Configuration Reference

### Connection Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ftp.protocol` | String | - | Protocol: `ftp` or `sftp` |
| `ftp.host` | String | - | FTP/SFTP server hostname |
| `ftp.port` | Int | 21 (FTP) / 22 (SFTP) | Server port |
| `ftp.username` | String | - | Authentication username |
| `ftp.password` | Password | - | Authentication password |

### Directory Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ftp.directory` | String | - | Input directory path |
| `ftp.directory.stage` | String | - | Staging directory path |
| `ftp.directory.archive` | String | - | Archive directory path |
| `ftp.file.pattern` | String | `.*\.txt` | Regex pattern for file matching |

### File Processing

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ftp.file.encoding` | String | `UTF-8` | File character encoding |
| `ftp.file.output.format` | String | `string` | Output format: `string` or `json` |
| `ftp.file.tokenizer` | String | `;` | Field delimiter for JSON format |
| `ftp.file.headers` | String | - | Comma-separated field names |
| `ftp.file.skip.header.lines` | Int | `0` | Skip N header lines |
| `ftp.file.skip.footer.lines` | Int | `0` | Skip N footer lines |
| `ftp.file.empty.lines.skip` | Boolean | `true` | Skip empty lines |
| `ftp.file.comment.prefix` | String | - | Skip lines with this prefix |

### 🆕 Validation Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ftp.validation.enabled` | Boolean | `false` | Enable data validation |
| `ftp.validation.rules` | String | - | Validation rules (see below) |
| `ftp.validation.mode` | String | `strict` | `strict` or `lenient` |

#### Validation Rules Format

```
field1:rule1,field2:rule2(param1,param2)
```

**Available Rules:**
- `not_empty` - Field must not be empty
- `numeric` - Field must be numeric (int or decimal)
- `integer` - Field must be integer
- `email` - Field must be valid email
- `date(pattern)` - Field must match date pattern (e.g., `date(yyyyMMdd)`)
- `length_min(n)` - Minimum length
- `length_max(n)` - Maximum length
- `range(min,max)` - Numeric range
- `pattern(regex)` - Match regex pattern

**Examples:**
```
"ftp.validation.rules": "email:email,age:range(18,100),zipcode:pattern(\\d{5})"
```

### 🆕 Dead Letter Queue

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ftp.dlq.enabled` | Boolean | `false` | Enable DLQ |
| `ftp.dlq.topic` | String | `<topic>.dlq` | DLQ topic name |

### 🆕 Retry Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ftp.retry.max.attempts` | Int | `3` | Max retry attempts |
| `ftp.retry.backoff.ms` | Long | `1000` | Initial backoff (ms) |
| `ftp.retry.max.backoff.ms` | Long | `30000` | Max backoff (ms) |

### Performance Tuning

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ftp.buffer.size.bytes` | Int | `32768` | Read buffer size (32KB) |
| `ftp.file.compression.auto.detect` | Boolean | `true` | Auto-handle .gz files |
| `ftp.max.records.per.poll` | Int | `1000` | Max records per poll |
| `ftp.poll.interval.ms` | Int | `10000` | Poll interval |
| `ftp.metrics.interval.lines` | Int | `10000` | Log metrics every N lines |

---

## 📊 Output Examples

### JSON Output with Validation

**Input file (`data.csv`):**
```
WB;20250217;1754;284;255
WB;20250217;1755;invalid;300
```

**Configuration:**
```json
{
  "ftp.file.output.format": "json",
  "ftp.file.tokenizer": ";",
  "ftp.file.headers": "type,date,time,code,value",
  "ftp.kafka.key.field": "type+code",
  "ftp.validation.enabled": "true",
  "ftp.validation.rules": "type:not_empty,code:numeric,value:range(0,1000)",
  "ftp.validation.mode": "strict",
  "ftp.dlq.enabled": "true"
}
```

**Output to main topic:**
```json
{
  "key": "WB_284",
  "payload": {
    "type": "WB",
    "date": "20250217",
    "time": "1754",
    "code": "284",
    "value": "255"
  }
}
```

**Output to DLQ topic (for invalid record):**
```json
{
  "key": "data.csv:2",
  "payload": {
    "original_line": "WB;20250217;1755;invalid;300",
    "error_type": "VALIDATION_ERROR",
    "error_message": "Field 'code' validation 'numeric' failed: Field must be numeric (value='invalid')",
    "source_file": "data.csv",
    "line_number": 2,
    "timestamp": "2025-02-11T10:30:45.123Z"
  }
}
```

---

## 🔍 Monitoring & Metrics

### Log Output

The connector provides detailed structured logging:

```
INFO  Connected to SFTP ftp.example.com server in 234 ms with retry support (max attempts: 3)
INFO  Polled 3 files from directory: /input in 45 ms
INFO  Staging file: /input/data.csv → /staging/data.csv in 123 ms
INFO  Streamed file: /staging/data.csv in 89 ms
INFO  Resuming file data.csv from line 5000 (previous offset found)
INFO  Processed 10000 lines (skipped 5) from data.csv in 2345 ms (row read avg 0 ms max 12 ms, lines/sec: 4265.91)
INFO  Finished processing file data.csv with 50000 lines (skipped 20) in 12340 ms (lines/sec: 4051.86)
INFO  Summary file written: /archive/data_20250211_103045123.txt in 45 ms
```

### Metrics Available

The connector tracks:
- Total files processed
- Total lines processed  
- Total bytes processed
- Total errors
- Total validation errors
- Current file processing time
- Lines per second
- Average/max line read time

---

## 🛠️ Troubleshooting

### Problem: Files being reprocessed after restart

**Symptoms**: Same files are read multiple times  
**Cause**: Offset not persisted or file changed  
**Solution**:
1. Check offset.storage.topic configuration
2. Verify logs for offset commit errors
3. File hash changed = intentional reprocessing

### Problem: OutOfMemoryError

**Symptoms**: Connector crashes with OOM  
**Cause**: File too large or buffer too high  
**Solution**:
```json
{
  "ftp.max.records.per.poll": "500",
  "ftp.buffer.size.bytes": "8192"
}
```

### Problem: Validation errors

**Symptoms**: Records going to DLQ  
**Cause**: Data doesn't match validation rules  
**Solution**:
1. Check DLQ topic for error details
2. Adjust validation rules if needed
3. Use `"ftp.validation.mode": "lenient"` to log warnings instead of failing

### Problem: Connection timeouts

**Symptoms**: Retry attempts exhausted  
**Cause**: Network issues or server unavailable  
**Solution**:
```json
{
  "ftp.retry.max.attempts": "5",
  "ftp.retry.max.backoff.ms": "60000"
}
```

---

## 📈 Performance Tuning Guide

### Small Files (<1MB)

```json
{
  "ftp.max.records.per.poll": "5000",
  "ftp.buffer.size.bytes": "8192",
  "ftp.poll.interval.ms": "5000"
}
```

### Large Files (>100MB)

```json
{
  "ftp.max.records.per.poll": "500",
  "ftp.buffer.size.bytes": "65536",
  "ftp.poll.interval.ms": "30000"
}
```

### High Throughput

```json
{
  "ftp.max.records.per.poll": "10000",
  "ftp.buffer.size.bytes": "131072",
  "ftp.validation.enabled": "false",
  "tasks.max": "1"
}
```

---

## 🔄 Migration from v1.0

### Configuration Changes

**v1.0:**
```json
{
  "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnector"
}
```

**v2.0 (Enhanced):**
```json
{
  "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced"
}
```

All existing configurations are **backward compatible**. New features are opt-in via configuration.

---

## 🧪 Example Use Cases

### Use Case 1: CSV with Validation and DLQ

```json
{
  "ftp.file.output.format": "json",
  "ftp.file.tokenizer": ",",
  "ftp.file.headers": "id,name,email,age",
  "ftp.validation.enabled": "true",
  "ftp.validation.rules": "id:integer,email:email,age:range(0,150)",
  "ftp.dlq.enabled": "true"
}
```

### Use Case 2: Log Files with Comments

```json
{
  "ftp.file.skip.header.lines": "2",
  "ftp.file.comment.prefix": "#",
  "ftp.file.empty.lines.skip": "true",
  "ftp.file.output.format": "string"
}
```

### Use Case 3: Compressed Files with Retry

```json
{
  "ftp.file.pattern": ".*\\.gz",
  "ftp.file.compression.auto.detect": "true",
  "ftp.retry.max.attempts": "5",
  "ftp.retry.backoff.ms": "2000"
}
```

---

## 📦 Building

```bash
mvn clean package
```

The JAR with dependencies will be in:
```
target/ftp-source-connector-2.0.0-jar-with-dependencies.jar
```

---

## 📝 License

Apache License 2.0

---

## 🆕 What's New in v2.0

- ✅ **Offset Management** - Resume from exact line
- ✅ **Retry with Backoff** - Automatic retry for failures
- ✅ **Data Validation** - 8 built-in validation rules
- ✅ **Dead Letter Queue** - Error handling and analysis
- ✅ **Schema Caching** - Performance optimization
- ✅ **Compression Support** - Auto-detect .gz files
- ✅ **Enhanced Logging** - Structured logs with metrics
- ✅ **File Skipping** - Headers, footers, empty lines, comments
- ✅ **Processing Metrics** - Lines/sec, avg/max read time

---

## 🤝 Contributing

Contributions are welcome! Please open issues or pull requests.

---

## 📧 Support

For issues and questions, please open a GitHub issue.
