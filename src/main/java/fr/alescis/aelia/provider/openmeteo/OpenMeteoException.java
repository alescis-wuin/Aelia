package fr.alescis.aelia.provider.openmeteo;

/**
 * Runtime exception used for remote Open-Meteo transport, payload and mapping failures.
 */
public class OpenMeteoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private static final int NO_STATUS_CODE = -1;

    private final int statusCode;
    private final boolean transientFailure;

    public OpenMeteoException(String message) {
        this(message, null, NO_STATUS_CODE, false);
    }

    public OpenMeteoException(String message, Throwable cause) {
        this(message, cause, NO_STATUS_CODE, true);
    }

    public OpenMeteoException(String message, int statusCode, boolean transientFailure) {
        this(message, null, statusCode, transientFailure);
    }

    public OpenMeteoException(String message, Throwable cause, int statusCode, boolean transientFailure) {
        super(message, cause);
        this.statusCode = statusCode;
        this.transientFailure = transientFailure;
    }

    public int statusCode() {
        return statusCode;
    }

    public boolean hasStatusCode() {
        return statusCode >= 100;
    }

    public boolean transientFailure() {
        return transientFailure;
    }
}
