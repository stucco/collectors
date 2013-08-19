package gov.pnnl.stucco.collectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

public class CollectorFileImpl extends CollectorAbstractBase {
 private final static String EOL = System.getProperty("line.separator");
    
    /** file from which we are obtaining content*/
    private File m_filename;
    
    /** content of the message to be sent */
    private String m_msgContent;
    
    /** raw content going into the message */
    private String m_rawContent;
    
    /** time the data was collected */
    private Date m_timestamp = null;
    
    
    /** Sets up a sender for a directory. */
    public CollectorFileImpl(File fname) {
      if (fname.isDirectory()) {
        throw new IllegalArgumentException(fname + "is a directory, not a file");
      }
      
      m_filename = fname;
    }
    
    /**
     * perform the collect on this content type and send the result to the queue
     */
    @Override
    public void collect() {
        collectOnly();
        m_msgContent = this.prepMessage(m_filename.getName(), getRawContent());
        send();
    }
    
    /**
     * the content that was extracted from the source (in this case the file)
     * @return - the contents of the file
     */
    public String getRawContent() {
        return m_rawContent;
    }
    
    /**
     * Only collect the content the content and retain it for later.
     */
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
    
    /**
     * Send the content when requested
     */
    public void send() {
        queueSender.send(m_msgContent);
    }
        
    /** 
     * Reads the contents of the file. 
     * 
     * @return String consisting of filename + EOL + content
     */
    private String readFile(File f) throws IOException {
      BufferedReader in = null;
      StringBuffer buffer = new StringBuffer();
      
      try {      
        in = new BufferedReader(new FileReader(f));
               
        String line;
        while ((line = in.readLine()) != null) {
          buffer.append(line);
          buffer.append(EOL);
        }
      }
      finally {
        if (in != null) {
          in.close();
        }
      }
      
      return buffer.toString();
    }
    
    /**
     * Preparing the message we will send into the queue
     * @param URI
     * @param rawContent
     * @return
     */
    public String prepMessage(String URI, String rawContent) {
        StringBuffer buffer = new StringBuffer();
        
        // add the filename/URI
        buffer.append(URI);
        buffer.append(EOL);
        
        // add the content;  
        String content = contentConverter.convertContent(URI, rawContent, m_timestamp);
        buffer.append(content);
        
        m_msgContent = buffer.toString();
        return m_msgContent;
    }
    
    
    /**
     * Main
     * @param args  -  Requires
     */
    static public void main(String[] args) {
        try {
            File filename = new File("Send/malwaredomains-domains-short.txt");
            CollectorFileImpl collectFile= new CollectorFileImpl(filename);
            collectFile.collect();
            
        } catch (IllegalArgumentException e) {
            System.out.println("Error in finding file");
        }

    }
    
    
}
