package gov.pnnl.stucco.collectors;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;


/** Base class for collectors reading directly from file. */
public abstract class CollectorFileBase extends CollectorAbstractBase {

    /** file from which we are obtaining content */
    protected File contentFile;

    
    public CollectorFileBase(Map<String, String> configData) {
        super(configData);
        setFilenameFromConfig(configData);
    }

    /** Gets the filename from the configuration data, and sets it for this collector. */
    private final void setFilenameFromConfig(Map<String, String> configData) {
        String filename = configData.get("source-URI");
        File f = new File(filename);
        if (f.isDirectory()) {
            throw new IllegalArgumentException(f + "is a directory, not a file");
        }
    
        contentFile = f;
    }

    /** Records the collection of a file in our metadata database. */
	protected void updateFileMetadataRecord(File file) throws IOException {
		// Get the file's URI and modification date
		String uri = file.toURI().toString();
		long modificationTime = file.lastModified();
		Date modificationDate = new Date(modificationTime);
		
		pageMetadata.setTimestamp(uri, modificationDate);
		pageMetadata.save();
	}

	/** 
	 * Checks whether we need to collect a File. We want to collect if the File 
	 * hasn't been collected before, or if it's been collected but there's a
	 * newer version there now.
	 */
	protected boolean needToGet(File file) {
		String uri = file.toURI().toString();
		
		if (pageMetadata.contains(uri)) {
			// Check modification date 
			long metadataModTime = pageMetadata.getTimestamp(uri).getTime();
			long currentModTime = file.lastModified();
			return (metadataModTime < currentModTime);
		}
		else {
			// Haven't collected it before
			return true;
		}
	}

}