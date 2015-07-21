package gov.pnnl.stucco.utilities;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;


/**
 * Model for the collector metadata table.
 */
public class CollectorMetadataTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    
    // Column numbers
    public static final int URL_COLUMN = 0;
    public static final int TIMESTAMP_COLUMN = 1;
    public static final int UUID_COLUMN = 2;
    public static final int HASH_COLUMN = 3;
    public static final int ETAG_COLUMN = 4;
    
    
    // INDEPENDENT fields
    
    /** The model content. */
    private CollectorMetadata metadata;
    
    
    // DEPENDENT fields that must be updated when INDEPENDENT fields change
    
    /** Cached list of the URL keys, so we can access by index. */
    private List<String> urlList = new ArrayList<String>();
    
    
    public CollectorMetadataTableModel(CollectorMetadata metadata) {
        this.metadata = metadata;  
        cacheUrlList(metadata);
    }
    
    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public int getRowCount() {
        int rows = metadata.size();
        return rows;
    }

    @Override
    public Object getValueAt(int row, int column) {
        String url = urlList.get(row);
        
        switch (column) {
            case URL_COLUMN:
                return url;
                
            case TIMESTAMP_COLUMN:
                Date timestamp = metadata.getTimestamp(url);
                return timestamp;
                
            case UUID_COLUMN:
                String uuid = metadata.getUuid(url);
                return uuid;

            case HASH_COLUMN:
                String hash = metadata.getHash(url);
                return hash;         
                
            case ETAG_COLUMN:
                String eTag = metadata.getETag(url);
                return eTag;
                
            default:
                throw new IndexOutOfBoundsException();       
        }
    }
    
    @Override
    public String getColumnName(int column) {
        switch (column) {
            case URL_COLUMN:
                return "URL";
                
            case TIMESTAMP_COLUMN:
                return "Timestamp";
                
            case UUID_COLUMN:
                return "UUID";
                
            case HASH_COLUMN:
                return "SHA-1";
            
            case ETAG_COLUMN:
                return "ETag";
                
            default:
                throw new IndexOutOfBoundsException();       
        }
        
    }
    
    /** Removes the given rows from the model. */
    public void removeAll(List<Integer> rows) {
        for (int row : rows) {
            String url = (String) getValueAt(row, 0);
            metadata.remove(url);
        }
        cacheUrlList(metadata);
        
        fireTableDataChanged();
    }
    
    /** Updates the URLs from the metadata. */
    private void cacheUrlList(CollectorMetadata metadata) {
        urlList = metadata.getUrls();
    }
}
