package gov.pnnl.stucco.collectors;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** 
 * Collector for a Bugtraq listing page. This collects the listing entries from 
 * a page and recursively collects the entries.
 */
public class CollectorBugtraqListing extends CollectorWebPageImpl {

    /** 
     * Whether the entire Bugtraq database is thought to have been collected 
     * already. This will occur if the listing is empty or if we encounter an
     * entry with a URL already in our CollectorMetadata database.
     */
    private boolean allCollected = false;
    
    
    public CollectorBugtraqListing(Map<String, String> configData) {
        super(configData);
    }

    /** Gets whether this collector has gotten all that it needs to get. */
    public boolean isAllCollected() {
        return allCollected;
    }
    
    @Override
    public void collect() {
        try {
            if (needToGet(m_URI)) {
                if (obtainWebPage(m_URI)) {
                    
                    // Scrape the entry URLs from the listing
                    List<String> urlList = scrapeEntryUrls();
                    
                    if (urlList.isEmpty()) {
                        // There's nothing on the listing page, so we're done
                        allCollected = true;
                        return;
                    }
                    
                    // For each entry 
                    for (String url : urlList) {
                        // If URL already in database
                        if (metadata.contains(url)) {
                            // It's old, so we're done
                            allCollected = true;
                            return;
                        }
                        
                        // It's new, so collect it
                        configData.put(SOURCE_URI, url);
                        CollectorBugtraqEntry entryCollector = new CollectorBugtraqEntry(configData);
                        entryCollector.collect();
                    } 
                }
            }
            clean();
        }
        catch (IOException e) {
            logger.error("Exception raised while reading web page", e);
        }
    }

    /** Parses the entry URLs from the page content. */
    private List<String> scrapeEntryUrls() {
        // Convert bytes to String
        String page = new String(m_rawContent);
        
        // Prepare a Java regex to find the URLs
        String regex = "<a href=\"(/bid/\\d+)\">http";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(page);

        // For each match of the regex
        List<String> urlList = new ArrayList<String>();
        while (matcher.find()) {
            // Convert the match to an absolute URL
            String match = matcher.group(1);            
            String url = "http://www.securityfocus.com" + match;
            urlList.add(url);
        }
        
        return urlList;
    }
    
    static public void main(String[] args) {
        String url = "http://www.securityfocus.com/cgi-bin/index.cgi?op=display_list&c=12&o=0&l=30";
        Config.setConfigFile(new File("../config/stucco.yml"));
        Map<String, String> configData = new HashMap<String, String>();
        configData.put(SOURCE_URI, url);
        
        CollectorBugtraqListing collector = new CollectorBugtraqListing(configData);
        collector.collect();
    }
}
