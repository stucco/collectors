package gov.pnnl.stucco.collectors;

import java.util.Map;


public class CollectorSophosEntry extends CollectorHttp { 

    public CollectorSophosEntry(Map<String, String> configData) {
        super(configData);
    }

    @Override
    public void collect() {
        // Start the entry message
        StringBuilder messageBuffer = new StringBuilder();
        
        // Generate tab URLs
        String moreInfoUri = sourceUri.replace(".aspx", "/detailed-analysis.aspx");
        String[] urls = { sourceUri, moreInfoUri };
        
        // For each tab
        for (String url : urls) {

            // Create URL and collector
            collectorConfigData.put(SOURCE_URI, url);
            CollectorWebPageImpl tabCollector = new CollectorWebPageImpl(collectorConfigData);
            
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
        rawContent = messageBuffer.toString().getBytes();
        messageSender.sendIdMessage(messageMetadata, rawContent);
    }
}
