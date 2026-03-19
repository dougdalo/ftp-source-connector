package br.com.datastreambrasil.kafka.connector.ftp.retry;

/**
 * Configuration for retry behavior
 */
public class RetryConfig {
    private final int maxAttempts;
    private final long initialBackoffMs;
    private final long maxBackoffMs;
    private final double backoffMultiplier;

    public RetryConfig(int maxAttempts, long initialBackoffMs, long maxBackoffMs, double backoffMultiplier) {
        this.maxAttempts = maxAttempts;
        this.initialBackoffMs = initialBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    public static RetryConfig defaultConfig() {
        return new RetryConfig(3, 1000L, 30000L, 2.0);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getInitialBackoffMs() {
        return initialBackoffMs;
    }

    public long getMaxBackoffMs() {
        return maxBackoffMs;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public long calculateBackoff(int attempt) {
        if (attempt <= 0) {
            return 0;
        }
        long backoff = (long) (initialBackoffMs * Math.pow(backoffMultiplier, attempt - 1));
        return Math.min(backoff, maxBackoffMs);
    }
}
