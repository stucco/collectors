package gov.pnnl.stucco.utilities;

import java.util.Date;

/** Struct-like class holding the metadata for collection at one URI. */
public class UriMetadata {
    /** Magic value indicating there is no value. */
    public static final String NONE = "";

    /** Timestamp (never null). */
    private Date timestamp = new Date(0);
    
    /** UUID */
    private String uuid = NONE;
    
    /** HTTP ETag (never null). */
    private String eTag = NONE;
    
    /** Our internal checksum (never null). */
    private String hash = NONE;
    
    
    /**
     * Constructs an empty record. It will default to the start-of-epoch 
     * Date and an empty hash. 
     */
    public UriMetadata() {
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getUuid() {
        return uuid;
    }

    public void setHash(String hash) {
        if (hash == null) {
            throw new NullPointerException();
        }
        
        this.hash = hash;
    }
    
    public String getHash() {
        return hash;
    }

    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        if (timestamp == null) {
            throw new NullPointerException();
        }
        
        this.timestamp = timestamp;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        if (eTag == null) {
            throw new NullPointerException();
        }
        
        this.eTag = eTag;
    }

    public String toString() {
        return toPersist();
    }
    
    /** 
     * Converts to human-readable format used for persisting the data. 
     * 
     * <p>Using this instead of toString() makes calls to it easier to find.
     */
    public String toPersist() {
        String httpTimestamp = TimestampConvert.dateToRfc1123(timestamp);
        return String.format("%s\t%s\t%s\t%s", httpTimestamp, eTag, hash, uuid);
    }
}
