package br.com.datastreambrasil.kafka.connector.ftp;

import java.io.InputStream;
import java.util.List;

public interface RemoteClient {
    void connect() throws Exception;

    List<String> listFiles(String directory, String pattern) throws Exception;

    InputStream retrieveFileStream(String filePath) throws Exception;

    void moveFile(String sourcePath, String destinationPath) throws Exception;

    void disconnect();
}
