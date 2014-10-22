package gov.pnnl.stucco.collectors;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Collector for gathering a sequence of pages that list entries, 
 * similar to an RSS feed.
 */
public class CollectorPageSequence extends CollectorWebPageImpl {

    // Config keys
    public static final String ENTRY_REGEX_KEY = "entry-regex";
    public static final String NEXT_PAGE_REGEX_KEY = "next-page-regex";
    
    /** Limit on the number of entries to process. */
    private int maxEntries = Integer.MAX_VALUE;

    /** 
     * Option to stop collection if we encounter an entry that has already
     * been collected (according to our collection metadata database).
     */ 
    private boolean stopOnRepeatEntry = true;

    /** Number of entries processed. */
    private int entryCount = 0;
        
    /** Enum for returning status of entries collection. */ 
    private static enum EntriesCollectionStatus  {
        /** OK to continue. */
        GO,
        
        // Various stop conditions
        
        /** Stop because scraping found no entries. */
        STOP_EMPTY,
        
        /** Stop because we hit our max limit. */
        STOP_MAXED,
        
        /** Stop because we found a repeat. */
        STOP_REPEAT
    }
    
    
    public CollectorPageSequence(Map<String, String> configData) {
        super(configData);
    }
    
    public void setMaxEntries(int n) {
        maxEntries = n;
    }
    
    public void setStopOnRepeatEntry(boolean flag) {
        stopOnRepeatEntry = flag;
    }
    
    @Override
    public void collect() {
        try {
            String pageUrl = sourceUri;
            while (pageUrl != null) {
                String nextPageUrl = null;
                
                if (needToGet(pageUrl)) {
                    if (obtainWebPage(pageUrl)) {
                        nextPageUrl = collectPage(pageUrl);
                    }
                }

                pageUrl = nextPageUrl;
            }
        }
        catch (IOException e) {
            logger.error("Exception raised while reading web page", e);
        } 
        finally {
            clean();
        }
    }

    /**
     * Collects one listing page (and entries), then returns URL for the next page.
     * 
     * @param pageUrl  Current page's URL
     * 
     * @return Next page's URL (or null if we should stop)
     */
    private String collectPage(String pageUrl) {
        // Get the regex for finding entry URLs
        String entryRegEx = collectorConfigData.get(ENTRY_REGEX_KEY);
        if (entryRegEx == null) {
            return null;
        }
        
        // Scrape the entry URLs from the listing
        List<String> entryList = scrapeUrls(entryRegEx);
        
        // Collect the entries
        EntriesCollectionStatus status = collectEntries(entryList);
        
        // Find the next page
        String nextPageUrl = null;
        if (status == EntriesCollectionStatus.GO) {
            nextPageUrl = getNextPageUrl();
        }
        
        return nextPageUrl;
    }
    
    
    /** 
     * Collects the entries from one listing page. 
     * 
     * @return Status of collecting entries.
     */
    private EntriesCollectionStatus collectEntries(List<String> entryUrls) {
        // Stop if we scraped no entries
        if (entryUrls.isEmpty()) {
            return EntriesCollectionStatus.STOP_EMPTY;
        }
        
        // Limit the list to the remainder of our quota
        int entriesAllowed = maxEntries - entryCount;
        int entriesAvailable = entryUrls.size();
        int entriesToCollect = Math.min(entriesAllowed, entriesAvailable);
        entryUrls = entryUrls.subList(0, entriesToCollect);
        
        // Loop through entries
        for (String entryUrl : entryUrls) {
            entryCount++;

            if (stopOnRepeatEntry) {
                // This option is on...
                
                //...so stop if we already collected this URL
                if (pageMetadata.contains(entryUrl)) {
                    return EntriesCollectionStatus.STOP_REPEAT;
                }
            }
            
            // Collect the URL
            Collector entryCollector = getEntryCollector(entryUrl);
            entryCollector.collect();
        }
        
        if (entryCount < maxEntries) {
            return EntriesCollectionStatus.GO;
        }
        else {
            // Stop if we reached our limit
            return EntriesCollectionStatus.STOP_MAXED;
        }
    }

    /** Scrapes the URL for the next page.*/
    private String getNextPageUrl() {
        // Default value if we can't or shouldn't continue
        String nextPageUrl = null;
        
        // Try scraping a regex to get the URL for the next page
        String nextPageRegEx = collectorConfigData.get(NEXT_PAGE_REGEX_KEY);
        if (nextPageRegEx != null) {
            List<String> nextPageList = scrapeUrls(nextPageRegEx);
            if (!nextPageList.isEmpty()) {
                nextPageUrl = nextPageList.get(0);
            }
        }
        
        return nextPageUrl;
    }

    /** Test driver. */
    public static void main(String[] args) {
        // Test URL
        String url = "http://www.securityfocus.com/vulnerabilities";
        
        // Regexes for identifying various URLs
        String entryRegEx = "href=\"(/bid/\\d+)\"";
        String tabRegEx = "href=\"(/bid/\\d+/(info|discuss|exploit|solution|references))\"";
        String nextPageRegEx = "href=\"(/cgi-bin/index\\.cgi\\?o[^\"]+)\">Next &gt;";
        
        Config.setConfigFile(new File("../config/stucco.yml"));
        Map<String, String> collectorConfigData = new HashMap<String, String>();
        collectorConfigData.put(SOURCE_URI, url);
        
        collectorConfigData.put(ENTRY_REGEX_KEY, entryRegEx);
        collectorConfigData.put(TAB_REGEX_KEY, tabRegEx);
        collectorConfigData.put(NEXT_PAGE_REGEX_KEY, nextPageRegEx);
        
        CollectorPageSequence collector = new CollectorPageSequence(collectorConfigData);
        collector.setMaxEntries(1);
        collector.setStopOnRepeatEntry(false);
        collector.collect();
    }
}





















