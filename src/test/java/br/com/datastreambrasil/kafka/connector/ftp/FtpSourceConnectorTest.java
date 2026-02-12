package br.com.datastreambrasil.kafka.connector.ftp;

import org.apache.kafka.common.config.ConfigDef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FtpSourceConnectorTest {

    private FtpSourceConnector connector;
    private Map<String, String> config;

    @BeforeEach
    void setup() {
        connector = new FtpSourceConnector();

        config = new HashMap<>();
        config.put(FtpSourceConnector.FTP_PROTOCOL, "sftp");
        config.put(FtpSourceConnector.FTP_HOST, "localhost");
        config.put(FtpSourceConnector.FTP_PORT, "22");
        config.put(FtpSourceConnector.FTP_USERNAME, "user");
        config.put(FtpSourceConnector.FTP_PASSWORD, "pass");
        config.put(FtpSourceConnector.FTP_DIRECTORY, "/input");
        config.put(FtpSourceConnector.FTP_DIRECTORY_STAGE, "/stage");
        config.put(FtpSourceConnector.FTP_DIRECTORY_ARCHIVE, "/archive");
        config.put(FtpSourceConnector.FTP_FILE_PATTERN, ".*\\.txt");
        config.put(FtpSourceConnector.FTP_FILE_ENCODING, "UTF-8");
        config.put(FtpSourceConnector.FTP_FILE_OUTPUT_FORMAT, "json");
        config.put(FtpSourceConnector.FTP_FILE_TOKENIZER, ";");
        config.put(FtpSourceConnector.FTP_FILE_HEADERS, "col1,col2,col3");
        config.put(FtpSourceConnector.FTP_POLL_INTERVAL, "10000");
        config.put(FtpSourceConnector.TOPIC, "test-topic");
    }

    @Test
    void testStartAndTaskConfigs() {
        connector.start(config);

        List<Map<String, String>> taskConfigs = connector.taskConfigs(2);

        assertEquals(2, taskConfigs.size());
        assertEquals(config, taskConfigs.get(0));
    }

    @Test
    void testTaskClass() {
        assertEquals(FtpSourceTask.class, connector.taskClass());
    }

    @Test
    void testVersion() {
        assertEquals(FtpSourceConnector.VERSION, connector.version());
    }

    @Test
    void testConfigDefinitionIncludesAllFields() {
        ConfigDef configDef = connector.config();

        assertNotNull(configDef.configKeys().get(FtpSourceConnector.FTP_PROTOCOL));
        assertNotNull(configDef.configKeys().get(FtpSourceConnector.FTP_HOST));
        assertNotNull(configDef.configKeys().get(FtpSourceConnector.FTP_USERNAME));
        assertNotNull(configDef.configKeys().get(FtpSourceConnector.TOPIC));
        assertNotNull(configDef.configKeys().get(FtpSourceConnector.FTP_FILE_HEADERS));

        assertEquals("Protocol to use: ftp or sftp",
                configDef.configKeys().get(FtpSourceConnector.FTP_PROTOCOL).documentation);
        assertEquals("Optional comma-separated list of field names to be used as JSON keys",
                configDef.configKeys().get(FtpSourceConnector.FTP_FILE_HEADERS).documentation);
    }
}
