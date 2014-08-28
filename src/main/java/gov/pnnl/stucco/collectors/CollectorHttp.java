package gov.pnnl.stucco.collectors;

import gov.pnnl.stucco.utilities.CollectorMetadata;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for collectors making HTTP or HTTPS requests.
 */
public abstract class CollectorHttp extends CollectorAbstractBase {

    protected static final Logger logger = LoggerFactory.getLogger(CollectorHttp.class);
    
    /** Number of milliseconds to allow for making a connection. */
    private static final int TIMEOUT = 15 * 1000;
    
    /** Collection metadata as singleton. */
    protected static CollectorMetadata metadata = CollectorMetadata.getInstance();

    /** URI from which we are obtaining content*/
    protected String m_URI;

    
    public CollectorHttp(Map<String, String> configData) {
        super(configData);
        
        m_URI = configData.get("source-URI");  // should probably encode the URI here in-case there are weird characters URLEncoder.encode(URI, "UTF-8");
        m_metadata.put("sourceUrl", m_URI);
    }
    
    public String getURL() {
        return m_URI;
    }

    /** Writes the current content to a temp file that can be inspected post-run. */
    protected void debugSaveContent(String uri) {
        try {
            if (m_rawContent != null) {
                String prefix = debugGenerateFilePrefix(uri);
                Path tempFile = Files.createTempFile(prefix, null);
                logger.info("Writing {}", tempFile);
                Files.write(tempFile, m_rawContent);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Converts a URI into a prefix for a temp file. */
    private String debugGenerateFilePrefix(String uri) {
        // Find the last path element
        String[] token = uri.trim().split("/");
        String last = token[token.length - 1];
        
        // Strip out any non-word characters
        String prefix = last.replaceAll("\\W", "");
        return prefix;
    }

    /** 
     * Makes a conditional HTTP request, using stored metadata.
     * 
     * @param httpRequestMethod  HTTP request such as "GET" or "HEAD"
     * @param uri                HTTP target URI
     */
    protected HttpURLConnection makeConditionalRequest(String httpRequestMethod, String uri)
            throws IOException {
        // Set up request
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(httpRequestMethod);
        connection.setReadTimeout(TIMEOUT);

        // Add metadata hints (which may get ignored):

        // Timestamp
        long lastTime = metadata.getTimestamp(uri).getTime();
        connection.setIfModifiedSince(lastTime);

        // ETag
        String lastETag = metadata.getETag(uri);
        if (!lastETag.equals(CollectorMetadata.NONE)) {
            connection.setRequestProperty("If-None-Match", lastETag);
        }


        // Make request
        logger.info("{} - {}", uri, httpRequestMethod);
        connection.connect();

        return connection;
    }

    /** 
     * Gets a potentially edited HTTP response code for a connection. The code
     * will be the same as from the HttpURLConnection.getResponseCode() with 
     * an extra check as follows. If the response is HTTP_OK, then we check the
     * timestamp and ETag, to see if we're modified. If not, this method will 
     * return HTTP_NOT_MODIFIED instead of HTTP_OK.
     */
    protected int getEnhancedResponseCode(HttpURLConnection connection) throws IOException {
        String requestMethod = connection.getRequestMethod();
        int responseCode = connection.getResponseCode();
        
        // Get the possibly redirected URL
        URL url = connection.getURL();
        String uri = url.toExternalForm();
        
        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                // Connected OK
    
                // Check timestamp
                long now = System.currentTimeMillis();
                long lastModified = connection.getHeaderFieldDate("Last-Modified", now);
                long lastTime = metadata.getTimestamp(uri).getTime();
                if (lastModified <= lastTime) {
                    // Page is unmodified
                    responseCode = HttpURLConnection.HTTP_NOT_MODIFIED;
    
                    // Log it
                    Date lastModifiedDate = new Date(lastModified);
                    logger.info("{} - Already retrieved since Last-Modified: {}", uri, lastModifiedDate);
                }
                else {
                    // Check hash
                    String lastETag = metadata.getETag(uri);
                    String eTag = connection.getHeaderField("ETag");
                    if (eTag != null  &&  !lastETag.equals(CollectorMetadata.NONE)  &&  eTag.equals(lastETag)) {
                        // Page is unmodified
                        responseCode = HttpURLConnection.HTTP_NOT_MODIFIED;
    
                        // Log it
                        logger.info("{} - Already retrieved with ETag: {}", uri, eTag);
                    }
                    else {
                        logger.info("{} - 200 OK", uri);
                    }
                }
                break;
                
            case HttpURLConnection.HTTP_NOT_MODIFIED:
                logger.info("{} - 304 Not Modified", uri);
                break;
                
            case HttpURLConnection.HTTP_NOT_FOUND:
                // Common case that we may want to know about
                logger.warn("{} - 404 Not Found", uri);
                break;

            default:
                // Any other response may also be something we want to know about
                logger.warn("{} - {}", uri, responseCode);
                break;
        }
        
        return responseCode;
    }

    /** 
     * Performs a header-based pre-retrieval check on a URI to see if we need 
     * to get its content. 
     */
    protected boolean needToGet(String uri) throws IOException {
        HttpURLConnection connection = makeConditionalRequest("HEAD", uri);
        int responseCode = getEnhancedResponseCode(connection);
        
        switch (responseCode) {
            // Codes where there's no point in making a GET request
            case HttpURLConnection.HTTP_NO_CONTENT:
            case HttpURLConnection.HTTP_MULT_CHOICE:
            case HttpURLConnection.HTTP_SEE_OTHER:
            case HttpURLConnection.HTTP_USE_PROXY:
            case HttpURLConnection.HTTP_NOT_MODIFIED:
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_PAYMENT_REQUIRED:
            case HttpURLConnection.HTTP_FORBIDDEN:
            case HttpURLConnection.HTTP_NOT_FOUND:
            case HttpURLConnection.HTTP_BAD_METHOD:
            case HttpURLConnection.HTTP_NOT_ACCEPTABLE:
            case HttpURLConnection.HTTP_PROXY_AUTH:
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_LENGTH_REQUIRED:
            case HttpURLConnection.HTTP_ENTITY_TOO_LARGE:
            case HttpURLConnection.HTTP_REQ_TOO_LONG:
            case HttpURLConnection.HTTP_UNSUPPORTED_TYPE:
            case HttpURLConnection.HTTP_UNAVAILABLE:
            case HttpURLConnection.HTTP_VERSION:
                return false;
            
            // Codes where the GET request might work
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_CREATED:
            case HttpURLConnection.HTTP_ACCEPTED:
            case HttpURLConnection.HTTP_NOT_AUTHORITATIVE:
            case HttpURLConnection.HTTP_RESET:
            case HttpURLConnection.HTTP_PARTIAL:
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
            case HttpURLConnection.HTTP_NOT_IMPLEMENTED:
            case HttpURLConnection.HTTP_BAD_GATEWAY:
            case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                return true;
            
            // Codes where we're not sure, so try the GET
            case HttpURLConnection.HTTP_CONFLICT:
            case HttpURLConnection.HTTP_PRECON_FAILED:
            default:
                return true;
        }
    }

}