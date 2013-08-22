package gov.pnnl.stucco.collectors;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

/**
 * Collector interface to support the various collectors we will have to collect data from
 * endogenous and exogenous data sources
 */
public interface Collector {
    
    /**
     * How many threads should be running when collecting information from this source
     * @param threadCount
     */
    public void setNumberOfThreads(final int threadCount);
    
    /**
     * perform the collection process for this source
     */
    public void collect();

}
