package gov.pnnl.stucco.collectors;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

public class CollectorFileImpl extends CollectorAbstractBase {
    /** file from which we are obtaining content*/
    private File m_filename;
    
    /** Sets up a sender for a directory. */
    public CollectorFileImpl(Map<String, String> configData) {
      super(configData);
      
      String filename = configData.get("source-URI");
      File f = new File(filename);
      if (f.isDirectory()) {
        throw new IllegalArgumentException(f + "is a directory, not a file");
      }
      
      m_filename = f;
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
        return m_rawContent;
    }
    
    /** Collects the content and retains it for later. */
    public void collectOnly() {
      try {
          // Read the file
          m_rawContent = readFile(m_filename);   
          m_timestamp = new Date();
      }
      catch (IOException e) {
        System.err.println("Unable to collect '" + m_filename.toString() + "' because of IOException");
      }
    }
        
    /** Reads the contents of a file. */
    public byte[] readFile(File file) throws IOException {
        int byteCount = (int) file.length();
        byte [] content = new byte[byteCount];
        DataInputStream in = new DataInputStream((new FileInputStream(file)));
        in.readFully(content);
        in.close();
        
        return content;
    }
    
    @Override
    public void clean() {
        m_rawContent = null;
    }  
    
}
