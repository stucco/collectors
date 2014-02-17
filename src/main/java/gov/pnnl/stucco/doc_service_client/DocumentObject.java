package gov.pnnl.stucco.doc_service_client;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;
import org.apache.commons.io.IOUtils;

/**
 * Stores various types of objects as a generalized document
 */
public class DocumentObject {
    
    // Specifies the content type of the document
    private String contentType = "application/octet-stream";
    
    // The document is stored as raw bytes
    private byte[] bytes;
    
    /**
     * Constructs a DocumentObject from an InputStream
     * @param stream
     * @throws IOException
     */
    public DocumentObject(InputStream stream) throws IOException {
        bytes = IOUtils.toByteArray(stream);
        contentType = "application/octet-stream";
    }
    
    /**
     * Constructs a DocumentObject from a JSONObject
     * @param json
     */
    public DocumentObject(JSONObject json) {
        bytes = json.toString().getBytes();
        contentType = "application/json";
    }
    
    /**
     * Constructs a DocumentObject from a String
     * @param text
     */
    public DocumentObject(String text) {
        bytes = text.getBytes();
        contentType = "text/plain";
    }
    
    /**
     * Gets the contents of the document as raw bytes
     * @return bytes
     */
    public byte[] getDataAsBytes() {
        return bytes;
    }
    
    /**
     * Gets the contents of the document as a String
     * @return string
     */
    public String getDataAsString() {
        return new String(bytes);
    }
    
    /**
     * Gets the Document's content type
     * @return content type
     */
    public String getContentType() {
        return contentType;
    }   
    
    /**
     * Sets the Document's content type
     * @return
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
