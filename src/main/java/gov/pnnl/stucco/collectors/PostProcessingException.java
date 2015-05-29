package gov.pnnl.stucco.collectors;

/**
 * Exception type for collector post-processing.
 * @author d3e145
 */
public class PostProcessingException extends Exception {

    public PostProcessingException() {
    }

    public PostProcessingException(String message) {
        super(message);
    }

    public PostProcessingException(Throwable cause) {
        super(cause);
    }

    public PostProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PostProcessingException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
