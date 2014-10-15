package gov.pnnl.stucco.collectors;

import java.util.Map;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

public class CollectorFactory {
    
    // simple factory approach, will probably want to redo this as we include more collector types
    /* 
     * @param configData  Configuration data specific to a given collector
     */
    public static Collector makeCollector(Map<String, String> configData) {
        String collectorType = configData.get("type");     
        switch (collectorType.toUpperCase()) {
            case "WEB":
                return new CollectorWebPageImpl(configData);
                
            case "FILE":
                return new CollectorFileImpl(configData);
                
            case "FILEBYLINE":
                return new CollectorFileByLineImpl(configData);
                
            case "DIRECTORY":
                return new CollectorDirectoryImpl(configData);
                
            case "RSS":
                return new CollectorRssImpl(configData);
                
            case "BUGTRAQ":
                return new CollectorBugtraqVulnerabilities(configData);
                
            case "SOPHOS":
                return new CollectorSophosEntry(configData);
            
            case "PAGE_SEQUENCE":
                return new CollectorPageSequence(configData);
            
            case "TABBED_ENTRY":
                return new CollectorTabbedEntry(configData);
                
            default:
                break;
        }
        return null;
    }

}
