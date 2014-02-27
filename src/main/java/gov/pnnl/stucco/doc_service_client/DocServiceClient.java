package gov.pnnl.stucco.doc_service_client;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Client to add and get objects to and from a document service
 */
public class DocServiceClient {

    // IP address or host name
    private String host = "localhost";
    
    // TCP port number
    private int port = 8118;

    /**
     * Default constructor 
     */
    public DocServiceClient()
    {}
    
    /**
     * Constructs client with specified connection information
     * @param host the IP address or host name
     * @param port the TCP port number
     */
    public DocServiceClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * Constructs client with specified connection information
     * @param config options that contain information on client connection to server
     */
    public DocServiceClient(Map<String, Object> config) {
        if( config != null) {             
            this.host = (String) config.get("/stucco/document-service/host");
            String portStr = (String) config.get("/stucco/document-service/port");
            this.port = Integer.parseInt(portStr);
        }
    }

    /**
     * Stores document in the document service
     * @param doc the document to store
     * @return Document ID
     */
    public String store(DocumentObject doc) throws DocServiceException {
        return store(doc, "");
    }

    /**
     * Convenience method to allow caller to store String directly.
     * @param text the text of the document to store
     * @return Document ID
     * @throws DocServiceException
     */
    public String store(String text) throws DocServiceException {
        return store(new DocumentObject(text));
    }
    
    /**
     * Convenience method to allow caller to store bytes directly and specify
     * the content-type
     * @param text the text of the document to store
     * @param contentType 
     * @return Document ID
     * @throws DocServiceException
     */
    public String store(byte[] bytes, String contentType) throws DocServiceException {
        return store(new DocumentObject(bytes, contentType));
    }
    
    /**
     * Convenience method to allow caller to store String directly and specify
     * the content-type
     * @param text the text of the document to store
     * @param contentType 
     * @return Document ID
     * @throws DocServiceException
     */
    public String store(String text, String contentType) throws DocServiceException {
        return store(new DocumentObject(text, contentType));
    }
    
    /**
     * Stores document in the document service and specifies an ID to use
     * @param doc the document to store
     * @param id the document ID
     * @return Document ID
     */
    public String store(DocumentObject doc, String id) throws DocServiceException {
        String idFromServer;
        try {
            InputStream response = HttpHelper.put(makeURL(id), doc.getContentType(), doc.getDataAsBytes());
            idFromServer = getId(IOUtils.toString(response));
        } catch (IOException e) {
            throw new DocServiceException("Cannot store to document server", e);
        } catch (JSONException e) {
            throw new DocServiceException("Cannot store to document server", e);
        }
        return idFromServer;
    }
    
    /**
     * Fetches document from the document service
     * @param id the id of document to fetch
     * @return Document
     * @throws IOException
     */
    public DocumentObject fetch(String id) throws DocServiceException{
        DocumentObject doc;
        try {
            InputStream stream = HttpHelper.get(makeURL(id));
            doc = new DocumentObject(stream);
        } catch (IOException e) {
            throw new DocServiceException("Cannot fetch from document server", e);
        }
        return doc;
    }
    
    /**
     * Gets document ID from server response
     * @param serverResponse
     * @return Document ID
     * @throws JSONException
     */
    private String getId(String serverResponse) throws JSONException {
        JSONObject json = new JSONObject(serverResponse);
        return json.get("key").toString();
    }
    
    /**
     * Makes a URL given a document ID
     * @param id the document ID
     * @return URL for the client to connect to
     * @throws MalformedURLException
     */
    private URL makeURL(String id) throws MalformedURLException  {
        String urlString = "http://" + host + ":" + Integer.toString(port) + "/document";
        if (!id.isEmpty()) {
            urlString = urlString + "/" + id;
        }
        return new URL(urlString);
    }
}   
    
