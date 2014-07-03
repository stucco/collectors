package gov.pnnl.stucco.collectors;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import gov.pnnl.stucco.doc_service_client.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

/** Abstract base class used in implementing Collectors. */
public abstract class CollectorAbstractBase implements Collector {
    private static final Logger logger = LoggerFactory.getLogger(CollectorAbstractBase.class);
                    
    protected final Map<String, String> m_metadata = new HashMap<String, String>();
    protected final QueueSender m_queueSender           = new QueueSender();
    
    /** raw content from source */
    protected byte[] m_rawContent;
    
    /** time the data was collected */
    protected Date m_timestamp = null;
    
    private int numberOfThreads = 1;
    
    /** Map of configuration data for the specific collector. */
    private Map<String, String> configData;

    protected CollectorAbstractBase(Map<String, String> configData) {
        this.configData = configData;
        
        // default metadata comes from configuration
        m_metadata.put("contentType", configData.get("content-type"));
        m_metadata.put("dataType", configData.get("data-type"));
        m_metadata.put("sourceName", configData.get("source-name"));
        m_metadata.put("sourceUrl", configData.get("source-URI"));
        
        Map<String, Object> configMap = (Map<String, Object>) Config.getMap();
        Map<String, Object> stuccoMap = (Map<String, Object>) configMap.get("stucco");
        
        Map<String, Object> docServiceConfig = (Map<String, Object>) stuccoMap.get("document-service");
       
        try {
            DocServiceClient docServiceClient = new DocServiceClient(docServiceConfig);
        
            // we create a delegate for queueSender
            m_queueSender.setDocService(docServiceClient);
        } catch (DocServiceException e) {
            logger.error("Could instantiate document-service client", e);
        }
    }

    /**
     * Send the content when requested
     */
    public void send() {
        m_queueSender.send(m_metadata, m_rawContent);
    }
    
    public void setNumberOfThreads(int threadCount) {
        
        // we're only allow the number of threads to be between 1 and 8 (at this time)
        if (threadCount > 0 && threadCount < 9) {
            numberOfThreads = threadCount;
        }
    }

    @Override
    public abstract void collect();
    
    @Override
    public abstract void clean();
}
