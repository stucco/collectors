package gov.pnnl.stucco.collectors;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

public class CollectorFactory {
    // get the instance of the global logger
    private static final Logger logger = LoggerFactory.getLogger(CollectorFactory.class);
    
    // simple factory approach, will probably want to redo this as we include more collector types
    /* 
     * @param configData  Configuration data specific to a given collector
     */
    public static Collector makeCollector(Map<String, String> configData) {
        String collectorType = configData.get("type");
        logger.debug("Creating collector: " + collectorType);
        switch (collectorType.toUpperCase()) {
            case "WEB":
                return new CollectorWebPageImpl(configData);
                
            case "FILE":
                return new CollectorFileImpl(configData);
                
            case "FILEBYLINE":
                return new CollectorFileByLineImpl(configData);
                
            case "RSS":
                return new CollectorRssImpl(configData);
            
            case "NVD":
                return new CollectorNVDPageImpl(configData);

            case "PSEUDO_RSS":
                return new CollectorPseudoRss(configData);
            
            case "TABBED_ENTRY":
                return new CollectorTabbedEntry(configData);
                
            default:
                break;
        }
        return null;
    }

}
