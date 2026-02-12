package br.com.datastreambrasil.kafka.connector.ftp.retry;

import br.com.datastreambrasil.kafka.connector.ftp.RemoteClient;
import org.apache.kafka.connect.errors.ConnectException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetryableRemoteClientTest {

    @Test
    void testConnectSucceedsFirstAttempt() throws Exception {
        RemoteClient delegate = mock(RemoteClient.class);
        doNothing().when(delegate).connect();

        RetryConfig config = new RetryConfig(3, 100L, 1000L, 2.0);
        RetryableRemoteClient client = new RetryableRemoteClient(delegate, config);

        client.connect();

        verify(delegate, times(1)).connect();
    }

    @Test
    void testConnectSucceedsAfterRetry() throws Exception {
        RemoteClient delegate = mock(RemoteClient.class);
        AtomicInteger attempts = new AtomicInteger(0);

        doAnswer(invocation -> {
            if (attempts.incrementAndGet() < 3) {
                throw new Exception("Connection failed");
            }
            return null;
        }).when(delegate).connect();

        RetryConfig config = new RetryConfig(3, 100L, 1000L, 2.0);
        RetryableRemoteClient client = new RetryableRemoteClient(delegate, config);

        assertDoesNotThrow(() -> client.connect());
        verify(delegate, times(3)).connect();
    }

    @Test
    void testConnectFailsAfterMaxRetries() throws Exception {
        RemoteClient delegate = mock(RemoteClient.class);
        doThrow(new Exception("Connection failed")).when(delegate).connect();

        RetryConfig config = new RetryConfig(3, 100L, 1000L, 2.0);
        RetryableRemoteClient client = new RetryableRemoteClient(delegate, config);

        assertThrows(ConnectException.class, () -> client.connect());
        verify(delegate, times(3)).connect();
    }

    @Test
    void testListFilesWithRetry() throws Exception {
        RemoteClient delegate = mock(RemoteClient.class);
        AtomicInteger attempts = new AtomicInteger(0);

        when(delegate.listFiles(anyString(), anyString())).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() < 2) {
                throw new Exception("List failed");
            }
            return Arrays.asList("/file1.txt", "/file2.txt");
        });

        RetryConfig config = new RetryConfig(3, 100L, 1000L, 2.0);
        RetryableRemoteClient client = new RetryableRemoteClient(delegate, config);

        List<String> files = client.listFiles("/dir", ".*");

        assertEquals(2, files.size());
        verify(delegate, times(2)).listFiles("/dir", ".*");
    }

    @Test
    void testRetrieveFileStreamWithRetry() throws Exception {
        RemoteClient delegate = mock(RemoteClient.class);
        InputStream expectedStream = new ByteArrayInputStream("content".getBytes());
        AtomicInteger attempts = new AtomicInteger(0);

        when(delegate.retrieveFileStream(anyString())).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() < 2) {
                throw new Exception("Retrieve failed");
            }
            return expectedStream;
        });

        RetryConfig config = new RetryConfig(3, 100L, 1000L, 2.0);
        RetryableRemoteClient client = new RetryableRemoteClient(delegate, config);

        InputStream result = client.retrieveFileStream("/file.txt");

        assertSame(expectedStream, result);
        verify(delegate, times(2)).retrieveFileStream("/file.txt");
    }

    @Test
    void testMoveFileWithRetry() throws Exception {
        RemoteClient delegate = mock(RemoteClient.class);
        AtomicInteger attempts = new AtomicInteger(0);

        doAnswer(invocation -> {
            if (attempts.incrementAndGet() < 2) {
                throw new Exception("Move failed");
            }
            return null;
        }).when(delegate).moveFile(anyString(), anyString());

        RetryConfig config = new RetryConfig(3, 100L, 1000L, 2.0);
        RetryableRemoteClient client = new RetryableRemoteClient(delegate, config);

        assertDoesNotThrow(() -> client.moveFile("/source", "/dest"));
        verify(delegate, times(2)).moveFile("/source", "/dest");
    }

    @Test
    void testDeleteFileWithRetry() throws Exception {
        RemoteClient delegate = mock(RemoteClient.class);
        AtomicInteger attempts = new AtomicInteger(0);

        doAnswer(invocation -> {
            if (attempts.incrementAndGet() < 2) {
                throw new Exception("Delete failed");
            }
            return null;
        }).when(delegate).deleteFile(anyString());

        RetryConfig config = new RetryConfig(3, 100L, 1000L, 2.0);
        RetryableRemoteClient client = new RetryableRemoteClient(delegate, config);

        assertDoesNotThrow(() -> client.deleteFile("/file"));
        verify(delegate, times(2)).deleteFile("/file");
    }

    @Test
    void testWriteTextFileWithRetry() throws Exception {
        RemoteClient delegate = mock(RemoteClient.class);
        AtomicInteger attempts = new AtomicInteger(0);

        doAnswer(invocation -> {
            if (attempts.incrementAndGet() < 2) {
                throw new Exception("Write failed");
            }
            return null;
        }).when(delegate).writeTextFile(anyString(), anyString(), any(Charset.class));

        RetryConfig config = new RetryConfig(3, 100L, 1000L, 2.0);
        RetryableRemoteClient client = new RetryableRemoteClient(delegate, config);

        assertDoesNotThrow(() -> client.writeTextFile("/file", "content", Charset.defaultCharset()));
        verify(delegate, times(2)).writeTextFile(eq("/file"), eq("content"), any(Charset.class));
    }

    @Test
    void testDisconnectNoRetry() {
        RemoteClient delegate = mock(RemoteClient.class);
        doNothing().when(delegate).disconnect();

        RetryConfig config = new RetryConfig(3, 100L, 1000L, 2.0);
        RetryableRemoteClient client = new RetryableRemoteClient(delegate, config);

        client.disconnect();

        verify(delegate, times(1)).disconnect();
    }

    @Test
    void testBackoffCalculation() {
        RetryConfig config = new RetryConfig(5, 1000L, 10000L, 2.0);

        assertEquals(0, config.calculateBackoff(0));
        assertEquals(1000, config.calculateBackoff(1));
        assertEquals(2000, config.calculateBackoff(2));
        assertEquals(4000, config.calculateBackoff(3));
        assertEquals(8000, config.calculateBackoff(4));
        assertEquals(10000, config.calculateBackoff(5)); // capped at max
    }
}
