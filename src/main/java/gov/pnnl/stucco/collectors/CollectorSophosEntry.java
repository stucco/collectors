package gov.pnnl.stucco.collectors;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class CollectorSophosEntry extends CollectorHttp { 

    public CollectorSophosEntry(Map<String, String> configData) {
        super(configData);
    }

    @Override
    public void collect() {
        // Generate tab URLs
        String moreInfoUri = sourceUri.replace(".aspx", "/detailed-analysis.aspx");
        List<String> urls = Arrays.asList(new String[] { sourceUri, moreInfoUri });
        
        collectAndAggregateUrls(urls);
    }
}
