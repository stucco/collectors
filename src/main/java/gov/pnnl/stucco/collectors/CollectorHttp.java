package gov.pnnl.stucco.collectors;


import gov.pnnl.stucco.utilities.FeedCollectionStatus;
import gov.pnnl.stucco.utilities.MutableUriMetadata;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for collectors making HTTP or HTTPS requests.
 */
public abstract class CollectorHttp extends CollectorAbstractBase {

    /** Configuration data key for a collector's URI. */
    public static final String SOURCE_URI = "source-URI";

    /** Configuration data key for scheduler startup behavior. */
    public static final String NOW_COLLECT_KEY = "now-collect";
    
    /** Configuration data key for a regex for finding tabbed subpages. */
    public static final String TAB_REGEX_KEY = "tab-regex";
    
    /** Configuration data key for robots.txt Crawl-delay. */
    public static final String CRAWL_DELAY_KEY = "crawl-delay";
    
    protected static final Logger logger = LoggerFactory.getLogger(CollectorHttp.class);
    
    /** Number of milliseconds to allow for making a connection. */
    private static final int TIMEOUT = 60 * 1000;

    /** URI from which we are obtaining content*/
    protected String sourceUri;

    /** UUID assigned to the collected content. */
    protected String docId = "";
    
    /** Content of the message that got sent (empty if there wasn't one). */
    protected byte[] messageContent = new byte[0];

    /** Whether to force collection, even if a duplicate is encountered. */
    private boolean forcedCollection = false;
    
    /** Status of our collection effort. */
    private FeedCollectionStatus collectionStatus = FeedCollectionStatus.GO;
    
    
    public CollectorHttp(Map<String, String> configData) {
        super(configData);
        
        sourceUri = configData.get(SOURCE_URI);  // should probably encode the URI here in-case there are weird characters URLEncoder.encode(URI, "UTF-8");
        messageMetadata.put("sourceUrl", sourceUri);
        
        String nowCollect = configData.get(NOW_COLLECT_KEY);
        forcedCollection = "all".equalsIgnoreCase(nowCollect);
    }
    
    final public String getDocId() {
        return docId;
    }
    
    final public String getURL() {
        return sourceUri;
    }
    
    final public byte[] getMessageContent() {
        return messageContent;
    }
    
    final public boolean isForcedCollection() {
        return forcedCollection;
    }

    final public FeedCollectionStatus getCollectionStatus() {
        return collectionStatus;
    }

    final public void setCollectionStatus(FeedCollectionStatus status) {
        collectionStatus = status;
    }

