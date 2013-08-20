package gov.pnnl.stucco.collectors;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

public abstract class CollectorAbstractBase implements Collector {
    
    protected final QueueSender m_queueSender           = new QueueSender();
    protected final ContentConverter m_contentConverter = new ContentConverter();
    
    private int numberOfThreads = 1;

    @Override
    public void setNumberOfThreads(int threadCount) {
        
        // we're only allow the number of threads to be between 1 and 8 (at this time)
        if (threadCount > 0 && threadCount < 9) {
            numberOfThreads = threadCount;
        }
    }

    @Override
    public void collect() {
        // TODO Auto-generated method stub
        
    }
    

}
