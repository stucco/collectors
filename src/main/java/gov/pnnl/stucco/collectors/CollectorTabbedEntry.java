package gov.pnnl.stucco.collectors;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
                    List<String> urlList = scrapeTabUrls();
                    
                    // For each entry 
                    for (String url : urlList) {
                        // If URL already in database
                        if (pageMetadata.contains(url)) {
                            // It's old, so we're done
                            break;
                        }
                        
                        // It's new, so collect it
                        collectorConfigData.put(SOURCE_URI, url);
                        CollectorWebPageImpl webCollector = new CollectorWebPageImpl(collectorConfigData);
                        webCollector.collect();
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
    private List<String> scrapeTabUrls() {
        List<String> urlList = new ArrayList<String>();

        try {
            // Convert bytes to String
            String page = new String(rawContent);
            
            // Get the regex definition for finding the tab URLs
            String tabRegEx = getTabRegEx();
            
            // Prepare the regex to find the URLs
            Pattern pattern = Pattern.compile(tabRegEx);
            Matcher matcher = pattern.matcher(page);

            URI source = new URI(sourceUri);
            
            // For each match of the regex
            while (matcher.find()) {
                // Grab the matching URL
                String match = getFirstCapture(matcher);
                
                // If it's a relative path, convert to absolute
                match = source.resolve(match).toString();
                
                // Save it
                urlList.add(match);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        
        return urlList;
    }
    
    private String getTabRegEx() {
        String tabRegEx = collectorConfigData.get("tabRegEx");
        return tabRegEx;
    }
    
    /** Gets the Matcher's first captured group (or null if nothing got captured. */
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

    public static void main(String[] args) {
//        try {
            // Test URL
            String url = "http://www.securityfocus.com/bid/70094";
//            URI baseUri = new URI(url);

            // Regex for identifying tabs
            String tabRegEx = "href=\"(/bid/\\d+/(info|discuss|exploit|solution|references))\"";
//            String tabRegEx = "href=\"(/bid/\\d+/info)\"";
//            tabRegEx += "|href=\"(/bid/\\d+/discuss)\"";
//            tabRegEx += "|href=\"(/bid/\\d+/exploit)\"";
//            tabRegEx += "|href=\"(/bid/\\d+/solution)\"";
//            tabRegEx += "|href=\"(/bid/\\d+/references)\"";

//            // String to test regex on
//            String content = "<ul>\n" +
//                            "<li class=\"here\"><a href=\"/bid/70094/info\">info</a></li>\n" +
//                            "<li><a href=\"/bid/70094/discuss\">discussion</a></li>\n" +
//                            "<li><a href=\"/bid/70094/exploit\">exploit</a></li>\n" +
//                            "<li><a href=\"/bid/70094/solution\">solution</a></li>\n" +
//                            "<li><a href=\"/bid/70094/references\">references</a></li>\n" +
//                            "</ul>";
//            
//            // Build the matcher
//            Pattern pattern = Pattern.compile(tabRegEx);
//            Matcher matcher = pattern.matcher(content);
//            
//            // For each match in the test content
//            while (matcher.find()) {
//                
//                // Get the first captured group (we expect only one)
//                String capture = CollectorTabbedEntry.getFirstCapture(matcher);
//                
//                if (capture != null) {
//                    // Print it, before/after resolving it
//                    System.err.println(capture);
//                    URI tabUri = baseUri.resolve(capture);
//                    System.err.println(tabUri);
//                }
//            }
            
            // Set configuration for the collector
            Config.setConfigFile(new File("../config/stucco.yml"));
            Map<String, String> configData = new HashMap<String, String>();
            configData.put(SOURCE_URI, url);
            configData.put("tabRegEx", tabRegEx);
            
            // Run the test collection
            CollectorTabbedEntry collector = new CollectorTabbedEntry(configData);
            collector.collect();
//        } 
//        catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
        
        
    }
}
