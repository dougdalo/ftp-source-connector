# Troubleshooting Guide - FTP/SFTP Source Connector v2.0

## 🔍 Quick Diagnosis

### Check Connector Status

```bash
# Get connector status
curl http://localhost:8083/connectors/ftp-source/status | jq

# Expected output:
{
  "name": "ftp-source",
  "connector": {
    "state": "RUNNING",
    "worker_id": "connect-1:8083"
  },
  "tasks": [{
    "id": 0,
    "state": "RUNNING",
    "worker_id": "connect-1:8083"
  }]
}
```

### Check Logs

```bash
# Tail logs
tail -f /var/log/kafka-connect/connect.log | grep FtpSource

# Search for errors
grep -i "error\|exception\|failed" /var/log/kafka-connect/connect.log | tail -20
```

---

## ❌ Common Problems & Solutions

### 1. Files Being Reprocessed After Restart

**Symptoms**:
- Same files processed multiple times
- Duplicate records in Kafka

**Diagnostic**:
```bash
# Check offset storage
kafka-console-consumer \
  --topic __consumer_offsets \
  --bootstrap-server localhost:9092 \
  --formatter "kafka.coordinator.group.GroupMetadataManager\$OffsetsMessageFormatter" \
  | grep ftp-source
```

**Causes & Solutions**:

#### Cause 1: Offset not persisted
```
ERROR Failed to commit offsets
```
**Solution**:
```json
{
  "offset.storage.topic": "__consumer_offsets",
  "offset.flush.interval.ms": "60000"
}
```

#### Cause 2: File changed (hash/size mismatch)
```
INFO File data.csv has changed (hash or size mismatch), processing from start
```
**Solution**: This is expected behavior. v2.0 detects file changes and reprocesses intentionally.

#### Cause 3: Migrated from v1.0
```
INFO Resuming file data.csv from line 0 (no previous offset found)
```
**Solution**: Normal for first run after v1.0→v2.0 migration. Will track properly after first complete run.

---

### 2. OutOfMemoryError

**Symptoms**:
```
Exception in thread "task-thread-ftp-source-0" java.lang.OutOfMemoryError: Java heap space
```

**Diagnostic**:
```bash
# Check heap usage
jstat -gcutil <pid> 1000

# Check connector config
curl http://localhost:8083/connectors/ftp-source/config | jq '.["ftp.max.records.per.poll"]'
```

**Solutions**:

#### Solution 1: Reduce records per poll
```json
{
  "ftp.max.records.per.poll": "500"  // Down from 1000
}
```

#### Solution 2: Reduce buffer size
```json
{
  "ftp.buffer.size.bytes": "8192"  // Down from 32768
}
```

#### Solution 3: Increase heap
```bash
# In connect-distributed.properties
export KAFKA_HEAP_OPTS="-Xms1G -Xmx2G"  // Up from 1G
```

#### Solution 4: Process files in smaller chunks
```json
{
  "ftp.file.pattern": "small_.*\\.txt"  // Process smaller files first
}
```

---

### 3. Connection Timeout / Network Errors

**Symptoms**:
```
ERROR Operation 'connect' failed after 3 attempts
ConnectException: Connection timed out
```

**Diagnostic**:
```bash
# Test FTP connection manually
ftp ftp.example.com
# or
sftp user@sftp.example.com

# Check network connectivity
telnet ftp.example.com 21
nc -zv ftp.example.com 22
```

**Solutions**:

#### Solution 1: Increase retry attempts
```json
{
  "ftp.retry.max.attempts": "5",
  "ftp.retry.max.backoff.ms": "60000"
}
```

#### Solution 2: Check firewall rules
```bash
# Allow FTP ports
sudo ufw allow 21/tcp
sudo ufw allow 22/tcp

# For passive FTP
sudo ufw allow 49152:65534/tcp
```

#### Solution 3: Use passive mode (FTP only)
```java
// Already enabled by default in FtpRemoteClient
ftpClient.enterLocalPassiveMode();
```

#### Solution 4: Verify credentials
```bash
# Test with curl
curl -u username:password ftp://ftp.example.com/
```

---

### 4. Validation Errors

**Symptoms**:
```
WARN Validation failed for line 123: Field 'age' validation 'range' failed: 
     Field must be between 0 and 150 (value='999')
```

