package gov.pnnl.stucco.collectors;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

/** Abstract base class used in implementing Collectors. */
public abstract class CollectorAbstractBase implements Collector {
    
    protected final QueueSender m_queueSender           = new QueueSender();
    protected final ContentConverter m_contentConverter = new ContentConverter();
    
    private int numberOfThreads = 1;

    public void setNumberOfThreads(int threadCount) {
        
        // we're only allow the number of threads to be between 1 and 8 (at this time)
        if (threadCount > 0 && threadCount < 9) {
            numberOfThreads = threadCount;
        }
    }

    public abstract void collect();
}
