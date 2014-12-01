package gov.pnnl.stucco.utilities;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;


/** 
 * Repository for metadata used to help collectors know whether content has been updated. 
 */
public class CollectorMetadata {
        
    private static final CollectorMetadata singleton;
    static {
        try {
            singleton = new CollectorMetadata(new File("CollectorMetadata.mdb"));
        }
        catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** The MapDB database. */
    private DB db;
    
    /** 
     * Map of lowercased URI Strings to URI Metadata. The values must be 
     * immutable in order to use MapDB.
     */
    private SortedMap<String, ImmutableUriMetadata> collectionMap;
    
    
    
    public static CollectorMetadata getInstance() {
        return singleton;
    }
    
    private CollectorMetadata(File persistenceFile) throws IOException {
        // configure and open database using builder pattern.
        // all options are available with code auto-completion.
        db = DBMaker.newFileDB(persistenceFile)
                .closeOnJvmShutdown()
                .make();

        // open existing an collection (or create new)
        collectionMap = db.getTreeMap("collectionMap");
        
        debugPrint();
    }
    
    /** Gets the number of entries in the metadata collection. */
    public int size() {
        int count = collectionMap.size();
        return count;
    }
    
    /** Gets the URLs in the metadata collection. */
    public List<String> getUrls() {
        List<String> urlList = new ArrayList<String>(collectionMap.keySet());
        return urlList;
    }
    
    /** Debug method for printing the collection. */
    private void debugPrint() {
        Set<Entry<String, ImmutableUriMetadata>> entrySet = collectionMap.entrySet();
        for (Entry<String, ImmutableUriMetadata> entry : entrySet) {
            String url = entry.getKey();
            ImmutableUriMetadata metadata = entry.getValue();
            System.err.println("Key: " + url);
            System.err.println("Value: ");
            String uuid = metadata.getUuid();
            String hash = metadata.getHash();
            String eTag = metadata.getETag();
            Date time = metadata.getTimestamp();
            System.err.println("  UUID = " + uuid);
            System.err.println("  SHA-1 = " + hash);
            System.err.println("  ETag = " + eTag);
            System.err.println("  Time = " + time);
        }
    } 
    
    /** Gets whether there's metadata for a given URI. */
    public boolean contains(String uri) {
        return collectionMap.containsKey(uri);
    }
    
    /** Gets metadata for a URI, creating a new object. */
    private MutableUriMetadata getMetadata(String uri) {
        // Normalize
        uri = uri.toLowerCase();
        
        ImmutableUriMetadata metadata = collectionMap.get(uri);

        if (metadata == null) {
        	return new MutableUriMetadata();
        }
        else {
        	return new MutableUriMetadata(metadata);
        }
        
    }
    
    /** Puts URI metadata into the map. */
    private void putMetadata(String uri, MutableUriMetadata metadata) {
        // Normalize
        uri = uri.toLowerCase();
                
        ImmutableUriMetadata copy = new ImmutableUriMetadata(metadata);
        collectionMap.put(uri, copy);
    }
    
    /** Sets the UUID for a URI. */
    public void setUuid(String uri, String uuid) {
        MutableUriMetadata metadata = getMetadata(uri);
        metadata.setUuid(uuid);
        putMetadata(uri, metadata);
    }
    
    /** Gets the UUID for a URI. */
    public String getUuid(String uri) {
        MutableUriMetadata metadata = getMetadata(uri);
        String uuid = metadata.getUuid();
        
        return uuid;
    }
    
    /** Sets the timestamp for a URI. */ 
    public void setTimestamp(String uri, Date timestamp) {
        MutableUriMetadata metadata = getMetadata(uri);
        metadata.setTimestamp(timestamp);
        putMetadata(uri, metadata);
   }
    
    /** Gets the timestamp for a URI. */
    public Date getTimestamp(String uri) {
        MutableUriMetadata metadata = getMetadata(uri);
        Date timestamp = metadata.getTimestamp();
        
        return timestamp;
    }
    
    /** Checks whether a timestamp is newer than the current saved value for a URI. */
    public boolean isNewTimestamp(String uri, Date timestamp) {
        Date currentTimestamp = getTimestamp(uri);
        
        return timestamp.after(currentTimestamp);
    }
    
    /** Sets the HTTP ETag for a URI. */
    public void setETag(String uri, String eTag) {
        MutableUriMetadata metadata = getMetadata(uri);
        metadata.setETag(eTag);
        putMetadata(uri, metadata);
    }
    
    /** 
     * Gets the HTTP ETag for a URI. 
     * 
     * @return ETag (or CollectorMetadata.NONE)
     */
    public String getETag(String uri) {
        MutableUriMetadata metadata = getMetadata(uri);
        String eTag = metadata.getETag();
        
        return eTag;
    }
    
    /** 
     * Updates the hash for the content at a URI.
     * 
     * @return whether the hash changed from what we had recorded
     */
    public boolean updateHash(String uri, byte[] content) {        
        // Default in case anything goes wrong
        boolean changed = true;
        
        try {
            String oldChecksum = getHash(uri);
            String newChecksum = FileChecksum.compute("SHA-1", content);
            changed = !oldChecksum.equalsIgnoreCase(newChecksum);
            if (changed) {
                setHash(uri, newChecksum);
            }
        }
        catch (NoSuchAlgorithmException e) {
            // Should never happen since we know SHA-1 is supported.
            // If it does, 
            e.printStackTrace();
        }
        
        return changed;
    }
    
    /** Computes our internal hash for byte content. */
    public static String computeHash(byte[] content) {
        String checksum = MutableUriMetadata.NONE;
        try {
            checksum = FileChecksum.compute("SHA-1", content);
        }
        catch (NoSuchAlgorithmException e) {
            // Should never happen since SHA-1 is supported
            e.printStackTrace();
        }
        return checksum;
    }
    
    /** Sets the current hash associated with a URI. */
    public boolean setHash(String uri, String checksum) {
        String oldChecksum = getHash(uri);
        boolean changed = !oldChecksum.equalsIgnoreCase(checksum);
        if (changed) {
            MutableUriMetadata metadata = getMetadata(uri);
            metadata.setHash(checksum);
            putMetadata(uri, metadata);
        }
        
        return changed;
    }
    
    /** 
     * Gets the current hash code associated with a URI. 
     * 
     * @return Hash (or CollectorMetadata.NONE)
     */
    public String getHash(String uri) {
        MutableUriMetadata metadata = getMetadata(uri);
        String hashCode = metadata.getHash();
        return hashCode;
    }
    
    /** 
     * Removes a single URL key from the metadata. 
     *
     * <p> Call save() to complete the operation.
     */
    public void remove(String url) {
        collectionMap.remove(url);
    }
    
    /** 
     * Removes multiple URL keys from the metadata. 
     * 
     * <p> Call save() to complete the operation.
     */
    public void removeAll(Collection<String> urls) {
        for (String url : urls) {
            remove(url);
        }
    }
    
    /** Saves the metadata to our tab-delimited file. */
    public void save() throws IOException {
        db.commit();
    }
    
}
