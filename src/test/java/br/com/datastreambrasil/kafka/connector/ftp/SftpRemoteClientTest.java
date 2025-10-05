package br.com.datastreambrasil.kafka.connector.ftp;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.Attributes;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SftpRemoteClientTest {

    private SftpClient mockSftp;
    private SshClient mockClient;
    private ClientSession mockSession;
    private SftpRemoteClient client;

    @BeforeEach
    void setup() {
        mockSftp = mock(SftpClient.class);
        mockClient = mock(SshClient.class);
        mockSession = mock(ClientSession.class);

        client = new SftpRemoteClient(Map.of(
                "ftp.host", "localhost",
                "ftp.port", "22",
                "ftp.username", "user",
                "ftp.password", "pass")) {
            private void injectMocks() {
                try {
                    var sftpField = SftpRemoteClient.class.getDeclaredField("sftp");
                    sftpField.setAccessible(true);
                    sftpField.set(this, mockSftp);

                    var sessionField = SftpRemoteClient.class.getDeclaredField("session");
                    sessionField.setAccessible(true);
                    sessionField.set(this, mockSession);

                    var clientField = SftpRemoteClient.class.getDeclaredField("client");
                    clientField.setAccessible(true);
                    clientField.set(this, mockClient);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public List<String> listFiles(String directory, String pattern) throws Exception {
                injectMocks();
                return super.listFiles(directory, pattern);
            }

            @Override
            public InputStream retrieveFileStream(String filePath) throws Exception {
                injectMocks();
                return super.retrieveFileStream(filePath);
            }

            @Override
            public void moveFile(String sourcePath, String destinationPath) throws Exception {
                injectMocks();
                super.moveFile(sourcePath, destinationPath);
            }

            @Override
            public void deleteFile(String path) throws Exception {
                injectMocks();
                super.deleteFile(path);
            }

            @Override
            public void writeTextFile(String path, String contents, Charset charset) throws Exception {
                injectMocks();
                super.writeTextFile(path, contents, charset);
            }

            @Override
            public void disconnect() {
                injectMocks();
                super.disconnect();
            }
        };
    }

    @Test
    void testListFilesMatchingPattern() throws Exception {
        Attributes attrs = mock(Attributes.class);
        when(attrs.isDirectory()).thenReturn(false);

        DirEntry entry = mock(DirEntry.class);
        when(entry.getFilename()).thenReturn("data.csv");
        when(entry.getAttributes()).thenReturn(attrs);

        when(mockSftp.readDir("/files")).thenReturn(List.of(entry));

        List<String> result = client.listFiles("/files", ".*\\.csv");

        assertEquals(1, result.size());
        assertEquals("/files/data.csv", result.get(0));
    }

    @Test
    void testListFilesNoMatch() throws Exception {
        Attributes attrs = mock(Attributes.class);
        when(attrs.isDirectory()).thenReturn(false);

        DirEntry entry = mock(DirEntry.class);
        when(entry.getFilename()).thenReturn("not-matching.txt");
        when(entry.getAttributes()).thenReturn(attrs);

        when(mockSftp.readDir("/files")).thenReturn(List.of(entry));

        List<String> result = client.listFiles("/files", ".*\\.csv");

        assertTrue(result.isEmpty());
    }

    @Test
    void testRetrieveFileStream() throws Exception {
        String content = "id,name\n1,Alice";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        when(mockSftp.read("/files/test.csv")).thenReturn(is);

        InputStream result = client.retrieveFileStream("/files/test.csv");

        assertNotNull(result);
        assertEquals(content, new String(result.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void testMoveFile() throws Exception {
        doNothing().when(mockSftp).rename("/stage/file.csv", "/archive/file.csv");

        assertDoesNotThrow(() -> client.moveFile("/stage/file.csv", "/archive/file.csv"));
        verify(mockSftp).rename("/stage/file.csv", "/archive/file.csv");
    }

    @Test
    void testDeleteFile() throws Exception {
        doNothing().when(mockSftp).remove("/stage/file.csv");

        assertDoesNotThrow(() -> client.deleteFile("/stage/file.csv"));
        verify(mockSftp).remove("/stage/file.csv");
    }

    @Test
    void testWriteTextFile() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(mockSftp.write("/archive/summary.txt")).thenReturn(out);

        assertDoesNotThrow(() -> client.writeTextFile("/archive/summary.txt", "content", StandardCharsets.UTF_8));
        assertEquals("content", new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testDisconnectWithAllResources() throws Exception {
        doNothing().when(mockSftp).close();
        doNothing().when(mockSession).close();
        doNothing().when(mockClient).stop();

        assertDoesNotThrow(() -> client.disconnect());

        verify(mockSftp).close();
        verify(mockSession).close();
        verify(mockClient).stop();
    }

    @Test
    void testDisconnectWithNulls() {
        SftpRemoteClient nullClient = new SftpRemoteClient(Map.of(
                "ftp.host", "localhost",
                "ftp.port", "22",
                "ftp.username", "user",
                "ftp.password", "pass"));

        assertDoesNotThrow(nullClient::disconnect);
    }

    @Test
    void testListFilesThrowsException() throws Exception {
        when(mockSftp.readDir("/files")).thenThrow(new RuntimeException("boom"));

        Exception e = assertThrows(RuntimeException.class, () -> client.listFiles("/files", ".*"));
        assertEquals("boom", e.getMessage());
    }
}
