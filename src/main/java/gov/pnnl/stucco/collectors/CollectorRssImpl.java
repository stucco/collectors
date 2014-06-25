package gov.pnnl.stucco.collectors;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;


/**
 * Collector for RSS and Atom feeds.
 */
public class CollectorRssImpl extends CollectorAbstractBase {
    /** Log */
    private static final Logger logger = LoggerFactory.getLogger(CollectorRssImpl.class);

    
    public CollectorRssImpl(Map<String, String> configData) {
        super(configData);
    }

    @Override
    public void collect() {
        try {
            // Get URL from config
            String urlStr = m_metadata.get("sourceUrl");
            URL feedUrl = new URL(urlStr);

            // Read the feed with the ROME library
            SyndFeedInput input = new SyndFeedInput();
            Reader read = new XmlReader(feedUrl);
            SyndFeed feed = input.build(read);

            // Set up for applying individual configuration for each URL
            Map<String, String> webConfig = new HashMap<String, String>();
            String sourceName = m_metadata.get("sourceName");
            
            // Retrieve the web page from each entry
            List<SyndEntryImpl> entries = feed.getEntries();
            for (SyndEntryImpl entry : entries) {
                String link = entry.getLink();
                logger.info("Collecting RSS feed entry: " + link);
                
                // Set the configuration info needed by the web collector
                webConfig.clear();
                webConfig.put("type", "WEB");
                webConfig.put("source-name", sourceName);
                webConfig.put("source-URI", link);
                webConfig.put("content-type", "text/html");
                webConfig.put("data-type", m_metadata.get("dataType")); 

                // Use the web collector
                Collector collector = CollectorFactory.makeCollector(webConfig);
                collector.collect();
            }
            clean();
        } catch (IOException | FeedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void clean() {
        // Nothing to do
    }
}
