package gov.pnnl.stucco.collectors;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Collector for gathering an entry with (linked) tabs.
 */
public class CollectorTabbedEntry extends CollectorWebPageImpl {

    public CollectorTabbedEntry(Map<String, String> configData) {
        super(configData);
    }

    @Override
    public void collect() {
        try {
            if (needToGet(sourceUri)) {
                if (obtainWebPage(sourceUri)) {
                    
                    // Scrape the tab URLs from the listing
                    String tabRegEx = collectorConfigData.get(TAB_REGEX_KEY);
                    List<String> urlList = scrapeUrls(tabRegEx);
                    
                    // Collect the tabs and send a single message about them
                    collectAndAggregateUrls(urlList);
                }
            }
            clean();
        }
        catch (IOException e) {
            logger.error("Exception raised while reading web page", e);
        }
        
    }

    public static void main(String[] args) {
        // Test URL
        String url = "http://www.securityfocus.com/bid/70094";

        // Regex for identifying tabs
        String tabRegEx = "href=\"(/bid/\\d+/(info|discuss|exploit|solution|references))\"";

        // Set configuration for the collector
        Config.setConfigFile(new File("../config/stucco.yml"));
        Map<String, String> configData = new HashMap<String, String>();
        configData.put(SOURCE_URI, url);
        configData.put(TAB_REGEX_KEY, tabRegEx);

        // Run the test collection
        CollectorTabbedEntry collector = new CollectorTabbedEntry(configData);
        collector.collect();
    }
}
