package br.com.datastreambrasil.kafka.connector.ftp.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileOffsetTest {

    @Test
    void testFileOffsetCreation() {
        FileOffset offset = new FileOffset("test.txt", "abc123", 100L, 1234567890L, 5000L);

        assertEquals("test.txt", offset.getFilename());
        assertEquals("abc123", offset.getFileHash());
        assertEquals(100L, offset.getLineNumber());
        assertEquals(1234567890L, offset.getLastModified());
        assertEquals(5000L, offset.getFileSize());
    }

    @Test
    void testToMap() {
        FileOffset offset = new FileOffset("test.txt", "abc123", 100L, 1234567890L, 5000L);

        Map<String, Object> map = offset.toMap();

        assertEquals("test.txt", map.get("filename"));
        assertEquals("abc123", map.get("file_hash"));
        assertEquals(100L, map.get("line_number"));
        assertEquals(1234567890L, map.get("last_modified"));
        assertEquals(5000L, map.get("file_size"));
    }

    @Test
    void testFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("filename", "test.txt");
        map.put("file_hash", "abc123");
        map.put("line_number", 100L);
        map.put("last_modified", 1234567890L);
        map.put("file_size", 5000L);

        FileOffset offset = FileOffset.fromMap(map);

        assertNotNull(offset);
        assertEquals("test.txt", offset.getFilename());
        assertEquals("abc123", offset.getFileHash());
        assertEquals(100L, offset.getLineNumber());
        assertEquals(1234567890L, offset.getLastModified());
        assertEquals(5000L, offset.getFileSize());
    }

    @Test
    void testFromMapWithNumbers() {
        Map<String, Object> map = new HashMap<>();
        map.put("filename", "test.txt");
        map.put("file_hash", "abc123");
        map.put("line_number", 100); // Integer instead of Long
        map.put("last_modified", 1234567890); // Integer instead of Long
        map.put("file_size", 5000); // Integer instead of Long

        FileOffset offset = FileOffset.fromMap(map);

        assertNotNull(offset);
        assertEquals(100L, offset.getLineNumber());
        assertEquals(1234567890L, offset.getLastModified());
        assertEquals(5000L, offset.getFileSize());
    }

    @Test
    void testFromMapNull() {
        assertNull(FileOffset.fromMap(null));
        assertNull(FileOffset.fromMap(new HashMap<>()));
    }

    @Test
    void testIsSameFile() {
        FileOffset offset1 = new FileOffset("test.txt", "abc123", 100L, 1234567890L, 5000L);
        FileOffset offset2 = new FileOffset("test.txt", "abc123", 200L, 9999999999L, 5000L);
        FileOffset offset3 = new FileOffset("test.txt", "different", 100L, 1234567890L, 5000L);
        FileOffset offset4 = new FileOffset("test.txt", "abc123", 100L, 1234567890L, 6000L);

        assertTrue(offset1.isSameFile(offset2)); // Same hash and size, different line number
        assertFalse(offset1.isSameFile(offset3)); // Different hash
        assertFalse(offset1.isSameFile(offset4)); // Different size
        assertFalse(offset1.isSameFile(null));
    }

    @Test
    void testEquals() {
        FileOffset offset1 = new FileOffset("test.txt", "abc123", 100L, 1234567890L, 5000L);
        FileOffset offset2 = new FileOffset("test.txt", "abc123", 100L, 1234567890L, 5000L);
        FileOffset offset3 = new FileOffset("test.txt", "abc123", 200L, 1234567890L, 5000L);

        assertEquals(offset1, offset2);
        assertNotEquals(offset1, offset3);
        assertNotEquals(offset1, null);
        assertNotEquals(offset1, "string");
    }

    @Test
    void testHashCode() {
        FileOffset offset1 = new FileOffset("test.txt", "abc123", 100L, 1234567890L, 5000L);
        FileOffset offset2 = new FileOffset("test.txt", "abc123", 100L, 1234567890L, 5000L);

        assertEquals(offset1.hashCode(), offset2.hashCode());
    }

    @Test
    void testToString() {
        FileOffset offset = new FileOffset("test.txt", "abc123", 100L, 1234567890L, 5000L);

        String str = offset.toString();

        assertTrue(str.contains("test.txt"));
        assertTrue(str.contains("abc123"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("1234567890"));
        assertTrue(str.contains("5000"));
    }
}