    /** Writes the current content to a temp file that can be inspected post-run. */
    protected void debugSaveContent(String uri) {
        try {
            if (rawContent != null) {
                String prefix = debugGenerateFilePrefix(uri);
                Path tempFile = Files.createTempFile(prefix, null);
                logger.info("Writing {}", tempFile);
                Files.write(tempFile, rawContent);
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
     * Makes an HTTP request, using stored metadata. Depending on the 
     * circumstances, the request may be conditional.
     * 
     * <p> The HttpURLConnection will handle most redirects. However, it 
     * intentionally won't redirect between HTTP and HTTPS, so we handle
     * up to 1 redirect to allow for that.
     * 
     * @param httpRequestMethod  
     * HTTP request such as "GET" or "HEAD"
     * 
     * @param uri                
     * HTTP target URI
     */
    protected HttpURLConnection makeRequest(String httpRequestMethod, String uri) throws IOException {
        int maxRedirectCount = 1;
        HttpURLConnection connection = makeRequest(httpRequestMethod, uri, maxRedirectCount);
        return connection;
    }
    
    /** 
     * Makes an HTTP request, using stored metadata. Depending on the 
     * circumstances, the request may be conditional.
     * 
     * <p> The HttpURLConnection will handle most redirects; we handle the rest.
     * 
     * @param httpRequestMethod  
     * HTTP request such as "GET" or "HEAD"
     * 
     * @param uri                
     * HTTP target URI
     * 
     * @param maxRedirectCount   
     * Maximum number of redirects that we handle (that is, not counting 
     * redirects inside HttpURLConnection).
     */
    private HttpURLConnection makeRequest(String httpRequestMethod, String uri, int maxRedirectCount)
            throws IOException {
        
        pauseCrawlDelay();

        // Set up request
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(httpRequestMethod);
        connection.setReadTimeout(TIMEOUT);

        boolean isConditional = !isForcedCollection();
        if (isConditional) {
            // Add metadata hints (which may get ignored anyway):

            // Timestamp
            long lastTime = pageMetadata.getTimestamp(uri).getTime();
            connection.setIfModifiedSince(lastTime);

            // ETag
            String lastETag = pageMetadata.getETag(uri);
            if (!lastETag.equals(MutableUriMetadata.NONE)) {
                connection.setRequestProperty("If-None-Match", lastETag);
            }
        }

        // Make request
        logger.info("{} - {}", uri, httpRequestMethod);
        connection.connect();

        connection = redirectIfNeeded(connection, httpRequestMethod, maxRedirectCount);

        return connection;
    }
   
    /** 
     * Pauses the thread for the number of seconds specified in the Crawl-delay.
     */
    private void pauseCrawlDelay() {
        try {
            // Default pause of 1 second
            long pauseMillis = 1000;
            
            // Look value up in config
            String crawlDelayStr = collectorConfigData.get(CRAWL_DELAY_KEY);
            if (crawlDelayStr != null) {
                try {
                    // Found it, so parse the number of seconds
                    float crawlDelay = Float.parseFloat(crawlDelayStr);
                    pauseMillis = Math.round(crawlDelay * 1000);
                }
                catch (NumberFormatException nfe) {
                    // Didn't parse correctly, so just use default
                }
            }
            
            // Pause
            Thread.sleep(pauseMillis);
        } 
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /** 
     * Checks a connection's response code and, if necessary, redirects to a 
     * new connection. 
     * 
     * @param connection
     * The current HTTP connection, already connected.
     * 
     * @param httpRequestMethod  
     * HTTP request such as "GET" or "HEAD"
     * 
     * @param maxRedirectCount   
     * Maximum number of redirects allowed. If <= 0, this method just returns
     * the same connection.
     * 
     * @return
     * A potentially redirected connection
     */
    private HttpURLConnection redirectIfNeeded(HttpURLConnection connection, String httpRequestMethod, 
                                               int maxRedirectCount) throws IOException {
        if (maxRedirectCount > 0) {
            int responseCode = connection.getResponseCode();
            switch (responseCode) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                    String url = connection.getHeaderField("Location");
                    logger.info("Redirect response: {} -> {}", this.sourceUri, url);
                    connection = makeRequest(httpRequestMethod, url, maxRedirectCount - 1);
                    break;

                default:
                    // Ignore other cases
                    break;
            }
        }
        
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
                long lastTime = pageMetadata.getTimestamp(uri).getTime();
                if (lastModified <= lastTime) {
                    // Page is unmodified
                    responseCode = HttpURLConnection.HTTP_NOT_MODIFIED;
    
                    // Log it
                    Date lastModifiedDate = new Date(lastModified);
                    logger.info("{} - Already retrieved since Last-Modified: {}", uri, lastModifiedDate);
                }
                else {
                    // Check hash
                    String lastETag = pageMetadata.getETag(uri);
                    String eTag = connection.getHeaderField("ETag");
                    if (eTag != null  &&  !lastETag.equals(MutableUriMetadata.NONE)  &&  eTag.equals(lastETag)) {
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
     * 
     * <p> NOTE: This method is low-level, and intentionally does not set feed 
     * collection status.
     */
    protected final boolean needToGet(String uri) throws IOException {
        HttpURLConnection connection = makeRequest("HEAD", uri);
        int responseCode = getEnhancedResponseCode(connection);
        
        switch (responseCode) {
            // Codes where there's no point in making a GET request
            case HttpURLConnection.HTTP_NO_CONTENT:
            case HttpURLConnection.HTTP_MULT_CHOICE:
            case HttpURLConnection.HTTP_SEE_OTHER:
            case HttpURLConnection.HTTP_NOT_MODIFIED:
            case HttpURLConnection.HTTP_USE_PROXY:
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

    /** Generates a UUID for the document being collected. */
    protected void assignDocId() {
        // Assign a document ID
        docId = UUID.randomUUID().toString();
    }

    /** Parses the entry URLs from the page content. */
    protected List<String> scrapeUrls(String currentUrl, String regEx) {
        List<String> urlList = new ArrayList<String>();
    
        try {
            // Convert bytes to String
            String page = new String(rawContent);
            
            // Prepare the regex to find the URLs
            Pattern pattern = Pattern.compile(regEx);
            Matcher matcher = pattern.matcher(page);
    
            URI source = new URI(currentUrl);
            
            // For each match of the regex
            while (matcher.find()) {
                // Grab the matching URL
                String match = getFirstCapture(matcher);
                
                // If it's a relative path, convert to absolute
                match = source.resolve(match).toString();
                
                // Save it (without duplicates)
                if (!urlList.contains(match)) {
                    urlList.add(match);
                }
            }
        } catch (URISyntaxException e) {
            logger.warn("Bad URI: {}", sourceUri);
            e.printStackTrace();
        }
        
        return urlList;
    }

    /** Gets the Matcher's first captured group (or null if nothing got captured). */
    private static String getFirstCapture(Matcher matcher) {
        // Check each capturing group
        int groupCount = matcher.groupCount();
        for (int i = 1; i <= groupCount; i++) {
            String group = matcher.group(i);
            
            if (group != null) {
                // Return the first one that captured anything
                return group;
            }
        }
        
        // Nothing captured
        return null;
    }

    /** Collects a list of URLs and sends a single aggregate message for them. */
    protected void collectAndAggregateUrls(List<String> urls) {
        // Start the entry message
        StringBuilder messageBuffer = new StringBuilder();
        logger.debug("Packaging multiple URLs for transport");
        
        // For each tab
        for (String url : urls) {
            // Create URL and collector
            collectorConfigData.put(SOURCE_URI, url);
            CollectorWebPageImpl tabCollector = new CollectorWebPageImpl(collectorConfigData);
            
            // The Stucco message will be handled by this entry collector instead of the tab collector
            tabCollector.setMessaging(false);
            
            // Try to collect the tab
            tabCollector.collect();
            
            String tabDocId = pageMetadata.getUuid(url);
            if (tabDocId != MutableUriMetadata.NONE) {
                // We have a record of this tab, meaning we just collected it or
                // we already had it. Either way we add it to the message.
                //
                // NOTE: For now, this is all we're checking. The software will
                // eventually need more robust exception handling.
                messageBuffer.append(tabDocId);
                messageBuffer.append(" ");
                messageBuffer.append(url);
                messageBuffer.append("\n");
            }
        }
        
        // Send the Stucco message
        rawContent = messageBuffer.toString().getBytes();
        messageSender.sendIdMessage(messageMetadata, rawContent);
    }

    /**
     * Gets a Collector suitable for an entry from an RSS (or other) feed.  
     * This determines if the entry has tabbed subpages based on whether 
     * a tab regex exists.
     */
    protected Collector getEntryCollector(String entryUrl) {
        collectorConfigData.put(SOURCE_URI, entryUrl);
        Collector entryCollector;
        
        String tabRegEx = collectorConfigData.get(TAB_REGEX_KEY);
        if (tabRegEx == null) {
            // Entry is a normal web page
            entryCollector = new CollectorWebPageImpl(collectorConfigData);
        }
        else {
            // Entry consists of multiple tabs
            entryCollector = new CollectorTabbedEntry(collectorConfigData);
        }
        
        return entryCollector;
    }
}
