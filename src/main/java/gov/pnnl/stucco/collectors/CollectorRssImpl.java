package gov.pnnl.stucco.collectors;

import gov.pnnl.stucco.utilities.CollectorMetadata;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;


/**
 * Collector for RSS and Atom feeds.
 */
public class CollectorRssImpl extends CollectorHttp {
    
    public CollectorRssImpl(Map<String, String> configData) {
        super(configData);
    }

    @Override
    public void collect() {
        try {
            if (needToGet(m_URI)) {
                obtainFeed(m_URI);
            }
            clean();
        }
        catch (IOException e) 
        {
            logger.error("Exception raised while reading web page", e);
        }
    }
    
    /**
     * Retrieves the RSS feed and the items in it.
     *
     * @return Whether the RSS feed had new entries
     */
    private boolean obtainFeed(String url) {
        try {
            // Read the feed with the ROME library
            SyndFeedInput input = new SyndFeedInput();
            Reader read = new XmlReader(new URL(url));
            SyndFeed feed = input.build(read);

            // Set up for applying individual configuration for each URL
            String sourceName = m_metadata.get("sourceName");
            
            // Checksum the list of feed URLs
            String checksum = computeFeedChecksum(feed, true);
            String oldChecksum = metadata.getHash(url);
            boolean isNewContent = !oldChecksum.equalsIgnoreCase(checksum);
            
            if (isNewContent) {
                // Get the pages
                obtainFeedPages(feed, sourceName);

                // Get a timestamp
                Date timestamp = feed.getPublishedDate();
                if (timestamp == null) {
                    timestamp = new Date();
                }
                
                // Revise the checksum to reflect only URLs that we've 
                // successfully fetched at some point
                checksum = computeFeedChecksum(feed, false);

                // Update the metadata
                metadata.setTimestamp(url, timestamp);
                metadata.setHash(url, checksum);
                metadata.save();
            }
            else {
                logger.info("GET {} RSS list has same SHA-1 as before", url);
            }
            
            clean();
            return isNewContent;
        } 
        catch (IOException | FeedException e) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Computes a checksum for an RSS feed listing. 
     * 
     * @param feed  
     * RSS feed to checksum
     * 
     * @param checksumAll  
     * If true, checksum all URLs. If false, checksum only those URLs we know
     * we've successfully fetched at some point.
     * */
    private String computeFeedChecksum(SyndFeed feed, boolean checksumAll) {
        // Prepare to gather a list of URLs, so we can sort them.
        // We want to do this because some feeds change the order of listing.
        List<SyndEntryImpl> entries = feed.getEntries();
        List<String> urlList = new ArrayList<String>(entries.size());
        
        // Copy each URL
        for (SyndEntryImpl entry : entries) {
            String url = entry.getLink().toLowerCase();            
            if (checksumAll || metadata.getHash(url) != CollectorMetadata.NONE) {
                urlList.add(url);
            };
        }
        
        // Normalize the listing order 
        Collections.sort(urlList);
        
        // Concatenate the sorted URLs
        StringBuilder builder = new StringBuilder();
        for (String url : urlList) {
            builder.append(url);
        }
        
        // Get a checksum from that
        byte[] bytes = builder.toString().getBytes();
        String checksum = CollectorMetadata.computeHash(bytes);
        
        return checksum;
    }
    
    /** Cycles through an RSS feed list, retrieving content from the individual URLs. */
    private void obtainFeedPages(SyndFeed feed, String sourceName) {
        Map<String, String> webConfig = new HashMap<String, String>();

        // Retrieve the web page from each entry
        List<SyndEntryImpl> entries = feed.getEntries();
        for (SyndEntryImpl entry : entries) {
            String link = entry.getLink();
            logger.info("Collecting RSS feed entry: " + link);
            
            // Set the configuration info needed by the web collector
            webConfig.clear();
            webConfig.put("type", "WEB");
            webConfig.put("source-name", sourceName);
            webConfig.put("source-URI", link);
            webConfig.put("content-type", "text/html");
            webConfig.put("data-type", m_metadata.get("dataType"));
            
            // Use the web collector
            Collector collector = CollectorFactory.makeCollector(webConfig);
            collector.collect();
        }
    }

    @Override
    public void clean() {
        // Nothing to do
    }
    
    /** Test driver used during development. */
    static public void main(String[] args) {
        try {
//            String url = "http://seclists.org/rss/fulldisclosure.rss";                 // OK: HEAD conditional
//            String url = "http://www.reddit.com/r/netsec/new.rss";                     // FAIL: HEAD conditional or GET SHA-1, but 'ups', 'score', comments change ~10 seconds
//            String url = "https://technet.microsoft.com/en-us/security/rss/bulletin";  // FAIL: Items contain IDs that change
//            String url = "http://www.f-secure.com/exclude/vdesc-xml/latest_50.rss";    // OK: HEAD Last-Modified
//            String url = "https://isc.sans.edu/rssfeed_full.xml";                      // FAIL: HEAD Last-Modified, 'lastBuildDate' changes ~10 minutes
//            String url = "http://exploit-db.com/rss.xml";                              // 403 Forbidden
//            String url = "https://blog.damballa.com/feed";                             // 403 Forbidden
//            String url = "https://evilzone.org/.xml/?type=rss";                        // SSLHandshakeException
            String url = "http://rss.packetstormsecurity.com/files/";                  // OK: HEAD Last-Modified
                
            Config.setConfigFile(new File("../config/stucco.yml"));
            Map<String, String> configData = new HashMap<String, String>();
            configData.put("source-URI", url);
            CollectorHttp collector = new CollectorRssImpl(configData);
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