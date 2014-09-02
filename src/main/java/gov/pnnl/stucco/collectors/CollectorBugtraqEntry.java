package gov.pnnl.stucco.collectors;

import java.util.Map;

/**
 * Collector for an individual Bugtraq page entry. The entry URL is expected to
 * be of the form "http://www.securityfocus.com/bid/\d+". Its content is the 
 * same as subpage "http://www.securityfocus.com/bid/\d+/info". Other subpages
 * use similar URLs for "discuss", "exploit", "solution", and "references". 
 */
public class CollectorBugtraqEntry extends CollectorWebPageImpl {

    /** 
     * Creates a collector for a Bugtraq entry. 
     * 
     * @param configData  
     * Configuration data for the collector. This must contain the "source-URI" 
     * key-value pair.
     */
    public CollectorBugtraqEntry(Map<String, String> configData) {
        super(configData);
        
        // This collector won't store what it directly collects; its delegate
        // collectors will store the tabbed pages.
        setStoring(false);
    }

    @Override
    public void collect() {
        // Start the entry message
        StringBuilder messageBuffer = new StringBuilder();
        
        // Prepare to generate tab URLs
        String baseUri = m_URI;
        if (!baseUri.endsWith("/")) {
            baseUri += "/";
        }
        String[] tabs = { "info", "discuss", "exploit", "solution", "references" };
        
        // For each tab
        for (String tab : tabs) {

            // Create URL and collector
            String url = baseUri + tab;
            configData.put(SOURCE_URI, url);
            CollectorWebPageImpl tabCollector = new CollectorWebPageImpl(configData);
            
            // The Stucco message will be handled by this entry collector instead of the tab collector
            tabCollector.setMessaging(false);
            
            // Collect the tab
            tabCollector.collect();
            
            // Add ID and URL to the entry message
            String tabDocId = tabCollector.getDocId();
            messageBuffer.append(tabDocId);
            messageBuffer.append(" ");
            messageBuffer.append(url);
            messageBuffer.append("\n");
        }
        
        // Send the Stucco message
        m_rawContent = messageBuffer.toString().getBytes();
        m_queueSender.sendIdMessage(m_metadata, m_rawContent);
    }

}
