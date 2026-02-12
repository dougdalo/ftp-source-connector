package br.com.datastreambrasil.kafka.connector.ftp;

import br.com.datastreambrasil.kafka.connector.ftp.metrics.ProcessingMetrics;
import br.com.datastreambrasil.kafka.connector.ftp.model.FileOffset;
import br.com.datastreambrasil.kafka.connector.ftp.model.ValidationResult;
import br.com.datastreambrasil.kafka.connector.ftp.retry.RetryConfig;
import br.com.datastreambrasil.kafka.connector.ftp.retry.RetryableRemoteClient;
import br.com.datastreambrasil.kafka.connector.ftp.validation.ConfigurableValidator;
import br.com.datastreambrasil.kafka.connector.ftp.validation.RecordValidator;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class FtpSourceTaskEnhanced extends SourceTask {

    public static final String VERSION = FtpSourceConnectorEnhanced.VERSION;
    private static final Logger log = LoggerFactory.getLogger(FtpSourceTaskEnhanced.class);

    protected RemoteClient client;
    private String topic;
    private String directory;
    private String stageDir;
    private String archiveDir;
    private String filePattern;
    private String fileEncoding;
    protected String outputFormat;
    protected String tokenizer;
    private String keyFieldName;
    private long pollInterval;
    private int maxRecordsPerPoll;
    private int bufferSize;
    private boolean autoDetectCompression;
    private int skipHeaderLines;
    private int skipFooterLines;
    private boolean skipEmptyLines;
    private String commentPrefix;

    // Validation
    private boolean validationEnabled;
    private RecordValidator validator;
    private String validationMode; // "strict" or "lenient"

    // DLQ
    private boolean dlqEnabled;
    private String dlqTopic;

    // Retry
    private int retryMaxAttempts;
    private long retryBackoffMs;
    private long retryMaxBackoffMs;

    // Metrics
    private ProcessingMetrics metrics;
    private int metricsIntervalLines;

    private BufferedReader currentReader;
    private InputStream currentStream;
    private String currentFilename;
    private String currentStagedPath;
    private long linesProcessed;
    private long linesSkipped;
    protected List<String> fieldHeaders;
    
    // Schema caching
    private Schema cachedSchema = null;
    private String cachedSchemaSignature = null;

    // Offset management
    private FileOffset currentFileOffset;
    private long resumeFromLine = 0;

    @Override
    public void start(Map<String, String> props) {
        log.info("Starting FtpSourceTask with enhanced features");

        String protocol = props.get(FtpSourceConnectorEnhanced.FTP_PROTOCOL);
        this.topic = props.get(FtpSourceConnectorEnhanced.TOPIC);
        this.directory = props.get(FtpSourceConnectorEnhanced.FTP_DIRECTORY);
        this.stageDir = props.get(FtpSourceConnectorEnhanced.FTP_DIRECTORY_STAGE);
        this.archiveDir = props.get(FtpSourceConnectorEnhanced.FTP_DIRECTORY_ARCHIVE);
        this.filePattern = props.get(FtpSourceConnectorEnhanced.FTP_FILE_PATTERN);
        this.fileEncoding = props.getOrDefault(FtpSourceConnectorEnhanced.FTP_FILE_ENCODING, "UTF-8");
        this.outputFormat = props.getOrDefault(FtpSourceConnectorEnhanced.FTP_FILE_OUTPUT_FORMAT, "string").toLowerCase();
        this.tokenizer = props.getOrDefault(FtpSourceConnectorEnhanced.FTP_FILE_TOKENIZER, ";");
        this.keyFieldName = props.getOrDefault(FtpSourceConnectorEnhanced.FTP_KAFKA_KEY_FIELD, "").trim();
        this.pollInterval = Long.parseLong(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_POLL_INTERVAL, "10000"));
        this.maxRecordsPerPoll = Integer.parseInt(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_MAX_RECORDS_PER_POLL, "1000"));

        // Enhanced configurations
        this.bufferSize = Integer.parseInt(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_BUFFER_SIZE, "32768"));
        this.autoDetectCompression = Boolean.parseBoolean(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_AUTO_DETECT_COMPRESSION, "true"));
        this.skipHeaderLines = Integer.parseInt(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_SKIP_HEADER_LINES, "0"));
        this.skipFooterLines = Integer.parseInt(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_SKIP_FOOTER_LINES, "0"));
        this.skipEmptyLines = Boolean.parseBoolean(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_SKIP_EMPTY_LINES, "true"));
        this.commentPrefix = props.getOrDefault(FtpSourceConnectorEnhanced.FTP_COMMENT_PREFIX, "");

        // Validation
        this.validationEnabled = Boolean.parseBoolean(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_VALIDATION_ENABLED, "false"));
        this.validationMode = props.getOrDefault(FtpSourceConnectorEnhanced.FTP_VALIDATION_MODE, "strict");
        String validationRules = props.getOrDefault(FtpSourceConnectorEnhanced.FTP_VALIDATION_RULES, "");
        if (validationEnabled && !validationRules.isEmpty()) {
            this.validator = new ConfigurableValidator(validationRules);
            log.info("Validation enabled with mode: {} and rules: {}", validationMode, validationRules);
        }

        // DLQ
        this.dlqEnabled = Boolean.parseBoolean(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_DLQ_ENABLED, "false"));
        this.dlqTopic = props.getOrDefault(FtpSourceConnectorEnhanced.FTP_DLQ_TOPIC, topic + ".dlq");

        // Retry
        this.retryMaxAttempts = Integer.parseInt(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_RETRY_MAX_ATTEMPTS, "3"));
        this.retryBackoffMs = Long.parseLong(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_RETRY_BACKOFF_MS, "1000"));
        this.retryMaxBackoffMs = Long.parseLong(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_RETRY_MAX_BACKOFF_MS, "30000"));

        // Metrics
        this.metrics = new ProcessingMetrics();
        this.metricsIntervalLines = Integer.parseInt(props.getOrDefault(FtpSourceConnectorEnhanced.FTP_METRICS_INTERVAL_LINES, "10000"));

        String headersConfig = props.getOrDefault(FtpSourceConnectorEnhanced.FTP_FILE_HEADERS, "").trim();
        this.fieldHeaders = headersConfig.isEmpty() ? null : Arrays.asList(headersConfig.split("\\s*,\\s*"));

        // Pre-build schema if headers are fixed
        if (fieldHeaders != null && !fieldHeaders.isEmpty() && "json".equals(outputFormat)) {
            this.cachedSchema = buildSchemaFromHeaders(fieldHeaders);
            this.cachedSchemaSignature = calculateSchemaSignature(fieldHeaders);
            log.info("Pre-built schema with {} fields", fieldHeaders.size());
        }

        try {
            if (this.client == null) {
                RemoteClient baseClient = "sftp".equalsIgnoreCase(protocol)
                        ? new SftpRemoteClient(props)
                        : new FtpRemoteClient(props);

                // Wrap with retry logic
                RetryConfig retryConfig = new RetryConfig(retryMaxAttempts, retryBackoffMs, retryMaxBackoffMs, 2.0);
                this.client = new RetryableRemoteClient(baseClient, retryConfig);
            }

            long startTime = System.currentTimeMillis();
            this.client.connect();
            long estimatedTime = System.currentTimeMillis() - startTime;
            log.info("Connected to {} {} server in {} ms with retry support (max attempts: {})",
                    protocol.toUpperCase(), props.get(FtpSourceConnectorEnhanced.FTP_HOST), estimatedTime, retryMaxAttempts);

            this.currentReader = null;
            this.currentStream = null;
            this.currentFilename = null;
            this.currentStagedPath = null;
            this.linesProcessed = 0;
            this.linesSkipped = 0;
        } catch (Exception e) {
            log.error("Failed to connect to remote server", e);
            throw new ConnectException("Failed to connect to remote server", e);
        }
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        List<SourceRecord> records = new ArrayList<>();
        List<SourceRecord> dlqRecords = new ArrayList<>();

        try {
            if (currentReader == null) {
                log.info("Polling files from directory: {}", directory);
                long startTime = System.currentTimeMillis();
                List<String> files = client.listFiles(directory, filePattern);
                long estimatedTime = System.currentTimeMillis() - startTime;
                log.info("Polled {} files from directory: {} in {} ms", files.size(), directory, estimatedTime);

                if (files.isEmpty()) {
                    Thread.sleep(pollInterval);
                    return records;
                }

                String file = files.get(0);
                currentFilename = file.substring(file.lastIndexOf('/') + 1);
                currentStagedPath = stageDir + "/" + currentFilename;

                // Check if we have a previous offset for this file
                Map<String, Object> partition = Collections.singletonMap("file", currentFilename);
                Map<String, Object> lastOffset = context.offsetStorageReader().offset(partition);
                FileOffset previousOffset = FileOffset.fromMap(lastOffset);

                log.info("Staging file: {} → {}", file, currentStagedPath);
                startTime = System.currentTimeMillis();
                client.moveFile(file, currentStagedPath);
                estimatedTime = System.currentTimeMillis() - startTime;
                log.info("Staged file: {} → {} in {} ms", file, currentStagedPath, estimatedTime);

                log.info("Streaming file: {}", currentStagedPath);
                startTime = System.currentTimeMillis();
                currentStream = client.retrieveFileStream(currentStagedPath);

                // Handle compression
                if (autoDetectCompression) {
                    currentStream = handleCompression(currentStream, currentFilename);
                }

                estimatedTime = System.currentTimeMillis() - startTime;
                log.info("Streamed file: {} in {} ms", currentStagedPath, estimatedTime);

                // Calculate file hash for offset management
                byte[] fileBytes = readAllBytes(currentStream);
                String fileHash = calculateHash(fileBytes);
                currentFileOffset = new FileOffset(currentFilename, fileHash, 0, System.currentTimeMillis(), fileBytes.length);

                // Check if we should resume from a previous offset
                if (previousOffset != null && previousOffset.isSameFile(currentFileOffset)) {
                    resumeFromLine = previousOffset.getLineNumber();
                    log.info("Resuming file {} from line {}", currentFilename, resumeFromLine);
                } else {
                    resumeFromLine = 0;
                    if (previousOffset != null) {
                        log.info("File {} has changed (hash or size mismatch), processing from start", currentFilename);
                    }
                }

                currentStream = new ByteArrayInputStream(fileBytes);
                currentReader = new BufferedReader(new InputStreamReader(currentStream, fileEncoding), bufferSize);
                linesProcessed = 0;
                linesSkipped = 0;

                metrics.startFile(currentFilename);

                // Skip header lines
                for (int i = 0; i < skipHeaderLines; i++) {
                    currentReader.readLine();
                    linesSkipped++;
                }

                // Skip to resume point if resuming
                for (long i = 0; i < resumeFromLine; i++) {
                    currentReader.readLine();
                    linesSkipped++;
                }
            }

            boolean eof = false;
            String line;
            long generalStartTime = System.currentTimeMillis();
            long readLineStartTime = System.currentTimeMillis();
            long readLineTotalTime = 0;
            long readLineMaxTime = 0;

            List<String> allLines = new ArrayList<>();
            while ((line = currentReader.readLine()) != null) {
                allLines.add(line);
            }

            // Remove footer lines if needed
            int linesToProcess = allLines.size() - skipFooterLines;
            if (linesToProcess < 0) linesToProcess = 0;

            for (int i = 0; i < linesToProcess && records.size() < maxRecordsPerPoll; i++) {
                line = allLines.get(i);
                long rowReadEstimatedTime = System.currentTimeMillis() - readLineStartTime;
                if (rowReadEstimatedTime > readLineMaxTime) {
                    readLineMaxTime = rowReadEstimatedTime;
                }
                readLineTotalTime += rowReadEstimatedTime;

                // Skip empty lines if configured
                if (skipEmptyLines && line.trim().isEmpty()) {
                    linesSkipped++;
                    readLineStartTime = System.currentTimeMillis();
                    continue;
                }

                // Skip comment lines if configured
                if (!commentPrefix.isEmpty() && line.trim().startsWith(commentPrefix)) {
                    linesSkipped++;
                    readLineStartTime = System.currentTimeMillis();
                    continue;
                }

                Map<String, Object> sourcePartition = Collections.singletonMap("file", currentFilename);
                Map<String, Object> sourceOffset = new HashMap<>();
                sourceOffset.put("filename", currentFilename);
                sourceOffset.put("file_hash", currentFileOffset.getFileHash());
                sourceOffset.put("line_number", linesProcessed + linesSkipped);
                sourceOffset.put("last_modified", currentFileOffset.getLastModified());
                sourceOffset.put("file_size", currentFileOffset.getFileSize());

                long startTime = System.currentTimeMillis();

                try {
                    RecordModel record = buildRecordModel(line);
                    Object value = record.value;
                    Schema schema = record.schema;

                    // Validate if enabled
                    if (validationEnabled && validator != null && value instanceof Struct) {
                        ValidationResult validationResult = validator.validate((Struct) value);
                        if (!validationResult.isValid()) {
                            metrics.incrementValidationErrors();
                            handleValidationError(line, validationResult, sourcePartition, sourceOffset, dlqRecords);

                            if ("strict".equals(validationMode)) {
                                linesProcessed++;
                                readLineStartTime = System.currentTimeMillis();
                                continue;
                            }
                        }
                    }

                    Object recordKey = null;
                    if ("json".equals(outputFormat) && value instanceof Struct) {
                        recordKey = buildKafkaKey((Struct) value, keyFieldName);
                    }

                    records.add(new SourceRecord(
                            sourcePartition,
                            sourceOffset,
                            topic,
                            Schema.OPTIONAL_STRING_SCHEMA,
                            recordKey != null ? recordKey.toString() : null,
                            schema,
                            value));

                    linesProcessed++;
                    metrics.incrementLines(1);
                    metrics.incrementBytes(line.getBytes(fileEncoding).length);

                } catch (Exception e) {
                    metrics.incrementErrors();
                    handleProcessingError(line, e, sourcePartition, sourceOffset, dlqRecords);
                }

                long estimatedTime = System.currentTimeMillis() - startTime;

                if ((linesProcessed + linesSkipped) % metricsIntervalLines == 0) {
                    long elapsedEstimatedTime = System.currentTimeMillis() - generalStartTime;
                    long currentReadLineAverageTime = linesProcessed > 0 ? readLineTotalTime / linesProcessed : 0;
                    log.info("Processed {} lines (skipped {}) from {} in {} ms (row read avg {} ms max {} ms, lines/sec: {:.2f})",
                            linesProcessed, linesSkipped, currentFilename, elapsedEstimatedTime,
                            currentReadLineAverageTime, readLineMaxTime, metrics.getCurrentFileLinesPerSecond());
                }

                readLineStartTime = System.currentTimeMillis();
            }

            eof = (linesToProcess == allLines.size() || linesToProcess == 0);

            long generalEstimatedTime = System.currentTimeMillis() - generalStartTime;
            long readLineAverageTime = linesProcessed > 0 ? readLineTotalTime / linesProcessed : 0;

            if (eof) {
                if (currentReader != null) currentReader.close();
                if (client instanceof FtpRemoteClient) {
                    ((FtpRemoteClient) client).completePending();
                }
                if (currentStream != null) currentStream.close();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");
                String timestamp = LocalDateTime.now().format(formatter);
                String summaryFilename = currentFilename.replaceAll("(\\.\\w+)?$", "_" + timestamp + ".txt");
                String summaryPath = archiveDir + "/" + summaryFilename;
                String summaryContent = String.format(Locale.ROOT,
                        "File: %s%nLines processed: %d%nLines skipped: %d%nProcessed at: %s%nProcessing time (ms): %d%n" +
                        "Average line read time (ms): %d%nMax line read time (ms): %d%nValidation errors: %d%nProcessing errors: %d%n" +
                        "Lines per second: %.2f",
                        currentFilename, linesProcessed, linesSkipped, timestamp, generalEstimatedTime,
                        readLineAverageTime, readLineMaxTime, metrics.getTotalValidationErrors(),
                        metrics.getTotalErrors(), metrics.getCurrentFileLinesPerSecond());

                log.info("Deleting staged file: {}", currentStagedPath);
                long startTime = System.currentTimeMillis();
                client.deleteFile(currentStagedPath);
                long estimatedTime = System.currentTimeMillis() - startTime;
                log.info("Deleted staged file: {} in {} ms", currentStagedPath, estimatedTime);

                log.info("Writing summary file: {}", summaryPath);
                startTime = System.currentTimeMillis();
                client.writeTextFile(summaryPath, summaryContent, Charset.forName(fileEncoding));
                estimatedTime = System.currentTimeMillis() - startTime;
                log.info("Summary file written: {} in {} ms", summaryPath, estimatedTime);

                log.info("Finished processing file {} with {} lines (skipped {}) in {} ms (row read avg {} ms max {} ms, lines/sec: {:.2f})",
                        currentFilename, linesProcessed, linesSkipped, generalEstimatedTime,
                        readLineAverageTime, readLineMaxTime, metrics.getCurrentFileLinesPerSecond());

                metrics.endFile();

                currentReader = null;
                currentStream = null;
                currentFilename = null;
                currentStagedPath = null;
                currentFileOffset = null;
                resumeFromLine = 0;
            }

        } catch (Exception e) {
            log.error("Error during polling", e);
            metrics.incrementErrors();
            throw new ConnectException("Error during polling from remote server", e);
        }

        // Add DLQ records if any
        records.addAll(dlqRecords);

        Thread.sleep(pollInterval);
        return records;
    }

    @Override
    public void stop() {
        log.info("Stopping FtpSourceTask");
        log.info("Final metrics: {}", metrics);
        if (client != null) {
            client.disconnect();
            log.info("Disconnected from server");
        }
    }

    public void setClient(RemoteClient client) {
        this.client = client;
    }

    @Override
    public String version() {
        return VERSION;
    }

    protected String buildKafkaKey(Struct struct, String keyFieldName) {
        if (keyFieldName == null || keyFieldName.isEmpty()) return null;

        String[] keyFields = keyFieldName.split("\\+");
        StringBuilder keyBuilder = new StringBuilder();

        for (String field : keyFields) {
            Object part = struct.get(field.trim());
            if (part != null) {
                if (keyBuilder.length() > 0) keyBuilder.append("_");
                keyBuilder.append(part.toString());
            }
        }

        return keyBuilder.length() > 0 ? keyBuilder.toString() : null;
    }

    protected RecordModel buildRecordModel(String line) {
        if ("json".equals(outputFormat)) {
            String[] parts = line.split(Pattern.quote(tokenizer), -1);

            Schema schema = getOrBuildSchema(parts.length);

            Struct struct = new Struct(schema);
            for (int i = 0; i < parts.length; i++) {
                String key = getFieldName(i);
                struct.put(key, parts[i].trim());
            }

            return new RecordModel(struct, schema);
        } else {
            return new RecordModel(line, Schema.STRING_SCHEMA);
        }
    }

    private Schema getOrBuildSchema(int fieldCount) {
        String currentSignature = calculateSchemaSignature(fieldCount);

        if (cachedSchema != null && cachedSchemaSignature.equals(currentSignature)) {
            return cachedSchema;
        }

        SchemaBuilder builder = SchemaBuilder.struct().optional();
        for (int i = 0; i < fieldCount; i++) {
            builder.field(getFieldName(i), Schema.OPTIONAL_STRING_SCHEMA);
        }

        cachedSchema = builder.build();
        cachedSchemaSignature = currentSignature;
        return cachedSchema;
    }

    private Schema buildSchemaFromHeaders(List<String> headers) {
        SchemaBuilder builder = SchemaBuilder.struct().optional();
        for (String header : headers) {
            builder.field(header, Schema.OPTIONAL_STRING_SCHEMA);
        }
        return builder.build();
    }

    private String getFieldName(int index) {
        return (fieldHeaders != null && index < fieldHeaders.size())
                ? fieldHeaders.get(index)
                : "field" + (index + 1);
    }

    private String calculateSchemaSignature(int fieldCount) {
        if (fieldHeaders != null && fieldHeaders.size() == fieldCount) {
            return String.join(",", fieldHeaders);
        }
        return "fields:" + fieldCount;
    }

    private String calculateSchemaSignature(List<String> headers) {
        return String.join(",", headers);
    }

    private InputStream handleCompression(InputStream stream, String filename) throws IOException {
        if (filename.endsWith(".gz") || filename.endsWith(".gzip")) {
            log.info("Detected GZIP compression for file: {}", filename);
            return new GZIPInputStream(stream);
        }
        // Add more compression types if needed
        return stream;
    }

    private String calculateHash(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to calculate file hash", e);
            return "unknown";
        }
    }

    private byte[] readAllBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[bufferSize];
        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private void handleValidationError(String line, ValidationResult result,
                                       Map<String, Object> sourcePartition,
                                       Map<String, Object> sourceOffset,
                                       List<SourceRecord> dlqRecords) {
        log.warn("Validation failed for line {}: {}", linesProcessed, result.getErrorMessage());

        if (dlqEnabled) {
            sendToDLQ(line, "VALIDATION_ERROR", result.getErrorMessage(),
                    sourcePartition, sourceOffset, dlqRecords);
        }
    }

    private void handleProcessingError(String line, Exception error,
                                       Map<String, Object> sourcePartition,
                                       Map<String, Object> sourceOffset,
                                       List<SourceRecord> dlqRecords) {
        log.error("Processing error for line {}: {}", linesProcessed, error.getMessage(), error);

        if (dlqEnabled) {
            sendToDLQ(line, "PROCESSING_ERROR", error.getMessage(),
                    sourcePartition, sourceOffset, dlqRecords);
        }
    }

    private void sendToDLQ(String originalLine, String errorType, String errorMessage,
                          Map<String, Object> sourcePartition,
                          Map<String, Object> sourceOffset,
                          List<SourceRecord> dlqRecords) {
        try {
            Schema errorSchema = SchemaBuilder.struct()
                    .field("original_line", Schema.STRING_SCHEMA)
                    .field("error_type", Schema.STRING_SCHEMA)
                    .field("error_message", Schema.STRING_SCHEMA)
                    .field("source_file", Schema.STRING_SCHEMA)
                    .field("line_number", Schema.INT64_SCHEMA)
                    .field("timestamp", Schema.STRING_SCHEMA)
                    .build();

            Struct errorStruct = new Struct(errorSchema)
                    .put("original_line", originalLine)
                    .put("error_type", errorType)
                    .put("error_message", errorMessage)
                    .put("source_file", currentFilename)
                    .put("line_number", linesProcessed + linesSkipped)
                    .put("timestamp", Instant.now().toString());

            SourceRecord dlqRecord = new SourceRecord(
                    sourcePartition,
                    sourceOffset,
                    dlqTopic,
                    Schema.STRING_SCHEMA,
                    currentFilename + ":" + (linesProcessed + linesSkipped),
                    errorSchema,
                    errorStruct
            );

            dlqRecords.add(dlqRecord);
            log.debug("Sent record to DLQ topic: {}", dlqTopic);
        } catch (Exception e) {
            log.error("Failed to send record to DLQ", e);
        }
    }
}
