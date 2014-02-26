package gov.pnnl.stucco.collectors;

import java.util.Map;

import gov.pnnl.stucco.doc_service_client.*;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

/** Abstract base class used in implementing Collectors. */
public abstract class CollectorAbstractBase implements Collector {
    
    protected final QueueSender m_queueSender           = new QueueSender();
    protected final ContentConverter m_contentConverter = new ContentConverter();
    
    private int numberOfThreads = 1;
    
    /** Map of configuration data for the specific collector. */
    private Map<String, String> configData;

    protected CollectorAbstractBase(Map<String, String> configData) {
        this.configData = configData;
        
        Map<String, Object> defaultSection = (Map<String, Object>) Config.getMap().get("default");
        Map<String, Object> docServiceConfig = (Map<String, Object>) defaultSection.get("document-service");
        
        DocServiceClient docServiceClient = new DocServiceClient(docServiceConfig);
        
        // we create a delegate for queueSender
        m_queueSender.setDocService(docServiceClient);
    }

    public void setNumberOfThreads(int threadCount) {
        
        // we're only allow the number of threads to be between 1 and 8 (at this time)
        if (threadCount > 0 && threadCount < 9) {
            numberOfThreads = threadCount;
        }
    }

    public abstract void collect();
    
    public abstract void clean();
}
