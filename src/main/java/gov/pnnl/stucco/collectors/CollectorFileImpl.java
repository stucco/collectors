package gov.pnnl.stucco.collectors;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

public class CollectorFileImpl extends CollectorFileBase {
    private static final Logger logger = LoggerFactory.getLogger(CollectorFileImpl.class);

    /** Sets up a sender for a directory. */
    public CollectorFileImpl(Map<String, String> configData) {
        super(configData);

        setFilenameFromConfig(configData);
    }

    /** Collects the content and sends it to the queue in a message. */
    @Override
    public void collect() {
        collectOnly();
        send();
        clean();
    }

    /**
     * Gets the binary content that was extracted from the source (in this case
     * the file).
     */
    public byte[] getRawContent() {
        return rawContent;
    }

    /** Collects the content and retains it for later. */
    public void collectOnly() {
        try {
            // Read the file
            rawContent = readFile(contentFile);
            timestamp = new Date();
        } catch (IOException e) {
            logger.error("Unable to collect '" + contentFile.toString() + "' because of IOException", e);
        }
    }

    /** Reads the contents of a file. */
    public byte[] readFile(File file) throws IOException {
        int byteCount = (int) file.length();
        byte[] content = new byte[byteCount];
        DataInputStream in = new DataInputStream((new FileInputStream(file)));
        in.readFully(content);
        in.close();

        return content;
    }

    @Override
    public void clean() {
        rawContent = null;
    }

}
