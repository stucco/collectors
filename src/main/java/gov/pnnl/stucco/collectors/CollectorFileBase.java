package gov.pnnl.stucco.collectors;

import java.io.File;
import java.util.Map;


/** Base class for collectors reading directly from file. */
public abstract class CollectorFileBase extends CollectorAbstractBase {

    /** file from which we are obtaining content */
    protected File contentFile;

    public CollectorFileBase(Map<String, String> configData) {
        super(configData);
    }

    /** Gets the filename from the configuration data, and sets it for this collector. */
    protected void setFilenameFromConfig(Map<String, String> configData) {
        String filename = configData.get("source-URI");
        File f = new File(filename);
        if (f.isDirectory()) {
            throw new IllegalArgumentException(f + "is a directory, not a file");
        }
    
        contentFile = f;
    }

}