package br.com.datastreambrasil.kafka.connector.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class FtpRemoteClient implements RemoteClient {

    private static final Logger log = LoggerFactory.getLogger(FtpRemoteClient.class);

    private final FTPClient ftpClient;
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public FtpRemoteClient(Map<String, String> config) {
        this.ftpClient = new FTPClient();
        this.host = config.get("ftp.host");
        this.port = Integer.parseInt(config.getOrDefault("ftp.port", "21"));
        this.username = config.get("ftp.username");
        this.password = config.get("ftp.password");
    }

    @Override
    public void connect() throws Exception {
        log.info("Connecting to FTP server {}:{}", host, port);
        ftpClient.connect(host, port);
        if (!ftpClient.login(username, password)) {
            throw new Exception("FTP login failed for user: " + username);
        }
        ftpClient.enterLocalPassiveMode();
        log.info("Connected and logged in to FTP server");
    }

    @Override
    public List<String> listFiles(String directory, String pattern) throws Exception {
        Pattern regex = Pattern.compile(pattern);
        FTPFile[] files = ftpClient.listFiles(directory);
        List<String> result = new ArrayList<>();

        for (FTPFile file : files) {
            if (file.isFile() && regex.matcher(file.getName()).matches()) {
                result.add(directory + "/" + file.getName());
            }
        }

        return result;
    }

    @Override
    public InputStream retrieveFileStream(String filePath) throws Exception {
        InputStream stream = ftpClient.retrieveFileStream(filePath);
        if (stream == null) {
            throw new Exception("Could not retrieve file: " + filePath);
        }
        return stream;
    }

    public void completePending() throws IOException {
        if (!ftpClient.completePendingCommand()) {
            throw new IOException("Failed to complete pending FTP command.");
        }
    }

    @Override
    public void moveFile(String sourcePath, String destinationPath) throws Exception {
        boolean success = ftpClient.rename(sourcePath, destinationPath);
        if (!success) {
            String reply = ftpClient.getReplyString();
            log.error("Failed to rename file. FTP reply: {}", reply);
            throw new IOException("Failed to move file from " + sourcePath + " to " + destinationPath
                    + ". FTP reply: " + reply);
        }
    }

    @Override
    public void deleteFile(String path) throws Exception {
        boolean success = ftpClient.deleteFile(path);
        if (!success) {
            String reply = ftpClient.getReplyString();
            log.error("Failed to delete file {}. FTP reply: {}", path, reply);
            throw new IOException("Failed to delete file " + path + ". FTP reply: " + reply);
        }
    }

    @Override
    public void writeTextFile(String path, String contents, Charset charset) throws Exception {
        byte[] bytes = contents.getBytes(charset);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            boolean success = ftpClient.storeFile(path, inputStream);
            if (!success) {
                String reply = ftpClient.getReplyString();
                log.error("Failed to store file {}. FTP reply: {}", path, reply);
                throw new IOException("Failed to write file " + path + ". FTP reply: " + reply);
            }
        }
    }

    @Override
    public void disconnect() {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
                log.info("Disconnected from FTP server");
            }
        } catch (Exception e) {
            log.warn("Error while disconnecting from FTP", e);
        }
    }
}
