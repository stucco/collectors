package gov.pnnl.stucco.utilities;


import java.io.Serializable;
import java.util.Date;

/**
 * Immutable version of URI metadata. This is intended to be used with MapDB
 * (see www.mapdb.org), which provides a Map backed by an embedded database.
 * MapDB requires immutable values in the Map.
 */
public class ImmutableUriMetadata implements Serializable {
    // For Serializable
    private static final long serialVersionUID = -6007444375800541775L;

    // Delegate to the mutable version, so we don't duplicate code.
    private final MutableUriMetadata delegate;


    public ImmutableUriMetadata(MutableUriMetadata original) {
        delegate = new MutableUriMetadata(original);
    }

    public String getUuid() {
        String uuid = delegate.getUuid();
        return uuid;
    }

    public String getHash() {
        String hash = delegate.getHash();
        return hash;
    }

    public Date getTimestamp() {
        Date timestamp = delegate.getTimestamp();
        return timestamp;
    }

    public String getETag() {
        String eTag = delegate.getETag();
        return eTag;
    }

    public String toString() {
        String str = delegate.toString();
        return str;
    }
}
