package gov.pnnl.stucco.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Collector for an individual Bugtraq page entry. The entry URL is expected to
 * be of the form "http://www.securityfocus.com/bid/\d+". Its content is the 
 * same as subpage "http://www.securityfocus.com/bid/\d+/info". Other subpages
 * use similar URLs for "discuss", "exploit", "solution", and "references". 
 */
public class CollectorBugtraqEntry extends CollectorHttp {

    /** 
     * Creates a collector for a Bugtraq entry. 
     * 
     * @param configData  
     * Configuration data for the collector. This must contain the "source-URI" 
     * key-value pair.
     */
    public CollectorBugtraqEntry(Map<String, String> configData) {
        super(configData);
     }

    @Override
    public void collect() {
        // Prepare to generate tab URLs
        String baseUri = sourceUri;
        if (!baseUri.endsWith("/")) {
            baseUri += "/";
        }
        String[] tabs = { "info", "discuss", "exploit", "solution", "references" };
        
        // Generate tab URLs
        List<String> urls = new ArrayList<String>(tabs.length);
        for (String tab : tabs) {
            String url = baseUri + tab;
            urls.add(url);
        }
        
        collectAndAggregateUrls(urls);
    }
}
