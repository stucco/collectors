package gov.pnnl.stucco.collectors;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

public class CollectorFileByLineImpl extends CollectorAbstractBase implements Runnable {
    /** file from which we are obtaining content*/
    private File m_filename;
    
    /** content of the message to be sent */
    private String m_msgContent;
    
    /** raw content going into the message */
    private byte[] m_rawContent;
    
    /** time the data was collected */
    private Date m_timestamp = null;
    
    
    /** Sets up a sender for a directory. */
    public CollectorFileByLineImpl(Map<String, String> configData) {
      super(configData);
      
      String filename = configData.get("source-URI");
      File f = new File(filename);
      if (f.isDirectory()) {
        throw new IllegalArgumentException(f + "is a directory, not a file");
      }
      
      m_filename = f;
    }
    
    /** Collects the content in an independent thread */
    @Override
    public void collect() {
        Thread t = new Thread(this);
        t.start();
    }
    
    /** Collects the content and sends it to the queue in a message. */
    
    public void collectInThread() {
        // Read the file - line by line
        InputStream  fileInputStream;
        BufferedReader aBufferedReader;
        try {
            String aLine;
            
            fileInputStream = new FileInputStream(m_filename);
            aBufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, Charset.forName("UTF-8")));
            while ((aLine = aBufferedReader.readLine()) != null) {
                m_rawContent = aLine.getBytes();
                m_timestamp = new Date();
                m_msgContent = prepMessage(m_filename.getName(), m_rawContent);
                send();
            }
            clean();
            aBufferedReader.close();
        }
        catch (IOException e) {
          System.err.println("Unable to collect aline from '" + m_filename.toString() + "' because of IOException");
        }
        finally {           
            aBufferedReader = null;
            fileInputStream = null;
        }       
    }
    
    /**
     * Gets the binary content that was extracted from the source (in this case
     * the file).
     */
    public byte[] getRawContent() {
        return m_rawContent;
    }
    
    /** Sends the content. */
    public void send() {
        m_queueSender.send(m_msgContent);
    }
    
    /**
     * Preparing the message we will send into the queue
     * @param URI         URI of the file
     * @param rawContent  Byte content of the file
     * 
     * @return JSON encoding of the URI and content (which is itself 
     * base64-encoded within the JSON) 
     */
    private String prepMessage(String URI, byte[] rawContent) {
        String jsonContent = m_contentConverter.convertContent(URI, rawContent, m_timestamp);
        return jsonContent;
    }

    @Override
    public void clean() {
        m_rawContent = null;
        m_msgContent = null;
    }

    @Override
    public void run() {
        collectInThread();
    }  
    
}
