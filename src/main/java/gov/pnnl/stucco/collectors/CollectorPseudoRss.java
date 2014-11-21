package gov.pnnl.stucco.collectors;

import gov.pnnl.stucco.utilities.FeedCollectionStatus;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Collector for gathering a sequence of pages that list entries, 
 * similar to an RSS feed.
 */
public class CollectorPseudoRss extends CollectorWebPageImpl {

    // Config keys
    public static final String ENTRY_REGEX_KEY = "entry-regex";
    public static final String NEXT_PAGE_REGEX_KEY = "next-page-regex";
    
    /** Limit on the number of entries to process. */
    private int maxEntries = Integer.MAX_VALUE;

    /** Number of entries processed. */
    private int entryCount = 0;
        
    
    public CollectorPseudoRss(Map<String, String> configData) {
        super(configData);
    }
    
    public void setMaxEntries(int n) {
        maxEntries = n;
    }
    
    @Override
    public void collect() {
        try {
            // See if we want everything, otherwise we want new content only
            boolean getEverything = isForcedCollection();
            
            String pageUrl = sourceUri;
            while (pageUrl != null) {
                String nextPageUrl = null;
                
                if (getEverything || needToGet(pageUrl)) {
                    // The page is at least potentially something we want, 
                    // so attempt to GET it
                    if (obtainWebPage(pageUrl)) {
                        // We got what we wanted, so process it
                        collectEntriesFromPage(pageUrl);
                        
                        FeedCollectionStatus status = getCollectionStatus();
                        if (status == FeedCollectionStatus.GO) {
                            // OK so far; check for next page
                            nextPageUrl = getNextPageUrl();
                        }
                    }
                }
                pageUrl = nextPageUrl;
            }
        }
        catch (IOException e) {
            logger.error("Exception raised while reading web page:  " + sourceUri, e);
        } 
        finally {
            clean();
        }
    }

    /**
     * Collects a listing page and its entries.
     * 
     * <p> Sets feed collection status if a stop condition is detected.
     * 
     * @param pageUrl  Current listing page's URL
     */
    private void collectEntriesFromPage(String pageUrl) {
        // Get the regex for finding entry URLs
        String entryRegEx = collectorConfigData.get(ENTRY_REGEX_KEY);
        if (entryRegEx == null) {
            // There was no regex, so we can't collect
            setCollectionStatus(FeedCollectionStatus.STOP_INVALID);
            return;
        }
        
        // Scrape the entry URLs from the listing
        List<String> entryList = scrapeUrls(entryRegEx);
        
        // Collect the entries
        collectEntries(entryList);
    }
    
    
    /** 
     * Collects the entries from one listing page.
     * 
     * <p> Sets feed collection status if a stop condition is detected.
     */
    private void collectEntries(List<String> entryUrls) {
        if (entryUrls.isEmpty()) {
            // We got no entries, so it's probably pointless to try additional pages
            setCollectionStatus(FeedCollectionStatus.STOP_EMPTY);
            return;
        }
        
        // Limit the list to the remainder of our quota
        int entriesAllowed = maxEntries - entryCount;
        int entriesAvailable = entryUrls.size();
        int entriesToCollect = Math.min(entriesAllowed, entriesAvailable);
        entryUrls = entryUrls.subList(0, entriesToCollect);
        
        boolean checkForDuplicates = !isForcedCollection();
        
        // Loop through entries
        for (String entryUrl : entryUrls) {
            entryCount++;

            if (checkForDuplicates  &&  pageMetadata.contains(entryUrl)) {
                // We were looking for only new entries, but found an old one
                setCollectionStatus(FeedCollectionStatus.STOP_DUPLICATE);
                return;
            }
            
            // Collect the URL
            Collector entryCollector = getEntryCollector(entryUrl);
            entryCollector.collect();
        }
        
        if (entryCount >= maxEntries) {
            // We reached our limit on how much to collect
            setCollectionStatus(FeedCollectionStatus.STOP_MAXED);
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
        
        collectorConfigData.put(NOW_COLLECT_KEY, "all");
        
        CollectorPseudoRss collector = new CollectorPseudoRss(collectorConfigData);
        collector.setMaxEntries(5);
        collector.collect();
    }
}





















