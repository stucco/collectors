package gov.pnnl.stucco.utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;


/**
 * Model for the collector metadata table.
 */
public class CollectorMetadataTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    
    
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
            case 0:
                return url;
                
            case 1:
                Date timestamp = metadata.getTimestamp(url);
                return timestamp;
                
            case 2:
                String hash = metadata.getHash(url);
                return hash;
            
            case 3:
                String eTag = metadata.getETag(url);
                return eTag;
                
            case 4:
                String uuid = metadata.getUuid(url);
                return uuid;
                
            default:
                throw new IndexOutOfBoundsException();       
        }
    }
    
    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "URL";
                
            case 1:
                return "Timestamp";
                
            case 2:
                return "SHA-1";
            
            case 3:
                return "ETag";
                
            case 4:
                return "UUID";
                
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

    /** Commits the changes to the backing database. */
    public void commit() throws IOException {
        metadata.save();
    }
}
