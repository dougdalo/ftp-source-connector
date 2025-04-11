package br.com.datastreambrasil.kafka.connector.ftp;

import org.apache.kafka.connect.data.*;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

public class FtpSourceTask extends SourceTask {

    public static final String VERSION = FtpSourceConnector.VERSION;
    private static final Logger log = LoggerFactory.getLogger(FtpSourceTask.class);

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
    protected List<String> fieldHeaders;

    @Override
    public void start(Map<String, String> props) {
        log.info("Starting FtpSourceTask");

        String protocol = props.get(FtpSourceConnector.FTP_PROTOCOL);
        this.topic = props.get(FtpSourceConnector.TOPIC);
        this.directory = props.get(FtpSourceConnector.FTP_DIRECTORY);
        this.stageDir = props.get(FtpSourceConnector.FTP_DIRECTORY_STAGE);
        this.archiveDir = props.get(FtpSourceConnector.FTP_DIRECTORY_ARCHIVE);
        this.filePattern = props.get(FtpSourceConnector.FTP_FILE_PATTERN);
        this.fileEncoding = props.getOrDefault(FtpSourceConnector.FTP_FILE_ENCODING, "UTF-8");
        this.outputFormat = props.getOrDefault(FtpSourceConnector.FTP_FILE_OUTPUT_FORMAT, "string").toLowerCase();
        this.tokenizer = props.getOrDefault(FtpSourceConnector.FTP_FILE_TOKENIZER, ";");
        this.keyFieldName = props.getOrDefault(FtpSourceConnector.FTP_KAFKA_KEY_FIELD, "").trim();
        this.pollInterval = Long.parseLong(props.getOrDefault(FtpSourceConnector.FTP_POLL_INTERVAL, "10000"));

        String headersConfig = props.getOrDefault(FtpSourceConnector.FTP_FILE_HEADERS, "").trim();
        this.fieldHeaders = headersConfig.isEmpty()
                ? null
                : Arrays.asList(headersConfig.split("\\s*,\\s*"));

        try {
            if (this.client == null) {
                this.client = "sftp".equalsIgnoreCase(protocol)
                        ? new SftpRemoteClient(props)
                        : new FtpRemoteClient(props);
            }
            this.client.connect();
            log.info("Connected to {} server", protocol.toUpperCase());
        } catch (Exception e) {
            log.error("Failed to connect to remote server", e);
            throw new ConnectException("Failed to connect to remote server", e);
        }
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        List<SourceRecord> records = new ArrayList<>();

        try {
            log.info("Polling files from directory: {}", directory);
            List<String> files = client.listFiles(directory, filePattern);

            for (String file : files) {
                String filename = file.substring(file.lastIndexOf("/") + 1);
                String stagedPath = stageDir + "/" + filename;

                log.info("Staging file: {} → {}", file, stagedPath);
                client.moveFile(file, stagedPath);

                try (InputStream is = client.retrieveFileStream(stagedPath);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, fileEncoding))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        Map<String, String> sourcePartition = Collections.singletonMap("file", filename);
                        Map<String, Long> sourceOffset = Collections.singletonMap("position",
                                System.currentTimeMillis());

                        RecordModel record = buildRecordModel(line);
                        Object value = record.value;
                        Schema schema = record.schema;

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
                    }

                } catch (Exception ex) {
                    log.error("Error while reading file: {}", stagedPath, ex);
                    throw new ConnectException("Error while reading file: " + stagedPath, ex);
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");
                String timestamp = LocalDateTime.now().format(formatter);
                String archiveFilename = filename.replaceAll("(\\.\\w+)$", "_" + timestamp + "$1");
                String archivedPath = archiveDir + "/" + archiveFilename;

                if (client instanceof FtpRemoteClient ftp) {
                    ftp.completePending();
                }

                log.info("Archiving file: {} → {}", stagedPath, archivedPath);
                client.moveFile(stagedPath, archivedPath);
            }

        } catch (Exception e) {
            log.error("Error during polling", e);
            throw new ConnectException("Error during polling from remote server", e);
        }

        Thread.sleep(pollInterval);
        return records;
    }

    @Override
    public void stop() {
        log.info("Stopping FtpSourceTask");
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
        if (keyFieldName == null || keyFieldName.isEmpty())
            return null;

        String[] keyFields = keyFieldName.split("\\+");
        StringBuilder keyBuilder = new StringBuilder();

        for (String field : keyFields) {
            Object part = struct.get(field.trim());
            if (part != null) {
                if (keyBuilder.length() > 0)
                    keyBuilder.append("_");
                keyBuilder.append(part.toString());
            }
        }

        return keyBuilder.length() > 0 ? keyBuilder.toString() : null;
    }

    protected RecordModel buildRecordModel(String line) {
        if ("json".equals(outputFormat)) {
            String[] parts = line.split(Pattern.quote(tokenizer), -1);

            SchemaBuilder builder = SchemaBuilder.struct().optional();
            for (int i = 0; i < parts.length; i++) {
                String key = (fieldHeaders != null && i < fieldHeaders.size())
                        ? fieldHeaders.get(i)
                        : "field" + (i + 1);
                builder.field(key, Schema.OPTIONAL_STRING_SCHEMA);
            }
            Schema dynamicSchema = builder.build();

            Struct struct = new Struct(dynamicSchema);
            for (int i = 0; i < parts.length; i++) {
                String key = (fieldHeaders != null && i < fieldHeaders.size())
                        ? fieldHeaders.get(i)
                        : "field" + (i + 1);
                struct.put(key, parts[i].trim());
            }

            return new RecordModel(struct, dynamicSchema);
        } else {
            return new RecordModel(line, Schema.STRING_SCHEMA);
        }
    }
}
