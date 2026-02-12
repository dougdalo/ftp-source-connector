package br.com.datastreambrasil.kafka.connector.ftp.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the processing state of a file for offset management.
 * This allows the connector to resume from the exact line where it left off.
 */
public class FileOffset {
    private final String filename;
    private final String fileHash;
    private final long lineNumber;
    private final long lastModified;
    private final long fileSize;

    public FileOffset(String filename, String fileHash, long lineNumber, long lastModified, long fileSize) {
        this.filename = filename;
        this.fileHash = fileHash;
        this.lineNumber = lineNumber;
        this.lastModified = lastModified;
        this.fileSize = fileSize;
    }

    public String getFilename() {
        return filename;
    }

    public String getFileHash() {
        return fileHash;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getFileSize() {
        return fileSize;
    }

    /**
     * Convert to Map for Kafka Connect offset storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("filename", filename);
        map.put("file_hash", fileHash);
        map.put("line_number", lineNumber);
        map.put("last_modified", lastModified);
        map.put("file_size", fileSize);
        return map;
    }

    /**
     * Create from Kafka Connect offset storage Map
     */
    public static FileOffset fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        return new FileOffset(
            (String) map.get("filename"),
            (String) map.get("file_hash"),
            ((Number) map.getOrDefault("line_number", 0L)).longValue(),
            ((Number) map.getOrDefault("last_modified", 0L)).longValue(),
            ((Number) map.getOrDefault("file_size", 0L)).longValue()
        );
    }

    /**
     * Check if this represents the same file (hash and size match)
     */
    public boolean isSameFile(FileOffset other) {
        if (other == null) {
            return false;
        }
        return Objects.equals(this.fileHash, other.fileHash) &&
               this.fileSize == other.fileSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileOffset that = (FileOffset) o;
        return lineNumber == that.lineNumber &&
               lastModified == that.lastModified &&
               fileSize == that.fileSize &&
               Objects.equals(filename, that.filename) &&
               Objects.equals(fileHash, that.fileHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, fileHash, lineNumber, lastModified, fileSize);
    }

    @Override
    public String toString() {
        return "FileOffset{" +
               "filename='" + filename + '\'' +
               ", fileHash='" + fileHash + '\'' +
               ", lineNumber=" + lineNumber +
               ", lastModified=" + lastModified +
               ", fileSize=" + fileSize +
               '}';
    }
}
