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
        Collector aCollector = null;
        if(collectorType.equalsIgnoreCase("WEB")) {
            CollectorWebPageImpl webCollector = new CollectorWebPageImpl(configData);
            aCollector = webCollector;
        } else if(collectorType.equalsIgnoreCase("FILE")) {
            CollectorFileImpl fileCollector = new CollectorFileImpl(configData);
            aCollector = fileCollector;
        } else if(collectorType.equalsIgnoreCase("FILEBYLINE")) {
            CollectorFileByLineImpl fileByLineCollector = new CollectorFileByLineImpl(configData);
            aCollector = fileByLineCollector;
        } else if(collectorType.equalsIgnoreCase("DIRECTORY")) {
            CollectorDirectoryImpl dirCollector = new CollectorDirectoryImpl(configData);
            aCollector = dirCollector;
        } else if (collectorType.equalsIgnoreCase("RSS")) {
            CollectorRssImpl rssCollector = new CollectorRssImpl(configData);
            aCollector = rssCollector;
        } else if (collectorType.equalsIgnoreCase("BUGTRAQ")) {
            CollectorBugtraqVulnerabilities rssCollector = new CollectorBugtraqVulnerabilities(configData);
            aCollector = rssCollector;
        } else if (collectorType.equalsIgnoreCase("SOPHOS")) {
            CollectorSophosEntry rssCollector = new CollectorSophosEntry(configData);
            aCollector = rssCollector;
        }
        
        return aCollector;
    }

}
