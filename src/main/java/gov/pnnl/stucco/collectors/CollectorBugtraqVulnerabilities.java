package gov.pnnl.stucco.collectors;


/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/** 
 * Collector for the Bugtraq vulnerabilities website. This collector can
 * recursively collect multiple listing pages and their entries. By default, it
 * collects the latest page of 30 entries. 
 */
public class CollectorBugtraqVulnerabilities extends CollectorHttp {
    
    /** Number of entries to get per listing page. */
    private int entriesPerListing = 30;
    
    /** Maximum number of listing pages to request. */
    private int maxListingPages = 1;
    
    
    /** 
     * Constructs the Bugtraq vulnerabilities collector.
     * 
     * @param configData
     * Configuration data from the config file/service, specific to this 
     * collector. For most collector, this would include the "source-URI" 
     * key-value, but that would be ignored in this case, because this 
     * collector automatically generates the Bugtraq listing URLs.
     */
    public CollectorBugtraqVulnerabilities(Map<String, String> configData) {
        super(configData);
    }
    
    /** 
     * Sets the number of entries to request for each listing page. The default 
     * is 30 (the same as for the site). The maximum is 100; higher numbers are 
     * allowed but return only 100.
     */
    public void setEntriesPerListing(int n) {
        entriesPerListing  = n;
    }
    
    /** Sets a limit on the number of entries to collect. The default is 1. */
    public void setMaxListingPages(int n) {
        maxListingPages = n;
    }
    
    @Override
    public void collect() {
        // The format for the listing URL
        final String urlFormat = "http://www.securityfocus.com/cgi-bin/index.cgi?op=display_list&c=12&o=%d&l=%d";
        
        if (!collectorConfigData.containsKey(CRAWL_DELAY_KEY)) {
            // No crawl delay specified; set a default
            collectorConfigData.put(CRAWL_DELAY_KEY, "2");
        }

        // For each listing page requested (possibly only one)
        for (int page = 1; page <= maxListingPages; page++) {
            // Generate URL
            int startIndex = 1 + ((page - 1) * entriesPerListing);
            String url = String.format(urlFormat, startIndex, entriesPerListing);
            
            // Create listing page collector
            collectorConfigData.put(SOURCE_URI, url);
            CollectorBugtraqListing listingCollector = new CollectorBugtraqListing(collectorConfigData);

            // Collect the listing and the content from its entries
            listingCollector.collect();
            
            if (listingCollector.isAllCollected()) {
                // Edge case: We already have everything, so stop
                break;
            }
        }
    }
           
    /** Test driver used during development. */
    static public void main(String[] args) {
        try {                
            String url = "http://www.securityfocus.com/vulnerabilities";
            
            Config.setConfigFile(new File("../config/stucco.yml"));
            Map<String, String> configData = new HashMap<String, String>();
            configData.put(SOURCE_URI, url);
            CollectorBugtraqVulnerabilities collector = new CollectorBugtraqVulnerabilities(configData);

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
