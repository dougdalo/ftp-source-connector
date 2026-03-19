package br.com.datastreambrasil.kafka.connector.ftp;

import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.apache.kafka.common.config.ConfigDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FtpSourceConnectorEnhanced extends SourceConnector {

    public static final String VERSION = "2.0.0";

    // Existing configs
    public static final String FTP_PROTOCOL = "ftp.protocol";
    public static final String FTP_HOST = "ftp.host";
    public static final String FTP_PORT = "ftp.port";
    public static final String FTP_USERNAME = "ftp.username";
    public static final String FTP_PASSWORD = "ftp.password";
    public static final String FTP_DIRECTORY = "ftp.directory";
    public static final String FTP_DIRECTORY_STAGE = "ftp.directory.stage";
    public static final String FTP_DIRECTORY_ARCHIVE = "ftp.directory.archive";
    public static final String FTP_FILE_PATTERN = "ftp.file.pattern";
    public static final String FTP_FILE_ENCODING = "ftp.file.encoding";
    public static final String FTP_FILE_OUTPUT_FORMAT = "ftp.file.output.format";
    public static final String FTP_FILE_TOKENIZER = "ftp.file.tokenizer";
    public static final String FTP_FILE_HEADERS = "ftp.file.headers";
    public static final String FTP_KAFKA_KEY_FIELD = "ftp.kafka.key.field";
    public static final String FTP_MAX_RECORDS_PER_POLL = "ftp.max.records.per.poll";
    public static final String FTP_POLL_INTERVAL = "ftp.poll.interval.ms";
    public static final String TOPIC = "topic";

    // NEW: Performance configs
    public static final String FTP_BUFFER_SIZE = "ftp.buffer.size.bytes";
    public static final String FTP_AUTO_DETECT_COMPRESSION = "ftp.file.compression.auto.detect";

    // NEW: File processing configs
    public static final String FTP_SKIP_HEADER_LINES = "ftp.file.skip.header.lines";
    public static final String FTP_SKIP_FOOTER_LINES = "ftp.file.skip.footer.lines";
    public static final String FTP_SKIP_EMPTY_LINES = "ftp.file.empty.lines.skip";
    public static final String FTP_COMMENT_PREFIX = "ftp.file.comment.prefix";

    // NEW: Validation configs
    public static final String FTP_VALIDATION_ENABLED = "ftp.validation.enabled";
    public static final String FTP_VALIDATION_RULES = "ftp.validation.rules";
    public static final String FTP_VALIDATION_MODE = "ftp.validation.mode";

    // NEW: DLQ configs
    public static final String FTP_DLQ_ENABLED = "ftp.dlq.enabled";
    public static final String FTP_DLQ_TOPIC = "ftp.dlq.topic";

    // NEW: Retry configs
    public static final String FTP_RETRY_MAX_ATTEMPTS = "ftp.retry.max.attempts";
    public static final String FTP_RETRY_BACKOFF_MS = "ftp.retry.backoff.ms";
    public static final String FTP_RETRY_MAX_BACKOFF_MS = "ftp.retry.max.backoff.ms";

    // NEW: Metrics configs
    public static final String FTP_METRICS_INTERVAL_LINES = "ftp.metrics.interval.lines";

    private Map<String, String> config;

    @Override
    public void start(Map<String, String> props) {
        this.config = props;
    }

    @Override
    public Class<? extends Task> taskClass() {
        return FtpSourceTaskEnhanced.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> configs = new ArrayList<>();
        for (int i = 0; i < maxTasks; i++) {
            configs.add(config);
        }
        return configs;
    }

    @Override
    public void stop() {
        // Do Nothing
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef()
                // Connection configs
                .define(FTP_PROTOCOL, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                        "Protocol to use: ftp or sftp")
                .define(FTP_HOST, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                        "Hostname or IP address of the FTP/SFTP server")
                .define(FTP_PORT, ConfigDef.Type.INT, ConfigDef.Importance.MEDIUM,
                        "Port number of the FTP/SFTP server")
                .define(FTP_USERNAME, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                        "Username for authentication")
                .define(FTP_PASSWORD, ConfigDef.Type.PASSWORD, ConfigDef.Importance.HIGH,
                        "Password for authentication")

                // Directory configs
                .define(FTP_DIRECTORY, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                        "Directory where input files are located")
                .define(FTP_DIRECTORY_STAGE, ConfigDef.Type.STRING, ConfigDef.Importance.MEDIUM,
                        "Directory for staging files during processing")
                .define(FTP_DIRECTORY_ARCHIVE, ConfigDef.Type.STRING, ConfigDef.Importance.MEDIUM,
                        "Directory for archiving files after processing")

                // File processing configs
                .define(FTP_FILE_PATTERN, ConfigDef.Type.STRING, ".*\\.txt", ConfigDef.Importance.MEDIUM,
                        "Regex pattern to match filenames for processing")
                .define(FTP_FILE_ENCODING, ConfigDef.Type.STRING, "UTF-8", ConfigDef.Importance.LOW,
                        "Character encoding used to read the files")
                .define(FTP_FILE_OUTPUT_FORMAT, ConfigDef.Type.STRING, "string", ConfigDef.Importance.LOW,
                        "Format for output records: 'string' or 'json'")
                .define(FTP_FILE_TOKENIZER, ConfigDef.Type.STRING, ";", ConfigDef.Importance.LOW,
                        "Delimiter used to split each line when format is 'json'")
                .define(FTP_FILE_HEADERS, ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW,
                        "Optional comma-separated list of field names to be used as JSON keys")
                .define(FTP_SKIP_HEADER_LINES, ConfigDef.Type.INT, 0, ConfigDef.Importance.LOW,
                        "Number of header lines to skip at start of file")
                .define(FTP_SKIP_FOOTER_LINES, ConfigDef.Type.INT, 0, ConfigDef.Importance.LOW,
                        "Number of footer lines to skip at end of file")
                .define(FTP_SKIP_EMPTY_LINES, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.LOW,
                        "Skip empty lines during processing")
                .define(FTP_COMMENT_PREFIX, ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW,
                        "Skip lines starting with this prefix (e.g., '#')")

                // Kafka configs
                .define(FTP_KAFKA_KEY_FIELD, ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW,
                        "Field name from headers to be used as Kafka message key (use + for composite keys)")
                .define(TOPIC, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                        "Kafka topic to publish the file data")

                // Performance configs
                .define(FTP_POLL_INTERVAL, ConfigDef.Type.INT, 10000, ConfigDef.Importance.LOW,
                        "Polling interval in milliseconds")
                .define(FTP_MAX_RECORDS_PER_POLL, ConfigDef.Type.INT, 1000, ConfigDef.Importance.LOW,
                        "Maximum number of records returned per poll")
                .define(FTP_BUFFER_SIZE, ConfigDef.Type.INT, 32768, ConfigDef.Importance.LOW,
                        "Buffer size in bytes for reading files (default: 32KB)")
                .define(FTP_AUTO_DETECT_COMPRESSION, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.LOW,
                        "Auto-detect and handle compressed files (.gz)")

                // Validation configs
                .define(FTP_VALIDATION_ENABLED, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.MEDIUM,
                        "Enable data validation")
                .define(FTP_VALIDATION_RULES, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM,
                        "Validation rules in format: field1:not_empty,field2:numeric,field3:range(0,100)")
                .define(FTP_VALIDATION_MODE, ConfigDef.Type.STRING, "strict", ConfigDef.Importance.MEDIUM,
                        "Validation mode: 'strict' (skip invalid records) or 'lenient' (log warning)")

                // DLQ configs
                .define(FTP_DLQ_ENABLED, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.MEDIUM,
                        "Enable Dead Letter Queue for failed records")
                .define(FTP_DLQ_TOPIC, ConfigDef.Type.STRING, "", ConfigDef.Importance.MEDIUM,
                        "Topic for dead letter queue (default: <topic>.dlq)")

                // Retry configs
                .define(FTP_RETRY_MAX_ATTEMPTS, ConfigDef.Type.INT, 3, ConfigDef.Importance.MEDIUM,
                        "Maximum number of retry attempts for failed operations")
                .define(FTP_RETRY_BACKOFF_MS, ConfigDef.Type.LONG, 1000L, ConfigDef.Importance.LOW,
                        "Initial backoff time in milliseconds for retries")
                .define(FTP_RETRY_MAX_BACKOFF_MS, ConfigDef.Type.LONG, 30000L, ConfigDef.Importance.LOW,
                        "Maximum backoff time in milliseconds for retries")

                // Metrics configs
                .define(FTP_METRICS_INTERVAL_LINES, ConfigDef.Type.INT, 10000, ConfigDef.Importance.LOW,
                        "Log metrics every N lines processed");
    }

    @Override
    public String version() {
        return VERSION;
    }
}