**Diagnostic**:
```bash
# Check DLQ topic
kafka-console-consumer \
  --topic ftp-errors \
  --bootstrap-server localhost:9092 \
  --from-beginning \
  --property print.key=true
```

**Solutions**:

#### Solution 1: Review validation rules
```json
{
  // Check if rules match your data
  "ftp.validation.rules": "age:range(0,150)"  // Too restrictive?
}
```

#### Solution 2: Switch to lenient mode temporarily
```json
{
  "ftp.validation.mode": "lenient"  // Log warnings but don't skip
}
```

#### Solution 3: Fix data at source
- Contact data provider
- Update data generation process
- Add data cleansing step

#### Solution 4: Disable validation temporarily
```json
{
  "ftp.validation.enabled": "false"
}
```

---

### 5. DLQ Topic Not Found

**Symptoms**:
```
ERROR Failed to send record to DLQ
org.apache.kafka.common.errors.UnknownTopicOrPartitionException: 
     This server does not host this topic-partition
```

**Solution**:
```bash
# Create DLQ topic
kafka-topics --create \
  --topic ftp-errors \
  --partitions 3 \
  --replication-factor 3 \
  --bootstrap-server localhost:9092

# Verify
kafka-topics --describe --topic ftp-errors --bootstrap-server localhost:9092
```

---

### 6. Schema Evolution / Field Mismatch

**Symptoms**:
```
ERROR Failed to build record model
org.apache.kafka.connect.errors.DataException: 
     Field count mismatch: expected 5, got 7
```

**Diagnostic**:
```bash
# Check file structure
head -5 /path/to/problematic/file.csv

# Compare with headers config
curl http://localhost:8083/connectors/ftp-source/config | jq '.["ftp.file.headers"]'
```

**Solutions**:

#### Solution 1: Update headers to match file
```json
{
  "ftp.file.headers": "field1,field2,field3,field4,field5,field6,field7"
}
```

#### Solution 2: Let connector auto-name extra fields
```json
{
  "ftp.file.headers": "field1,field2,field3,field4,field5"
  // field6, field7 will be auto-named
}
```

#### Solution 3: Skip problematic files
```json
{
  "ftp.file.pattern": "valid_files_.*\\.csv"
}
```

---

### 7. Slow Processing / Performance Issues

**Symptoms**:
```
INFO Processed 10000 lines in 60000 ms (lines/sec: 166.67)
WARN Current file processing duration: 300000 ms
```

**Diagnostic**:
```bash
# Check metrics
grep "lines/sec" /var/log/kafka-connect/connect.log | tail -10

# Check system resources
top
iostat -x 1
```

**Solutions**:

#### Solution 1: Increase buffer size
```json
{
  "ftp.buffer.size.bytes": "131072"  // 128KB
}
```

#### Solution 2: Increase records per poll
```json
{
  "ftp.max.records.per.poll": "5000"  // Up from 1000
}
```

#### Solution 3: Disable unnecessary features
```json
{
  "ftp.validation.enabled": "false",
  "ftp.file.skip.empty.lines": "false"
}
```

#### Solution 4: Optimize validation rules
```json
{
  // Remove expensive regex patterns
  "ftp.validation.rules": "id:not_empty"  // Instead of complex patterns
}
```

#### Solution 5: Check network latency
```bash
# Ping FTP server
ping ftp.example.com

# If high latency, consider:
# - Moving connector closer to FTP server
# - Using faster network connection
```

---

### 8. File Stuck in Staging Directory

**Symptoms**:
- Files in `/staging` not moving to `/archive`
- Connector appears hung

**Diagnostic**:
```bash
# Check staging directory
ls -lh /staging/

# Check connector logs
grep "Deleting staged file" /var/log/kafka-connect/connect.log
```

**Solutions**:

#### Solution 1: Restart connector
```bash
curl -X POST http://localhost:8083/connectors/ftp-source/restart
```

#### Solution 2: Manual cleanup (if safe)
```bash
# ONLY if connector is stopped
rm /staging/*
```

#### Solution 3: Check file permissions
```bash
# Verify FTP user can delete from staging
ls -la /staging/
```

---

### 9. Compression Not Detected

**Symptoms**:
```
ERROR Failed to read file: invalid gzip header
```

