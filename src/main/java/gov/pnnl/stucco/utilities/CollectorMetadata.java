package gov.pnnl.stucco.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/** 
 * Repository for metadata used to help collectors know whether content has been updated. 
 */
public class CollectorMetadata {
        
    private static final CollectorMetadata singleton;
    static {
        try {
            singleton = new CollectorMetadata(new File("CollectorMetadata.db"));
        }
        catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** Map of lowercased Uri Strings to UriCollectionRecords. */
    private final SortedMap<String, UriMetadata> collectionMap = new TreeMap<String, UriMetadata>();
    
    /** File for persisting the metadata. Each line is: URI\tTimestamp\tETag\tHashCode. */
    private File persistenceFile;
    
    
    
    public static CollectorMetadata getInstance() {
        return singleton;
    }
    
    private CollectorMetadata(File persistenceFile) throws IOException {
        this.persistenceFile = persistenceFile;
        
        if (persistenceFile.exists()) {
            load(persistenceFile);
        }
        else {
            // Not there, so create an empty one
            save();
        }
    } 
    
    /** Gets whether there's metadata for a given URI. */
    public boolean contains(String uri) {
        synchronized (collectionMap) {
            return collectionMap.containsKey(uri);
        }
    }
    
    /** Gets metadata for a URI, creating a new object if necessary. */
    private UriMetadata getOrCreateMetadata(String uri) {
        synchronized (collectionMap) {
            // Normalize
            uri = uri.toLowerCase();

            UriMetadata metadata = collectionMap.get(uri);
            if (metadata == null) {
                metadata = new UriMetadata();
                collectionMap.put(uri, metadata);
            }

            return metadata;
        }
    }
    
    /** Sets the UUID for a URI. */
    public void setUuid(String uri, String uuid) {
        UriMetadata metadata = getOrCreateMetadata(uri);
        metadata.setUuid(uuid);
    }
    
    /** Gets the UUID for a URI. */
    public String getUuid(String uri) {
        UriMetadata metadata = getOrCreateMetadata(uri);
        String uuid = metadata.getUuid();
        
        return uuid;
    }
    
    /** Sets the timestamp for a URI. */ 
    public void setTimestamp(String uri, Date timestamp) {
        UriMetadata metadata = getOrCreateMetadata(uri);
        metadata.setTimestamp(timestamp);
    }
    
    /** Gets the timestamp for a URI. */
    public Date getTimestamp(String uri) {
        UriMetadata metadata = getOrCreateMetadata(uri);
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
        UriMetadata metadata = getOrCreateMetadata(uri);
        metadata.setETag(eTag);
    }
    
    /** 
     * Gets the HTTP ETag for a URI. 
     * 
     * @return ETag (or CollectorMetadata.NONE)
     */
    public String getETag(String uri) {
        UriMetadata metadata = getOrCreateMetadata(uri);
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
        String checksum = UriMetadata.NONE;
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
            UriMetadata metadata = getOrCreateMetadata(uri);
            metadata.setHash(checksum);
        }
        
        return changed;
    }
    
    /** 
     * Gets the current hash code associated with a URI. 
     * 
     * @return Hash (or CollectorMetadata.NONE)
     */
    public String getHash(String uri) {
        UriMetadata metadata = getOrCreateMetadata(uri);
        String hashCode = metadata.getHash();
        return hashCode;
    }
    
    /** Saves the metadata to our tab-delimited file. */
    public void save() throws IOException {
        save(persistenceFile);
    }
    
    /** Saves the metadata to a tab-delimited file. */
    private synchronized void save(File f) throws IOException {
        synchronized (collectionMap) {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)))) {
                for (Map.Entry<String, UriMetadata> entry : collectionMap.entrySet()) {
                    String key = entry.getKey();
                    UriMetadata value = entry.getValue();
                    out.printf("%s\t%s%n", key, value.toPersist());
                }
            }
        }
    }

    /** Loads the metadata from a tab-delimited file where each line is URI\tTimestamp\tETag\tHashCode. */
    private synchronized void load(File f) throws IOException {
        synchronized (collectionMap) {
            try (BufferedReader in = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty() || line.startsWith("//")) {
                        // Empty or comment; ignore
                        continue;
                    }

                    // Split it on tabs
                    String[] token = line.split("\t", -1);

                    // Get the tokens
                    String key = token[0].toLowerCase();
                    String httpTimestamp = token[1];
                    String eTag = (token.length > 2)?  token[2] : UriMetadata.NONE;
                    String hash = (token.length > 3)?  token[3].toLowerCase() : UriMetadata.NONE;
                    String uuid = (token.length > 4)?  token[4].toLowerCase() : UriMetadata.NONE;

                    UriMetadata value = new UriMetadata();
                    value.setETag(eTag);
                    value.setHash(hash);
                    value.setUuid(uuid);

                    try {
                        // Convert timestamp to date
                        Date date = TimestampConvert.rfc1123ToDate(httpTimestamp);
                        value.setTimestamp(date);
                    }
                    catch (ParseException e) {
                        // Shouldn't happen unless the file has been manually edited.
                        // If so, we'll just leave the timestamp at the default value.
                        e.printStackTrace();
                    }

                    collectionMap.put(key, value);
                }
            }
        }
    }
}
