package gov.pnnl.stucco.utilities;


/** 
 * Enumeration used to indicate whether our feed collecting should continue
 * or stop (with the reason for stopping).
 */
public enum FeedCollectionStatus {
    /** OK to continue. */
    GO,
    
    // Various potential stop conditions
    
    /** Scraping found no entries. */
    STOP_EMPTY,
    
    /** We hit our max limit of pages. */
    STOP_MAXED,
    
    /** We collected a duplicate of something we already had. */
    STOP_DUPLICATE,
    
    /** Can't collect because something is specified incorrectly. */
    STOP_INVALID
}
