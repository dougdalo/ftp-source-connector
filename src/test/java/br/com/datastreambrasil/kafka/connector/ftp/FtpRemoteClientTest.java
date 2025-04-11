package br.com.datastreambrasil.kafka.connector.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FtpRemoteClientTest {

    private FTPClient mockFtpClient;
    private FtpRemoteClient client;

    @BeforeEach
    void setup() {
        mockFtpClient = mock(FTPClient.class);

        client = new FtpRemoteClient(Map.of(
                "ftp.host", "localhost",
                "ftp.port", "21",
                "ftp.username", "user",
                "ftp.password", "pass")) {
            private void injectMock() {
                try {
                    var field = FtpRemoteClient.class.getDeclaredField("ftpClient");
                    field.setAccessible(true);
                    field.set(this, mockFtpClient);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void connect() throws Exception {
                injectMock();
                super.connect();
            }

            @Override
            public List<String> listFiles(String directory, String pattern) throws Exception {
                injectMock();
                return super.listFiles(directory, pattern);
            }

            @Override
            public InputStream retrieveFileStream(String filePath) throws Exception {
                injectMock();
                return super.retrieveFileStream(filePath);
            }

            @Override
            public void moveFile(String sourcePath, String destinationPath) throws Exception {
                injectMock();
                super.moveFile(sourcePath, destinationPath);
            }

            @Override
            public void completePending() throws IOException {
                injectMock();
                super.completePending();
            }

            @Override
            public void disconnect() {
                injectMock();
                super.disconnect();
            }
        };
    }

    @Test
    void testConnectSuccess() throws Exception {
        when(mockFtpClient.login("user", "pass")).thenReturn(true);

        assertDoesNotThrow(() -> client.connect());
        verify(mockFtpClient).connect("localhost", 21);
        verify(mockFtpClient).login("user", "pass");
        verify(mockFtpClient).enterLocalPassiveMode();
    }

    @Test
    void testConnectFailureLogin() throws Exception {
        when(mockFtpClient.login("user", "pass")).thenReturn(false);

        Exception ex = assertThrows(Exception.class, () -> client.connect());
        assertTrue(ex.getMessage().contains("FTP login failed"));
    }

    @Test
    void testListFilesMatchingPattern() throws Exception {
        FTPFile file1 = new FTPFile();
        file1.setName("valid.csv");
        file1.setType(FTPFile.FILE_TYPE);

        FTPFile file2 = new FTPFile();
        file2.setName("skip.txt");
        file2.setType(FTPFile.FILE_TYPE);

        when(mockFtpClient.listFiles("/data")).thenReturn(new FTPFile[] { file1, file2 });

        List<String> result = client.listFiles("/data", ".*\\.csv");

        assertEquals(1, result.size());
        assertEquals("/data/valid.csv", result.get(0));
    }

    @Test
    void testRetrieveFileStream() throws Exception {
        String content = "line1,line2";
        InputStream mockStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        when(mockFtpClient.retrieveFileStream("/file.csv")).thenReturn(mockStream);

        InputStream result = client.retrieveFileStream("/file.csv");

        assertNotNull(result);
        assertEquals(content, new String(result.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void testRetrieveFileStreamNull() {
        assertThrows(Exception.class, () -> client.retrieveFileStream("/missing.txt"));
    }

    @Test
    void testMoveFileSuccess() throws Exception {
        when(mockFtpClient.rename("/src.csv", "/dest.csv")).thenReturn(true);

        assertDoesNotThrow(() -> client.moveFile("/src.csv", "/dest.csv"));
    }

    @Test
    void testMoveFileFailure() throws Exception {
        when(mockFtpClient.rename("/src.csv", "/dest.csv")).thenReturn(false);
        when(mockFtpClient.getReplyString()).thenReturn("550 File not found");

        Exception ex = assertThrows(Exception.class, () -> client.moveFile("/src.csv", "/dest.csv"));
        assertTrue(ex.getMessage().contains("Failed to move file"));
    }

    @Test
    void testCompletePendingSuccess() throws Exception {
        when(mockFtpClient.completePendingCommand()).thenReturn(true);

        assertDoesNotThrow(() -> client.completePending());
    }

    @Test
    void testCompletePendingFailure() throws Exception {
        when(mockFtpClient.completePendingCommand()).thenReturn(false);

        Exception ex = assertThrows(IOException.class, () -> client.completePending());
        assertTrue(ex.getMessage().contains("Failed to complete pending FTP command"));
    }

    @Test
    void testDisconnectWhenConnected() throws Exception {
        when(mockFtpClient.isConnected()).thenReturn(true);
        when(mockFtpClient.logout()).thenReturn(true);
        doNothing().when(mockFtpClient).disconnect();

        assertDoesNotThrow(() -> client.disconnect());

        verify(mockFtpClient).logout();
        verify(mockFtpClient).disconnect();
    }

    @Test
    void testDisconnectWhenNotConnected() throws Exception {
        when(mockFtpClient.isConnected()).thenReturn(false);

        assertDoesNotThrow(() -> client.disconnect());
        verify(mockFtpClient, never()).logout();
        verify(mockFtpClient, never()).disconnect();
    }
}
