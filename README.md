# FTP/SFTP Source Connector for Kafka Connect

A Kafka Connect source connector that ingests delimited flat files (`.txt`, `.csv`) from FTP/SFTP servers into Apache Kafka. Each line becomes a Kafka record -- either as a raw string or as a structured JSON message with schema.

Built for batch-file integration patterns where upstream systems drop files on an FTP server and downstream consumers need that data in Kafka.

## How It Works

```
FTP/SFTP Server                  Connector                         Kafka
 /input/
   WB1.txt  ──►  move to /staging/  ──►  read line-by-line  ──►  topic: ftp-data
   WB2.txt       parse & validate        build SourceRecord       (one record per line)
                 move to /archive/       apply key strategy
                 write summary file      emit to Kafka
```

1. The connector polls the input directory on a configurable interval.
2. Matching files are moved to a staging directory (prevents double-processing by other tasks).
3. Each file is read line-by-line, parsed using the configured delimiter, and emitted as Kafka records.
4. After processing, the file is archived and a summary report is written.
5. On restart, offset tracking (file hash + line number) enables exact resume from the last processed line.

## Quick Start

### Build

```bash
mvn clean package
```

This produces `target/ftp-source-connector-1.1.3-jar-with-dependencies.jar`.

### Deploy

Copy the JAR into your Kafka Connect plugin path:

```bash
cp target/ftp-source-connector-*-jar-with-dependencies.jar \
   /usr/share/java/kafka-connect-ftp/
```

Restart the Kafka Connect worker, then create the connector:

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ftp-source",
    "config": {
      "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnector",
      "ftp.protocol": "sftp",
      "ftp.host": "ftp.example.com",
      "ftp.port": "22",
      "ftp.username": "svc-kafka",
      "ftp.password": "secret",
      "ftp.directory": "/input",
      "ftp.directory.stage": "/staging",
      "ftp.directory.archive": "/archive",
      "ftp.file.pattern": ".*\\.txt",
      "ftp.file.output.format": "json",
      "ftp.file.tokenizer": ";",
      "ftp.file.headers": "type,date,time,code,value",
      "ftp.kafka.key.field": "type+code",
      "topic": "ftp-data",
      "tasks.max": "1"
    }
  }'
```

## Output Formats

### String mode (default)

Each line is emitted as-is. No parsing, no schema.

```
ftp.file.output.format=string
```

### JSON mode

Lines are split by the delimiter and mapped to named fields. The connector produces a Kafka Connect `Struct` with a schema, which means downstream connectors (JDBC Sink, Elasticsearch Sink, etc.) can use the schema directly.

Given this file content and `ftp.file.headers=type,date,time,code,value`:

```
WB;20250217;1754;284;255
```

The connector produces:

```json
{
  "schema": {
    "type": "struct",
    "fields": [
      { "field": "type", "type": "string" },
      { "field": "date", "type": "string" },
      { "field": "time", "type": "string" },
      { "field": "code", "type": "string" },
      { "field": "value", "type": "string" }
    ]
  },
  "payload": {
    "type": "WB",
    "date": "20250217",
    "time": "1754",
    "code": "284",
    "value": "255"
  }
}
```

If a line has more columns than headers, extra fields are named `field6`, `field7`, etc.

### Kafka Key Composition

Set `ftp.kafka.key.field` to control record keys. Use `+` for composite keys:

| Config | Input line | Resulting key |
|--------|-----------|---------------|
| `code` | `WB;20250217;1754;284;255` | `284` |
| `type+code` | `WB;20250217;1754;284;255` | `WB_284` |

Missing fields are silently omitted from the key.

## Configuration Reference

### Connection

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `ftp.protocol` | yes | -- | `ftp` or `sftp` |
| `ftp.host` | yes | -- | Server hostname or IP |
| `ftp.port` | no | 21 (ftp) / 22 (sftp) | Server port |
| `ftp.username` | yes | -- | Auth username |
| `ftp.password` | yes | -- | Auth password |

### Directories

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `ftp.directory` | yes | -- | Input directory to poll for files |
| `ftp.directory.stage` | yes | -- | Staging directory (files move here during processing) |
| `ftp.directory.archive` | yes | -- | Archive directory (files move here after processing) |

### File Processing

| Property | Default | Description |
|----------|---------|-------------|
| `ftp.file.pattern` | `.*\.txt` | Regex to match filenames |
| `ftp.file.encoding` | `UTF-8` | Character encoding |
| `ftp.file.output.format` | `string` | `string` or `json` |
| `ftp.file.tokenizer` | `;` | Delimiter for splitting lines in JSON mode |
| `ftp.file.headers` | *(empty)* | Comma-separated field names for JSON output |
| `ftp.file.skip.header.lines` | `0` | Number of header lines to skip |
| `ftp.file.skip.footer.lines` | `0` | Number of footer lines to skip |
| `ftp.file.empty.lines.skip` | `true` | Skip blank lines |
| `ftp.file.comment.prefix` | *(empty)* | Skip lines starting with this prefix (e.g. `#`) |
| `ftp.file.compression.auto.detect` | `true` | Auto-handle `.gz` files |

### Kafka

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `topic` | yes | -- | Target Kafka topic |
| `ftp.kafka.key.field` | no | *(empty)* | Field(s) for message key; use `+` for composite |

### Performance

| Property | Default | Description |
|----------|---------|-------------|
| `ftp.poll.interval.ms` | `10000` | How often to check for new files (ms) |
| `ftp.max.records.per.poll` | `1000` | Max records returned per `poll()` call |
| `ftp.buffer.size.bytes` | `32768` | Read buffer size |

### Retry

