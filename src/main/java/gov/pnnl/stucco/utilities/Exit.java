package gov.pnnl.stucco.utilities;

/** 
 * Utility for exiting when a service isn't functioning correctly. In 
 * particular, we'll be calling this when we get exceptions from the document
 * service or RabbitMQ. The intent is that supervisord will be monitoring, and
 * will attempt to restart.
 * 
 * <p> Currently, the static method just calls System.exit(), but this serves
 * as a central location for documenting why (supervisord) we are choosing to
 * exit instead of catch and proceed. It may also be convenient if we later 
 * evolve the exit strategy.
 * 
 * @author Grant Nakamura, April 2015
 */
public class Exit {

    // Not instantiable
    private Exit() {
    }
    
    /** Exits the application. */
    public static void exit(int code) {
        // See class javadoc
        System.exit(code);
    }
}
