package gov.pnnl.stucco.collectors;
/**
 * Listener interface used by NVDXMLExtractor and CollectorNVDPageImpl,
 * The Extractor fires events that the CollectorNVDPageImpl needs to know about
 * @author shawn
 *
 */
public interface NVDListener {
    void recordFound(NVDEvent e);
}
