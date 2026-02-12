# Migration Guide: v1.0 → v2.0 Enhanced

## 📋 Overview

This guide helps you migrate from FTP Source Connector v1.0 to v2.0 Enhanced with minimal disruption.

**Good News**: v2.0 is **100% backward compatible** with v1.0 configurations! 🎉

---

## 🚀 Quick Migration (5 minutes)

### Step 1: Update Connector Class

**Before (v1.0):**
```json
{
  "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnector"
}
```

**After (v2.0):**
```json
{
  "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnectorEnhanced"
}
```

### Step 2: Deploy New JAR

```bash
# Stop the connector
curl -X DELETE http://localhost:8083/connectors/ftp-source

# Replace JAR file
cp ftp-source-connector-2.0.0-jar-with-dependencies.jar \
   /usr/share/java/kafka-connect-ftp/

# Restart Kafka Connect worker
systemctl restart confluent-kafka-connect

# Recreate connector with new class
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @connector-config.json
```

### Step 3: Verify

```bash
# Check connector status
curl http://localhost:8083/connectors/ftp-source/status

# Monitor logs
tail -f /var/log/kafka-connect/connect.log
```

**Done!** Your connector is now running v2.0 with all existing functionality intact.

---

## 🎯 Enabling New Features (Recommended)

After basic migration, gradually enable new features:

### Phase 1: Enable Retry (Recommended for Production)

Add to your configuration:

```json
{
  "ftp.retry.max.attempts": "3",
  "ftp.retry.backoff.ms": "1000",
  "ftp.retry.max.backoff.ms": "30000"
}
```

**Benefits**:
- Automatic recovery from transient network issues
- No manual intervention needed for temporary failures
- Configurable retry behavior

**Test**: Temporarily disconnect network and verify retry in logs:
```
WARN Operation 'listFiles' failed (attempt 1/3), retrying in 1000ms
INFO Operation 'listFiles' succeeded after 2 attempt(s)
```

---

### Phase 2: Enable Data Validation (Optional)

If you need data quality guarantees:

```json
{
  "ftp.validation.enabled": "true",
  "ftp.validation.rules": "field1:not_empty,field2:numeric",
  "ftp.validation.mode": "lenient"
}
```

**Start with `lenient` mode** to observe validation errors without blocking:
```
WARN Validation failed for line 123: Field 'field2' validation 'numeric' failed
```

**Switch to `strict` mode** once confident:
```json
{
  "ftp.validation.mode": "strict"
}
```

---

### Phase 3: Enable Dead Letter Queue (Production Best Practice)

Capture and analyze failed records:

```json
{
  "ftp.dlq.enabled": "true",
  "ftp.dlq.topic": "ftp-errors"
}
```

**Create DLQ topic first**:
```bash
kafka-topics --create \
  --topic ftp-errors \
  --partitions 3 \
  --replication-factor 3 \
  --bootstrap-server localhost:9092
```

**Monitor DLQ**:
```bash
kafka-console-consumer \
  --topic ftp-errors \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

---

### Phase 4: Optimize Performance

Based on your file characteristics:

**For Large Files (>100MB)**:
```json
{
  "ftp.max.records.per.poll": "500",
  "ftp.buffer.size.bytes": "65536",
  "ftp.metrics.interval.lines": "50000"
}
```

**For Many Small Files**:
```json
{
  "ftp.max.records.per.poll": "5000",
  "ftp.buffer.size.bytes": "8192",
  "ftp.poll.interval.ms": "5000"
}
```

**For Compressed Files**:
```json
{
  "ftp.file.compression.auto.detect": "true"
}
```

---

## 📊 Configuration Mapping

### Unchanged Configurations (No Action Needed)

These work exactly the same in v2.0:

| Configuration | v1.0 | v2.0 | Notes |
|--------------|------|------|-------|
| `ftp.protocol` | ✅ | ✅ | Same |
| `ftp.host` | ✅ | ✅ | Same |
| `ftp.port` | ✅ | ✅ | Same |
| `ftp.username` | ✅ | ✅ | Same |
| `ftp.password` | ✅ | ✅ | Same |
| `ftp.directory` | ✅ | ✅ | Same |
| `ftp.directory.stage` | ✅ | ✅ | Same |
| `ftp.directory.archive` | ✅ | ✅ | Same |
| `ftp.file.pattern` | ✅ | ✅ | Same |
| `ftp.file.encoding` | ✅ | ✅ | Same |
| `ftp.file.output.format` | ✅ | ✅ | Same |
| `ftp.file.tokenizer` | ✅ | ✅ | Same |
| `ftp.file.headers` | ✅ | ✅ | Same |
| `ftp.kafka.key.field` | ✅ | ✅ | Same |
| `ftp.poll.interval.ms` | ✅ | ✅ | Same |
| `ftp.max.records.per.poll` | ✅ | ✅ | Same |
| `topic` | ✅ | ✅ | Same |
| `tasks.max` | ✅ | ✅ | Same |

### New Configurations (Optional in v2.0)

| Configuration | Default | When to Use |
|--------------|---------|-------------|
| `ftp.retry.max.attempts` | 3 | Production (recommended) |
| `ftp.retry.backoff.ms` | 1000 | Customize retry timing |
| `ftp.validation.enabled` | false | Data quality requirements |
| `ftp.validation.rules` | "" | Define validation rules |
| `ftp.dlq.enabled` | false | Error tracking needed |
| `ftp.buffer.size.bytes` | 32768 | Performance tuning |
| `ftp.file.skip.header.lines` | 0 | CSV files with headers |
| `ftp.file.skip.empty.lines` | true | Clean data processing |

---

## 🔄 Offset Management Changes

### v1.0 Behavior

```java
// Old: Used timestamp (problematic)
Map<String, Long> sourceOffset = Collections.singletonMap("position", System.currentTimeMillis());
```

**Problem**: Reprocesses entire file after restart

### v2.0 Behavior

```java
// New: Tracks exact position
Map<String, Object> sourceOffset = new HashMap<>();
sourceOffset.put("filename", currentFilename);
sourceOffset.put("file_hash", fileHash);
sourceOffset.put("line_number", linesProcessed);
sourceOffset.put("file_size", fileSize);
```

**Benefit**: Resumes from exact line after restart

### Migration Impact

**First run with v2.0**: May reprocess files if offset format changed  
**Subsequent runs**: Proper resume from last line

**To force reprocessing** (if needed):
```bash
# Reset offsets
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group connect-ftp-source \
  --topic __consumer_offsets \
  --reset-offsets --to-earliest --execute