| Property | Default | Description |
|----------|---------|-------------|
| `ftp.retry.max.attempts` | `3` | Max retry attempts on transient FTP/SFTP failures |
| `ftp.retry.backoff.ms` | `1000` | Initial backoff (ms) |
| `ftp.retry.max.backoff.ms` | `30000` | Max backoff ceiling (ms); backoff doubles each attempt |

### Validation

Enable field-level validation when using JSON output mode. Invalid records are either skipped (strict) or passed through with a warning (lenient).

| Property | Default | Description |
|----------|---------|-------------|
| `ftp.validation.enabled` | `false` | Enable validation |
| `ftp.validation.mode` | `strict` | `strict` = skip invalid records, `lenient` = log and pass through |
| `ftp.validation.rules` | *(empty)* | Rule definitions (see below) |

**Rule syntax:** `field1:rule1,field2:rule2,...`

Available rules: `not_empty`, `numeric`, `integer`, `email`, `date(yyyy-MM-dd)`, `length_min(n)`, `length_max(n)`, `range(min,max)`, `pattern(regex)`

Example:
```
ftp.validation.rules=code:not_empty,value:numeric,value:range(0,1000)
```

### Dead Letter Queue

Route invalid or failed records to a separate topic for inspection and reprocessing.

| Property | Default | Description |
|----------|---------|-------------|
| `ftp.dlq.enabled` | `false` | Enable DLQ |
| `ftp.dlq.topic` | `<topic>.dlq` | DLQ topic name |

DLQ records include the original line, error type, error message, source filename, line number, and timestamp.

### Metrics

| Property | Default | Description |
|----------|---------|-------------|
| `ftp.metrics.interval.lines` | `10000` | Log processing metrics every N lines |

Metrics include files processed, lines read, bytes transferred, errors, validation failures, and throughput (lines/sec).

## Architecture

```
FtpSourceConnector                  (entrypoint, defines config, creates tasks)
  └─ FtpSourceTaskEnhanced          (poll loop: list → stage → read → parse → emit → archive)
       ├─ RemoteClient              (interface)
       │    ├─ FtpRemoteClient      (Apache Commons Net)
       │    └─ SftpRemoteClient     (Apache SSHD)
       ├─ RetryableRemoteClient     (decorator: exponential backoff over any RemoteClient)
       ├─ ConfigurableValidator     (field-level validation with built-in rules)
       ├─ FileOffset                (offset tracking: file hash + line number for resume)
       └─ ProcessingMetrics         (throughput and error counters)
```

Key design decisions:

- **Decorator pattern for retry** -- `RetryableRemoteClient` wraps any `RemoteClient`, so retry logic is orthogonal to protocol implementation.
- **Stage-then-process** -- Files are moved to a staging directory before reading. This prevents other connector tasks (or external processes) from picking up the same file.
- **Hash-based offset tracking** -- `FileOffset` stores the MD5 hash of the file alongside the line offset. If a file changes between restarts, the connector detects the mismatch and reprocesses from the beginning.
- **Schema caching** -- In JSON mode the schema is built once per file and reused for every line, avoiding repeated allocation.

## Production Configuration Example

A realistic config for processing semicolon-delimited CSV files over SFTP with validation and DLQ:

```json
{
  "name": "payments-ingest",
  "config": {
    "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnector",
    "ftp.protocol": "sftp",
    "ftp.host": "sftp.payments.internal",
    "ftp.port": "22",
    "ftp.username": "svc-kafka-connect",
    "ftp.password": "${file:/opt/kafka-connect/secrets.properties:sftp.password}",
    "ftp.directory": "/drop/payments",
    "ftp.directory.stage": "/processing/payments",
    "ftp.directory.archive": "/archive/payments",
    "ftp.file.pattern": "PAY_.*\\.csv",
    "ftp.file.encoding": "UTF-8",
    "ftp.file.output.format": "json",
    "ftp.file.tokenizer": ";",
    "ftp.file.headers": "tx_id,amount,currency,account,timestamp",
    "ftp.file.skip.header.lines": "1",
    "ftp.kafka.key.field": "tx_id",
    "ftp.poll.interval.ms": "30000",
    "ftp.max.records.per.poll": "5000",
    "ftp.retry.max.attempts": "5",
    "ftp.retry.backoff.ms": "2000",
    "ftp.retry.max.backoff.ms": "60000",
    "ftp.validation.enabled": "true",
    "ftp.validation.rules": "tx_id:not_empty,amount:numeric,currency:length_max(3)",
    "ftp.validation.mode": "strict",
    "ftp.dlq.enabled": "true",
    "ftp.dlq.topic": "payments.dlq",
    "ftp.metrics.interval.lines": "50000",
    "topic": "payments.raw",
    "tasks.max": "1"
  }
}
```

## Migration from v1.0

v2.0 is **100% backward compatible**. Existing v1.0 configurations work without changes -- the `FtpSourceConnector` class is still the entrypoint and delegates to the enhanced implementation internally.

To adopt v2.0 features, add the new configuration properties incrementally. See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) for a phased adoption plan.

## Requirements

- Java 11+
- Apache Kafka Connect (tested with Kafka 3.x)

## Running Tests

```bash
mvn test
```

Coverage report (JaCoCo):

```bash
mvn verify
# Report at target/site/jacoco/index.html
```

## Further Documentation

- [CHANGELOG.md](CHANGELOG.md) -- Version history and release notes
- [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) -- v1.0 to v2.0 migration steps
- [CONFIGURATION_EXAMPLES.md](CONFIGURATION_EXAMPLES.md) -- 12 real-world configuration recipes
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) -- Common issues and diagnostic procedures

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

Uses Apache Kafka, Apache Commons Net, and Apache MINA SSHD.
