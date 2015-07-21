package gov.pnnl.stucco.utilities;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** Utility class for converting key paths from absolute paths to local paths. */
public class StuccoJetcdUtil {

    // Prevent instantiation
    private StuccoJetcdUtil() {
    }
    
    /** 
     * Trims the key names in a jetcd configuration Map, to remove path info.
     * 
     * <p> For example, the key-value pair ("/full/path/key", "value") would be 
     * replaced with ("key", "value").
     * 
     * @return New Map with the trimmed keys
     */
    public static Map<String, Object> trimKeyPaths(Map<String, Object> configMap) {
        
        Map<String, Object> trimmedMap = new HashMap<String, Object>(configMap.size());
        
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Shorten the key
            key = trimKey(key);
            
            if (value instanceof Map) {
                // Recursively shorten keys in submaps
                value = trimKeyPaths((Map<String, Object>) value);
            }
            else if (value instanceof List) {
                // Recursively shorted keys in lists
                value = trimKeyPaths((List<Object>) value);
            }
            
            trimmedMap.put(key, value);
        }
        return trimmedMap;
    }
    
    /** 
     * Trims the key names in a jetcd configuration List, to remove path info.
     * 
     * <p> For example, the key-value pair ("/full/path/key", "value") would be 
     * replaced with ("key", "value").
     * 
     * @return New Map with the trimmed keys
     */
    public static List<Object> trimKeyPaths(List<Object> configList) {
        List<Object> trimmedList = new ArrayList<Object>(configList.size());
        
        for (Object value : configList) {
            if (value instanceof Map) {
                value = trimKeyPaths((Map<String, Object>) value);
            }
            else if (value instanceof List) {
                value = trimKeyPaths((List<Object>) value);
            }
            
            trimmedList.add(value);
        }
        
        return trimmedList;
    }
    
    /** Trims a jetcd key path down to just the key alone. */
    private static String trimKey(String keyPath) {
        // Remove excess whitespace at ends
        keyPath = keyPath.trim();
        
        int lastSlash = keyPath.lastIndexOf('/');
        if (lastSlash >= 0) {
            // Remove last slash and everything before it
            return keyPath.substring(lastSlash + 1);
        }
        else {
            // No slashes to remove
            return keyPath;
        }
    }
}
