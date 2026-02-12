# Changelog

All notable changes to the FTP/SFTP Source Connector will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2025-02-11

### 🎉 Major Release - Enhanced Features

This release introduces significant enhancements while maintaining 100% backward compatibility with v1.0.

### Added

#### Offset Management
- **Proper offset tracking** with file hash and line number
- **Resume capability** - connector now resumes from exact line after restart
- **File change detection** - automatically detects when file content changes
- New offset fields: `filename`, `file_hash`, `line_number`, `last_modified`, `file_size`

#### Retry Mechanism
- **Exponential backoff retry** for all remote operations
- Configurable max attempts via `ftp.retry.max.attempts` (default: 3)
- Configurable backoff times via `ftp.retry.backoff.ms` and `ftp.retry.max.backoff.ms`
- Automatic recovery from transient network failures
- Detailed retry logging

#### Data Validation
- **Built-in validation framework** with 8+ validation rules
- Validation rules:
  - `not_empty` - Field must not be empty
  - `numeric` - Must be a number
  - `integer` - Must be an integer
  - `email` - Must be valid email
  - `date(pattern)` - Must match date pattern
  - `length_min(n)` - Minimum length
  - `length_max(n)` - Maximum length
  - `range(min,max)` - Numeric range
  - `pattern(regex)` - Custom regex
- Two validation modes: `strict` (skip invalid) and `lenient` (log warning)
- Configuration via `ftp.validation.enabled`, `ftp.validation.rules`, `ftp.validation.mode`

#### Dead Letter Queue (DLQ)
- **Error record handling** - invalid records sent to separate topic
- DLQ records include: original line, error type, error message, source file, line number, timestamp
- Enable via `ftp.dlq.enabled` and `ftp.dlq.topic`
- Supports both validation errors and processing errors

#### Performance Optimizations
- **Schema caching** - pre-build and reuse schemas for better performance
- **Configurable buffer size** via `ftp.buffer.size.bytes` (default: 32KB)
- **Compression support** - auto-detect and handle .gz files
- Better memory management for large files
- Reduced schema creation overhead

#### Enhanced File Processing
- **Skip header lines** via `ftp.file.skip.header.lines`
- **Skip footer lines** via `ftp.file.skip.footer.lines`
- **Skip empty lines** via `ftp.file.empty.lines.skip` (default: true)
- **Skip comment lines** via `ftp.file.comment.prefix` (e.g., "#")
- Support for compressed files (.gz) with auto-detection

#### Metrics and Monitoring
- **Comprehensive metrics tracking**:
  - Total files processed
  - Total lines processed
  - Total bytes processed
  - Total errors
  - Total validation errors
  - Current file processing time
  - Lines per second
  - Average line read time
  - Max line read time
- Enhanced logging with structured metrics
- Configurable metrics interval via `ftp.metrics.interval.lines`

#### Documentation
- Comprehensive README with all features
- Migration guide (v1.0 → v2.0)
- Troubleshooting guide
- Configuration examples
- Performance tuning guide

### Changed

- **FtpSourceTask**: Major refactoring to support new features
- **FtpSourceConnector**: Added all new configuration parameters
- **Logging**: Enhanced with structured metrics and better context
- **Error handling**: More robust with proper error categorization
- **Summary files**: Now include validation errors, processing errors, and performance metrics

### Fixed

- File reprocessing issue after connector restart (via proper offset management)
- Memory leaks with large files (via schema caching)
- Network timeout issues (via retry mechanism)
- Unclear error messages (via enhanced logging)

### Technical Details

#### New Classes Added
- `FileOffset` - Offset management model
- `ValidationResult` - Validation result model
- `RetryConfig` - Retry configuration
- `RetryableRemoteClient` - Remote client with retry logic
- `RecordValidator` - Validator interface
- `ValidationRule` - Validation rule interface
- `BuiltInValidationRules` - Built-in validation rules
- `ConfigurableValidator` - Configurable validator implementation
- `ProcessingMetrics` - Metrics collection

#### Modified Classes
- `FtpSourceTask` → `FtpSourceTaskEnhanced`
- `FtpSourceConnector` → `FtpSourceConnectorEnhanced`

#### Configuration Changes
- 15+ new configuration parameters
- All existing configurations remain unchanged
- Backward compatible with v1.0

---

## [1.0.1] - 2025-02-17

### Fixed
- Minor bug fixes in file handling
- Improved logging

---

## [1.0.0] - 2025-01-15

### Initial Release

#### Features
- FTP and SFTP protocol support
- Line-by-line file processing
- JSON and string output formats
- Configurable field delimiters
- Optional field mapping via headers
- Composite Kafka keys
- File staging and archiving
- Summary report generation
- Configurable polling interval
- Records per poll limit

#### Supported Configurations
- `ftp.protocol`
- `ftp.host`, `ftp.port`, `ftp.username`, `ftp.password`
- `ftp.directory`, `ftp.directory.stage`, `ftp.directory.archive`
- `ftp.file.pattern`, `ftp.file.encoding`
- `ftp.file.output.format`, `ftp.file.tokenizer`, `ftp.file.headers`
- `ftp.kafka.key.field`
- `ftp.poll.interval.ms`, `ftp.max.records.per.poll`
- `topic`, `tasks.max`

---

## Migration Path

### From 1.0.x to 2.0.0

**Compatibility**: 100% backward compatible

**Steps**:
1. Update connector class to `FtpSourceConnectorEnhanced`
2. Deploy new JAR
3. Optionally enable new features via configuration
4. No data migration needed

**Recommended**: Enable retry mechanism immediately for production stability

See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) for detailed instructions.

---

## Deprecation Notices

**None** - All v1.0 features are maintained in v2.0

---

## Future Roadmap

### Planned for 2.1.0
- Multiple directory support
- Connection pooling
- Custom validation rule plugins
- Prometheus metrics exporter
- Schema Registry integration

### Planned for 2.2.0
- Exactly-once semantics
- Parallel file processing
- File size-based batching
- Cloud storage support (S3, GCS, Azure Blob)

### Under Consideration
- Delta/incremental file processing
- Binary file support
- Custom delimiter patterns
- Multi-tenant support

---

## Versioning Strategy

We follow [Semantic Versioning](https://semver.org/):

- **MAJOR** version for incompatible API changes
- **MINOR** version for backward-compatible functionality additions
- **PATCH** version for backward-compatible bug fixes

---

## Support

- **v2.0.x**: Active development, full support
- **v1.0.x**: Maintenance mode, security fixes only

---

## Contributors

Thanks to all contributors who made v2.0 possible!

---

## License

Apache License 2.0 - See [LICENSE](LICENSE) file for details.
