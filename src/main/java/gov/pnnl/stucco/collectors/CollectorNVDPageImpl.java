package gov.pnnl.stucco.collectors;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

import gov.pnnl.stucco.doc_service_client.DocServiceException;
import gov.pnnl.stucco.doc_service_client.DocumentObject;
import gov.pnnl.stucco.utilities.CollectorMetadata;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class CollectorNVDPageImpl extends CollectorHttp {    
    
    /** Whether we will save the collected content to the document store. */
    protected boolean storing = true;
    
    /** Whether we will send a message for the collected content. */
    protected boolean messaging = true;

    /** 
     * constructor for obtaining the contents of a NVD webpage (consisting of multiple CVE records)
     * @param URI - where to get the contents on the web
     * @param configData - data from the configuration file (you need to know where to look for stuff)
     */
    public CollectorNVDPageImpl(Map<String, String> configData) {
        super(configData);
    }
    
    final public void setStoring(boolean flag) {
        storing = flag;
    }
    
    final public void setMessaging(boolean flag) {
        messaging = flag;
    }
    
    @Override
    public void collect() {
        try {      
            // break down the URI to see whether it's a web or file based
            URI u = new URI(sourceUri);
            String uriScheme = u.getScheme();
            String uriPath   = u.getPath();
            
            if(uriScheme == null) {
                uriScheme = "file";
            }
            
            // check on whether a file first
            if (uriScheme.equalsIgnoreCase("file")) {
                obtainFromFile(uriPath);
                processMultiRecords();
            } 
            else if (needToGet(sourceUri)) {
                if (obtainWebPage(sourceUri)) {
                    processMultiRecords();
                }
            }
            clean();           
        }
        catch (URISyntaxException e) {
            logger.error("Exception in parsing URI "+ sourceUri);
        }
        catch (IOException e) {
            logger.error("Exception raised while reading web page", e);
        } 
    }

    /**
     * processMultiRecord - after getting the primary (page or file) of all records
     * we pass the data off to the XMLextractor to do the heavy lifting and use events
     * as callbacks to process the individual records (i.e., sending them to RT and storing them)
     * 
     * @throws DocServiceException
     */
    private void processMultiRecords() {
        NVDXMLParser extractor = new NVDXMLParser();
        extractor.addListener(new NVDListener() 
        {
            public void recordFound(NVDEvent e) {
                byte[] src = e.getRecord();
                processRecord(src);
            }
        } );
        
        logger.info("Processing into single records");
        InputStream input = new ByteArrayInputStream(rawContent);
        extractor.parseForRecords(input);
    }
    
    /**
     * A record has been extracted from the XML stream and  we now need to send 
     * this one into the document store and a message to RT 
     * @param record - the XML record
     */
    private void processRecord(byte[] record) {
        try {
            rawContent = record;
            assignDocId();
            storeDocument();
            send();
            clean(); 
        }
        catch (DocServiceException e) {
            logger.error("Cannot send data", e);
        }
    }
    
    
    //  TODO: ISSUES TO DEAL WITH:  
    //       Authentication: Username, PW
    //       Cookies
    //       Encoding issues
    /**
     * Retrieves the webpage.
     *
     * @return Whether we got new content
     */
    protected boolean obtainWebPage(String uri) throws IOException
    {
        HttpURLConnection connection = makeConditionalRequest("GET", uri);
        int responseCode = getEnhancedResponseCode(connection);
        boolean isNewContent = (responseCode == HttpURLConnection.HTTP_OK);
        
        if (isNewContent) {
            // So far it seems new
            
            messageMetadata.put("contentType", connection.getHeaderField("Content-Type"));
            
            // Get the Last-Modified timestamp
            long now = System.currentTimeMillis();
            long time = connection.getHeaderFieldDate("Last-Modified", now);
            timestamp = new Date(time);
        
            // Get the ETag
            String eTag = connection.getHeaderField("ETag");
            if (eTag == null) {
                eTag = "";
            }
            
           // Get the content as a byte array, and compute its checksum
            byte[] content = null;
            try (
                    BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                    ByteArrayOutputStream out = new ByteArrayOutputStream()
            ) {
                // Get a chunk at a time 
                byte[] buffer = new byte[8192]; // 8K
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }

                content = out.toByteArray();
            }
            String checksum = CollectorMetadata.computeHash(content);
        
            
            // Update the metadata
            isNewContent = updatePageMetadata(uri, timestamp, eTag, checksum);
            String endUri = connection.getURL().toExternalForm();
            if (!uri.equalsIgnoreCase(endUri)) {
                // We got redirected, so save metadata for the end URL too
                updatePageMetadata(endUri, timestamp, eTag, checksum);
            }
            pageMetadata.save();
            
            if (isNewContent) {
                // Save the new content
                rawContent = content;
            }
            else {
                // Content isn't new
                logger.info("{} - SHA-1 unchanged", endUri);
                rawContent = null;
            }
            rawContent = isNewContent?  content : null;            
        }
        
        return isNewContent;
    }

    /** Updates the metadata for a URL after a successful GET. */
    private boolean updatePageMetadata(String url, Date timestamp, String eTag, String checksum) {
        // Timestamp
        pageMetadata.setTimestamp(url, timestamp);

        // ETag
        pageMetadata.setETag(url, eTag);
        
        // Update the SHA-1 checksum and see if it changed
        boolean isNewContent = pageMetadata.setHash(url, checksum);
        return isNewContent;
    }
    
    /**
     * obtain the contents from file
     * @param filename
     * @return
     */
    private byte[] obtainFromFile(String filename) {
        try {
            File contentFile = new File(filename);
            if (contentFile.isDirectory()) {
                logger.error(contentFile + "is a directory, not a file");
                throw new IllegalArgumentException(contentFile + "is a directory, not a file");
            }

            // Read the file
            rawContent = readFile(contentFile);
            timestamp = new Date();
        } catch (IOException e) {
            logger.error("Unable to collect '" + filename + "' because of IOException", e);
        }
        return rawContent;
    }

    /** 
     * Reads the contents of a file. 
     * @param file - file that is ready to be read
     * */
    private byte[] readFile(File file) throws IOException {
        int byteCount = (int) file.length();
        byte[] content = new byte[byteCount];
        DataInputStream in = new DataInputStream((new FileInputStream(file)));
        in.readFully(content);
        in.close();

        return content;
    }

    @Override
    public void clean() {
        rawContent = null;
    }
    
    /**
     * Stores the collected document to the document store.
     * 
     * @throws DocServiceException
     */
    protected void storeDocument() throws DocServiceException {
        if (storing) {
            // Send to document store
            String contentType = messageMetadata.get("contentType");
            DocumentObject doc = new DocumentObject(rawContent, contentType);
            docServiceClient.store(doc, docId);
        }
    }
    
    // Overridden to separate ID generation, document storage, and messaging
    @Override
    public void send() {
        if (messaging) {
            messageContent = docId.getBytes();
            messageSender.sendIdMessage(messageMetadata, messageContent);
        }
    }
    
    /** Test driver used during development. */
    static public void main(String[] args) {
        try {                
//            String url = "http://static.nvd.nist.gov/feeds/xml/cve/nvdcve-2.0-modified.xml";        // OK: HEAD conditional
//            String url = "http://geolite.maxmind.com/download/geoip/database/GeoIPCountryCSV.zip";  // OK: HEAD conditional
//            String url = "http://seclists.org/rss/fulldisclosure.rss";                              // OK: HEAD conditional
//            String url = "http://www.reddit.com/r/netsec/new.rss";                                  // FAIL: HEAD conditional or GET SHA-1, but 'ups', 'score', comments change ~10 seconds
//            String url = "http://blog.cmpxchg8b.com/feeds/posts/default";                           // OK: HEAD Last-Modified
            String url = "https://technet.microsoft.com/en-us/security/rss/bulletin";               // FAIL: RSS item order changes every time
//            String url = "http://metasploit.org/modules/";                                          // FAIL: 'csrf-token' changes every time
//            String url = "http://community.rapid7.com/community/metasploit/blog";                   // FAIL: IDs change every time
//            String url = "http://rss.packetstormsecurity.com/files/";                               // FAIL: 'utmn' changes every time
//            String url = "http://www.f-secure.com/exclude/vdesc-xml/latest_50.rss";                 // OK: HEAD Last-Modified
//            String url = "https://isc.sans.edu/rssfeed_full.xml";                                   // FAIL: HEAD Last-Modified, 'lastBuildDate' changes ~10 minutes
//            String url = "https://twitter.com/briankrebs";                                          // FAIL: Authenticity tokens change
//            String url = "http://www.mcafee.com/threat-intelligence/malware/latest.aspx";           // OK: GET SHA-1
//            String url = "http://about-threats.trendmicro.com/us/threatencyclopedia#malware";       // FAIL: GET SHA-1, but '__VIEWSTATE' and '__EVENTVALIDATION' change
//            String url = "https://cve.mitre.org/data/refs/refmap/source-BUGTRAQ.html";              // OK: GET SHA-1
//            String url = "https://isc.sans.edu/feeds/daily_sources";                                // OK: HEAD Last-Modified
            
//            String url = "http://espn.go.com";  // FAIL: Timestamp and IDs changed

            
            Config.setConfigFile(new File("../config/stucco.yml"));
            Map<String, String> configData = new HashMap<String, String>();
            configData.put("source-URI", url);
            CollectorNVDPageImpl collector = new CollectorNVDPageImpl(configData);
//            try {
//                collector.obtainWebPage("https://isc.sans.edu/diary.html?storyid=18311&rss");
//            }
//            catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
            System.err.println("COLLECTION #1");
            collector.collect();
            
            Thread.sleep(2000);
            System.err.println("\nCOLLECTION #2");
            collector.collect();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
