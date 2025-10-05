package br.com.datastreambrasil.kafka.connector.ftp;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SftpRemoteClient implements RemoteClient {

    private static final Logger log = LoggerFactory.getLogger(SftpRemoteClient.class);

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    private SshClient client;
    private ClientSession session;
    private SftpClient sftp;

    public SftpRemoteClient(Map<String, String> config) {
        this.host = config.get("ftp.host");
        this.port = Integer.parseInt(config.getOrDefault("ftp.port", "22"));
        this.username = config.get("ftp.username");
        this.password = config.get("ftp.password");
    }

    @Override
    public void connect() throws Exception {
        log.info("Connecting to SFTP server {}:{}", host, port);
        client = SshClient.setUpDefaultClient();
        client.setServerKeyVerifier((clientSession, remoteAddress, serverKey) -> true);

        client.start();

        session = client.connect(username, host, port).verify().getSession();
        session.addPasswordIdentity(password);
        session.auth().verify();

        sftp = SftpClientFactory.instance().createSftpClient(session);
        log.info("Connected and authenticated to SFTP server");
    }

    @Override
    public List<String> listFiles(String directory, String pattern) throws Exception {
        Pattern regex = Pattern.compile(pattern);
        List<String> files = new ArrayList<>();

        for (DirEntry entry : sftp.readDir(directory)) {
            if (!entry.getAttributes().isDirectory() && regex.matcher(entry.getFilename()).matches()) {
                files.add(directory + "/" + entry.getFilename());
            }
        }

        return files;
    }

    @Override
    public InputStream retrieveFileStream(String filePath) throws Exception {
        return sftp.read(filePath);
    }

    @Override
    public void moveFile(String sourcePath, String destinationPath) throws Exception {
        sftp.rename(sourcePath, destinationPath);
    }

    @Override
    public void deleteFile(String path) throws Exception {
        sftp.remove(path);
    }

    @Override
    public void writeTextFile(String path, String contents, Charset charset) throws Exception {
        byte[] data = contents.getBytes(charset);
        try (OutputStream out = sftp.write(path)) {
            out.write(data);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (sftp != null)
                sftp.close();
            if (session != null)
                session.close();
            if (client != null)
                client.stop();
            log.info("Disconnected from SFTP server");
        } catch (Exception e) {
            log.warn("Error while disconnecting from SFTP", e);
        }
    }
}