```

---

## 📈 Performance Comparison

### Before (v1.0)

```
INFO Processed 10000 lines from file.csv in 5432 ms
INFO Finished processing file.csv with 100000 lines
```

### After (v2.0)

```
INFO Processed 10000 lines (skipped 2) from file.csv in 2156 ms 
     (row read avg 0 ms max 5 ms, lines/sec: 4638.52)
INFO Finished processing file.csv with 100000 lines (skipped 20) 
     in 21560 ms (lines/sec: 4638.52)
INFO Final metrics: ProcessingMetrics{totalFilesProcessed=1, 
     totalLinesProcessed=100000, totalBytesProcessed=15000000, 
     totalErrors=0, totalValidationErrors=0}
```

**Improvements**:
- ✅ Detailed timing metrics
- ✅ Lines per second tracking
- ✅ Skipped lines count
- ✅ Error counters
- ✅ Validation statistics

---

## 🧪 Testing Migration

### Test Plan

1. **Deploy to Dev Environment**
```bash
# Use same configuration as production
# Enable lenient validation mode
"ftp.validation.mode": "lenient"
```

2. **Run Side-by-Side Comparison**
```bash
# Run v1.0 connector as "ftp-source-v1"
# Run v2.0 connector as "ftp-source-v2"
# Compare outputs
```

3. **Verify Offset Resume**
```bash
# Process half a file
# Stop connector
# Restart connector
# Verify it resumes from middle (not start)
```

4. **Test Retry Behavior**
```bash
# Temporarily block FTP access
# Verify retry attempts in logs
# Restore access
# Verify successful recovery
```

5. **Validate DLQ**
```bash
# Send file with known bad data
# Verify records in DLQ topic
# Check error messages are clear
```

---

## 🚨 Rollback Plan

If you need to rollback to v1.0:

### Step 1: Stop v2.0 Connector
```bash
curl -X DELETE http://localhost:8083/connectors/ftp-source
```

### Step 2: Restore v1.0 JAR
```bash
cp ftp-source-connector-1.0.1-jar-with-dependencies.jar \
   /usr/share/java/kafka-connect-ftp/
```

### Step 3: Update Configuration
```json
{
  "connector.class": "br.com.datastreambrasil.kafka.connector.ftp.FtpSourceConnector",
  // Remove v2.0-specific configs:
  // - ftp.retry.*
  // - ftp.validation.*
  // - ftp.dlq.*
  // Keep all original configs
}
```

### Step 4: Restart
```bash
systemctl restart confluent-kafka-connect
curl -X POST http://localhost:8083/connectors -d @connector-config-v1.json
```

**Note**: Files may be reprocessed after rollback due to offset format differences.

---

## 📞 Support & Help

### Common Migration Issues

**Issue**: Connector fails to start  
**Solution**: Check logs for configuration errors, verify class name

**Issue**: Files being reprocessed  
**Solution**: Normal for first v2.0 run, will track properly after

**Issue**: DLQ topic not found  
**Solution**: Create topic before enabling DLQ

**Issue**: Validation rejecting all records  
**Solution**: Start with `lenient` mode, adjust rules

---

## ✅ Post-Migration Checklist

- [ ] v2.0 connector deployed successfully
- [ ] All files processing correctly
- [ ] Retry behavior tested (if enabled)
- [ ] Validation rules working (if enabled)
- [ ] DLQ receiving errors (if enabled)
- [ ] Performance metrics reviewed
- [ ] Monitoring dashboards updated
- [ ] Team trained on new features
- [ ] Documentation updated
- [ ] Rollback plan tested

---

## 🎓 Training Resources

### For Operators

- Monitor new metrics in logs
- Understand retry behavior
- How to analyze DLQ records
- Performance tuning guidelines

### For Developers

- New configuration options
- Validation rule syntax
- DLQ record structure
- Offset management improvements

---

## 📅 Recommended Timeline

**Week 1: Planning**
- Review this guide
- Plan test scenarios
- Prepare rollback procedures

**Week 2: Dev/Test**
- Deploy to dev environment
- Run tests
- Tune configurations

**Week 3: Staging**
- Deploy to staging
- Run with production data volume
- Performance testing

**Week 4: Production**
- Deploy during maintenance window
- Monitor closely for 24h
- Gradually enable new features

---

**Questions?** Open an issue on GitHub or contact the team.

**Happy Migrating! 🚀**
