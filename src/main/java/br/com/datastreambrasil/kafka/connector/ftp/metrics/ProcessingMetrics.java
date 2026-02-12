package br.com.datastreambrasil.kafka.connector.ftp.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects metrics for file processing
 */
public class ProcessingMetrics {
    private final AtomicLong totalFilesProcessed = new AtomicLong(0);
    private final AtomicLong totalLinesProcessed = new AtomicLong(0);
    private final AtomicLong totalBytesProcessed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalValidationErrors = new AtomicLong(0);

    private volatile long currentFileStartTime = 0;
    private volatile long currentFileLinesProcessed = 0;
    private volatile String currentFileName = null;

    public void startFile(String filename) {
        this.currentFileName = filename;
        this.currentFileStartTime = System.currentTimeMillis();
        this.currentFileLinesProcessed = 0;
    }

    public void endFile() {
        totalFilesProcessed.incrementAndGet();
        this.currentFileName = null;
        this.currentFileStartTime = 0;
        this.currentFileLinesProcessed = 0;
    }

    public void incrementLines(long count) {
        totalLinesProcessed.addAndGet(count);
        currentFileLinesProcessed += count;
    }

    public void incrementBytes(long bytes) {
        totalBytesProcessed.addAndGet(bytes);
    }

    public void incrementErrors() {
        totalErrors.incrementAndGet();
    }

    public void incrementValidationErrors() {
        totalValidationErrors.incrementAndGet();
    }

    // Getters
    public long getTotalFilesProcessed() {
        return totalFilesProcessed.get();
    }

    public long getTotalLinesProcessed() {
        return totalLinesProcessed.get();
    }

    public long getTotalBytesProcessed() {
        return totalBytesProcessed.get();
    }

    public long getTotalErrors() {
        return totalErrors.get();
    }

    public long getTotalValidationErrors() {
        return totalValidationErrors.get();
    }

    public long getCurrentFileStartTime() {
        return currentFileStartTime;
    }

    public long getCurrentFileLinesProcessed() {
        return currentFileLinesProcessed;
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public long getCurrentFileDurationMs() {
        return currentFileStartTime > 0 ? System.currentTimeMillis() - currentFileStartTime : 0;
    }

    public double getCurrentFileLinesPerSecond() {
        long duration = getCurrentFileDurationMs();
        if (duration == 0) return 0;
        return (currentFileLinesProcessed * 1000.0) / duration;
    }

    public void reset() {
        totalFilesProcessed.set(0);
        totalLinesProcessed.set(0);
        totalBytesProcessed.set(0);
        totalErrors.set(0);
        totalValidationErrors.set(0);
        currentFileStartTime = 0;
        currentFileLinesProcessed = 0;
        currentFileName = null;
    }

    @Override
    public String toString() {
        return "ProcessingMetrics{" +
               "totalFilesProcessed=" + totalFilesProcessed +
               ", totalLinesProcessed=" + totalLinesProcessed +
               ", totalBytesProcessed=" + totalBytesProcessed +
               ", totalErrors=" + totalErrors +
               ", totalValidationErrors=" + totalValidationErrors +
               ", currentFileName='" + currentFileName + '\'' +
               ", currentFileDurationMs=" + getCurrentFileDurationMs() +
               ", currentFileLinesProcessed=" + currentFileLinesProcessed +
               '}';
    }
}
