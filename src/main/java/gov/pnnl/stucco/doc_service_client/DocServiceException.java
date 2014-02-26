package gov.pnnl.stucco.doc_service_client;

public class DocServiceException extends Exception {

    /**
     * Generated UID
     */
    private static final long serialVersionUID = 6931699502909335146L;
    
    public DocServiceException(Throwable e) {
        super(e);
    }
    
    public DocServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
