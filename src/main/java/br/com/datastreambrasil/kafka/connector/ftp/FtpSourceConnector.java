package br.com.datastreambrasil.kafka.connector.ftp;

/**
 * Backward-compatible entrypoint connector.
 *
 * <p>This class keeps the original connector.class used in production deployments while delegating to
 * the enhanced implementation.
 */
public class FtpSourceConnector extends FtpSourceConnectorEnhanced {
    // Intentionally empty: inherits all enhanced features while preserving the original class name.
}
