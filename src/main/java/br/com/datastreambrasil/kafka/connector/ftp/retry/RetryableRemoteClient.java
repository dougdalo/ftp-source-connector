package br.com.datastreambrasil.kafka.connector.ftp.retry;

import br.com.datastreambrasil.kafka.connector.ftp.RemoteClient;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Wrapper for RemoteClient that adds retry logic with exponential backoff
 */
public class RetryableRemoteClient implements RemoteClient {
    private static final Logger log = LoggerFactory.getLogger(RetryableRemoteClient.class);

    private final RemoteClient delegate;
    private final RetryConfig retryConfig;

    public RetryableRemoteClient(RemoteClient delegate, RetryConfig retryConfig) {
        this.delegate = delegate;
        this.retryConfig = retryConfig;
    }

    @Override
    public void connect() throws Exception {
        executeWithRetry(() -> {
            delegate.connect();
            return null;
        }, "connect");
    }

    @Override
    public List<String> listFiles(String directory, String pattern) throws Exception {
        return executeWithRetry(() -> delegate.listFiles(directory, pattern), "listFiles");
    }

    @Override
    public InputStream retrieveFileStream(String filePath) throws Exception {
        return executeWithRetry(() -> delegate.retrieveFileStream(filePath), "retrieveFileStream");
    }

    @Override
    public void moveFile(String sourcePath, String destinationPath) throws Exception {
        executeWithRetry(() -> {
            delegate.moveFile(sourcePath, destinationPath);
            return null;
        }, "moveFile");
    }

    @Override
    public void deleteFile(String path) throws Exception {
        executeWithRetry(() -> {
            delegate.deleteFile(path);
            return null;
        }, "deleteFile");
    }

    @Override
    public void writeTextFile(String path, String contents, Charset charset) throws Exception {
        executeWithRetry(() -> {
            delegate.writeTextFile(path, contents, charset);
            return null;
        }, "writeTextFile");
    }

    @Override
    public void disconnect() {
        delegate.disconnect();
    }

    private <T> T executeWithRetry(Callable<T> operation, String operationName) throws Exception {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < retryConfig.getMaxAttempts()) {
            try {
                T result = operation.call();
                if (attempt > 0) {
                    log.info("Operation '{}' succeeded after {} attempt(s)", operationName, attempt + 1);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt < retryConfig.getMaxAttempts()) {
                    long backoff = retryConfig.calculateBackoff(attempt);
                    log.warn("Operation '{}' failed (attempt {}/{}), retrying in {}ms: {}",
                            operationName, attempt, retryConfig.getMaxAttempts(), backoff, e.getMessage());

                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ConnectException("Retry interrupted", ie);
                    }
                } else {
                    log.error("Operation '{}' failed after {} attempt(s)", operationName, attempt, e);
                }
            }
        }

        throw new ConnectException("Operation '" + operationName + "' failed after " +
                retryConfig.getMaxAttempts() + " attempts", lastException);
    }
}
