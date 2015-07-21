package gov.pnnl.stucco.collectors;


/**
 * NVD extractor event when a record is found this event is fired during the extraction
 * of multiple records from an XML file
 */
public class NVDEvent {
    /** the contents of the xml record */
    private byte[] record;
    
    /** constructor */
    public NVDEvent (byte[] newRecord) {
        record = newRecord;
    }
    
    /** accessor */
    public byte[] getRecord() {
        return record;
    }

}
