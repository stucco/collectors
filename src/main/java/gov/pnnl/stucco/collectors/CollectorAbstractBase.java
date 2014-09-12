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
    
    /** Metadata for inclusion in the RabbitMQ header. */
    protected final Map<String, String> messageMetadata = new HashMap<String, String>();
    
    /** Delegate used to send RabbitMQ messages. */
    protected final QueueSender messageSender = new QueueSender();
    
    /** The document storage service. */
    protected DocServiceClient docServiceClient;

    /** raw content from source */
    protected byte[] rawContent;
    
    /** time the data was collected */
    protected Date timestamp = null;
    
    private int numberOfThreads = 1;
    
    /** Map of configuration data for the specific collector. */
    protected Map<String, String> collectorConfigData;


    protected CollectorAbstractBase(Map<String, String> configData) {
        this.collectorConfigData = configData;
        
        // default metadata comes from configuration
        messageMetadata.put("contentType", configData.get("content-type"));
        messageMetadata.put("dataType", configData.get("data-type"));
        messageMetadata.put("sourceName", configData.get("source-name"));
        messageMetadata.put("sourceUrl", configData.get("source-URI"));
        
        Map<String, Object> configMap = (Map<String, Object>) Config.getMap();
        Map<String, Object> stuccoMap = (Map<String, Object>) configMap.get("stucco");
        
        Map<String, Object> docServiceConfig = (Map<String, Object>) stuccoMap.get("document-service");
       
        try {
            docServiceClient = new DocServiceClient(docServiceConfig);
        
            // we create a delegate for queueSender
            messageSender.setDocService(docServiceClient);
        } catch (DocServiceException e) {
            logger.error("Could instantiate document-service client", e);
        }
    }

    /**
     * Send the content when requested
     */
    public void send() {
        messageSender.send(messageMetadata, rawContent);
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
    public void clean() {   
    }
}
