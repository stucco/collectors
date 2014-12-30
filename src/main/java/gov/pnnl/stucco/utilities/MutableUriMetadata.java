package gov.pnnl.stucco.utilities;

import java.io.Serializable;
import java.util.Date;

/** Data class containing the metadata for collection at one URI. */
public class MutableUriMetadata implements Serializable {
    // For Serializable
    private static final long serialVersionUID = 361465469785419709L;

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
     * Date and empty String values. 
     */
    public MutableUriMetadata() {
    }
    
    /** Creates a mutable copy of an immutable record. */
    public MutableUriMetadata(MutableUriMetadata original) {
        timestamp = original.getTimestamp();
        uuid = original.getUuid();
        eTag = original.getETag();
        hash = original.getHash();
    }

    /** Creates a mutable copy of an immutable record. */
    public MutableUriMetadata(ImmutableUriMetadata original) {
        timestamp = original.getTimestamp();
        uuid = original.getUuid();
        eTag = original.getETag();
        hash = original.getHash();
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
        String httpTimestamp = TimestampConvert.dateToRfc1123(timestamp);
        return String.format("%s\t%s\t%s\t%s", httpTimestamp, eTag, hash, uuid);
    }
}