**Solutions**:

#### Solution 1: Enable auto-detection
```json
{
  "ftp.file.compression.auto.detect": "true"
}
```

#### Solution 2: Verify file extension
```bash
# File must end with .gz
mv file.txt file.txt.gz
```

#### Solution 3: Test decompression manually
```bash
gunzip -t file.gz
```

---

### 10. Connector Won't Start

**Symptoms**:
```
ERROR Connector configuration is invalid
```

**Diagnostic**:
```bash
# Validate configuration
curl -X PUT http://localhost:8083/connector-plugins/FtpSourceConnectorEnhanced/config/validate \
  -H "Content-Type: application/json" \
  -d @config.json
```

**Common Configuration Errors**:

```json
// ERROR: Missing required field
{
  // Missing "topic"
}

// FIX:
{
  "topic": "ftp-data"
}

// ERROR: Invalid type
{
  "ftp.port": "twenty-two"  // Should be number
}

// FIX:
{
  "ftp.port": "22"
}

// ERROR: Invalid validation rule
{
  "ftp.validation.rules": "field:invalid_rule"
}

// FIX:
{
  "ftp.validation.rules": "field:not_empty"
}
```

---

## 🔧 Advanced Diagnostics

### Enable Debug Logging

```properties
# In connect-log4j.properties
log4j.logger.br.com.datastreambrasil.kafka.connector.ftp=DEBUG
```

```bash
# Restart connect
systemctl restart confluent-kafka-connect
```

### Thread Dump Analysis

```bash
# Get thread dump
jstack <pid> > thread_dump.txt

# Look for blocked threads
grep -A 10 "BLOCKED\|WAITING" thread_dump.txt
```

### Memory Analysis

```bash
# Heap dump
jmap -dump:format=b,file=heap.bin <pid>

# Analyze with MAT or VisualVM
```

---

## 📊 Performance Benchmarks

### Expected Performance

| File Size | Lines | Time | Lines/sec |
|-----------|-------|------|-----------|
| 1 MB | 10K | ~2s | ~5,000 |
| 10 MB | 100K | ~20s | ~5,000 |
| 100 MB | 1M | ~3-5min | ~4,000 |
| 1 GB | 10M | ~30-50min | ~3,500 |

**Note**: Actual performance varies based on:
- Network latency
- Validation enabled/disabled
- Field count
- Hardware specs

### Performance Tuning Matrix

| Use Case | max_records | buffer_size | Notes |
|----------|-------------|-------------|-------|
| Small files | 5000 | 8192 | Fast turnaround |
| Large files | 500 | 65536 | Memory efficient |
| Many fields | 1000 | 32768 | Balanced |
| Simple data | 10000 | 131072 | Maximum throughput |

---

## 🆘 Getting Help

### Information to Provide

When reporting issues, include:

1. **Connector version**
```bash
curl http://localhost:8083/connectors/ftp-source | jq '.version'
```

2. **Full configuration** (redact passwords)
```bash
curl http://localhost:8083/connectors/ftp-source/config | jq
```

3. **Error logs** (last 50 lines)
```bash
grep ERROR /var/log/kafka-connect/connect.log | tail -50
```

4. **File sample** (first 10 lines)
```bash
head -10 problematic_file.csv
```

5. **Environment**
- Kafka version
- Java version
- OS version
- Network topology

---

## ✅ Health Check Checklist

Run this checklist regularly:

```bash
# 1. Connector running
curl http://localhost:8083/connectors/ftp-source/status | jq '.connector.state'
# Expected: "RUNNING"

# 2. No errors in logs
grep -i error /var/log/kafka-connect/connect.log | tail -5
# Expected: No recent errors

# 3. Files being processed
grep "Finished processing file" /var/log/kafka-connect/connect.log | tail -1
# Expected: Recent timestamp

# 4. Offset committing
grep "commit offsets" /var/log/kafka-connect/connect.log | tail -1
# Expected: Recent successful commits

# 5. No files stuck in staging
ls /staging/ | wc -l
# Expected: 0 or low number

# 6. DLQ not filling up (if enabled)
kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic ftp-errors | tail -1
# Expected: Stable or low offset
```

---

**Still stuck?** Open a GitHub issue with the information template above!
