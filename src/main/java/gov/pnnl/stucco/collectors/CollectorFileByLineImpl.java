package gov.pnnl.stucco.collectors;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

public class CollectorFileByLineImpl extends CollectorFileBase {
    private static final Logger logger = LoggerFactory.getLogger(CollectorFileByLineImpl.class);


    /** Sets up a sender for a directory. */
    public CollectorFileByLineImpl(Map<String, String> configData) {
        super(configData);
    }

    /** Collects the content and sends it to the queue in a message. */
    public void collect() {
        if (needToGet(contentFile)) {
            logger.info("Collecting File by Line: " + contentFile.toURI());
            
            // Read the file - line by line
            InputStream fileInputStream;
            BufferedReader aBufferedReader;
            try {
                String aLine;

                fileInputStream = new FileInputStream(contentFile);
                aBufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, Charset.forName("UTF-8")));
                while ((aLine = aBufferedReader.readLine()) != null) {
                    rawContent = aLine.getBytes();
                    timestamp = new Date();
                    send();
                }

                // Record the file in the metadata database
                updateFileMetadataRecord(contentFile);

                clean();
                aBufferedReader.close();
            } catch (IOException e) {
                logger.error("Unable to collect line from '" + contentFile.toString() + "' because of IOException", e);
            } finally {
                aBufferedReader = null;
                fileInputStream = null;
            }
        } else {
            logger.info("Collection not required: " + contentFile.toURI());
        }
    }

    /**
     * Gets the binary content that was extracted from the source (in this case
     * the file).
     */
    public byte[] getRawContent() {
        return rawContent;
    }

    @Override
    public void clean() {
        rawContent = null;
    }

}
