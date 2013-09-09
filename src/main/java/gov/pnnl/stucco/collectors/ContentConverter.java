package gov.pnnl.stucco.collectors;
/**
 * $OPEN_SOURCE_DISCLAIMER$
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import javax.xml.bind.DatatypeConverter;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * takes the content and wraps it for transmission into the STUCCO pipeline
 * 
 * @author Shawn Bohn, August 2013
 */

public class ContentConverter { 

	public ContentConverter() {
	}
	
	/**
	 * converts the content into the correct content message
	 * 
	 * @param filename - name of the file the content came from (we need extension)
	 * @param content - the content of the file
	 * @param timestamp - the timestamp when the content was retrieved (or loaded)
	 * 
     * @return JSON encoding of the URI and content (which is itself 
     * base64-encoded within the JSON) 
	 */
	public String convertContent(String filename, byte[] binaryContent, Date timestamp) {
	    
		String msg = null;
		
		// extract the filename's extension
		String extension = extractExtension(filename);
		
		//TODO: Other source file formats (such as HTML and JSON)
		// call the appropriate collector converter
		if (extension.equalsIgnoreCase("xml")) {
			msg = convertToJson(filename, timestamp, binaryContent);
		} else {
		    // Default handling of a file
		    msg = convertToJson(filename, timestamp, binaryContent);
		}
		return msg;
	}
	
	/** Gets the extension (without dot) from a filename. */
	private String extractExtension(String filename) {
		String extension = null;
		String[] parts = filename.split("\\.");
		extension = parts[parts.length-1];  // take the last one
		return extension;
	}
	
	// TODO: Make timestamp semantics more consistent. Files use collection 
	// (read) time, while web pages use last modification time.
	/**
	 * Converts binary content into our JSON format
	 * 
	 * @param filename              Name of file being sent
	 * @param timestamp             Timestamp for file
	 * @param base64EncodedContent  Byte content of file, encoded as Base64
	 */
	private String convertToJson(String filename, Date timestamp, byte[] binaryContent) {
	    
	    try {
	        // Convert to Base64
	        String base64EncodedContent = DatatypeConverter.printBase64Binary(binaryContent);
	        
	        // Populate JSON
	        JSONObject json = new JSONObject();
	        json.put("dateCollected", timestamp.toString());

	        JSONObject sourceSection = new JSONObject();
	        sourceSection.put("name", filename);
	        sourceSection.put("URL", filename);
	        json.put("source", sourceSection);

	        json.put("contentType", "text/xml");
	        json.put("content", base64EncodedContent);
	        
	        return json.toString();
	    } 
	    catch (JSONException e) {
	        e.printStackTrace();
	        //TODO: need to resolve this!!!!
	        return "";
	    }
	}
	    
	// MAIN TEST PROGRAM
	static public void main(String[] args) throws IOException {
	    String cwd = System.getProperty("user.dir");
	    File dir = new File(cwd);
		File[] files = dir.listFiles();
		ContentConverter contConverter = new ContentConverter();
	    for (File f : files) {
	        BufferedReader in = null;
	        StringBuffer buffer = new StringBuffer();
	        
	        try {      
	          in = new BufferedReader(new FileReader(f));
	          
	          // Write the filename
	          String filename = f.getName();
	          buffer.append(filename);
	          buffer.append(System.getProperty("line.separator"));
	          
	          String line;
	          while ((line = in.readLine()) != null) {
	            buffer.append(line);
	            buffer.append(System.getProperty("line.separator"));
	          }
	        }
	        catch (IOException e) {
	            System.err.println("Unable to collect and send file '" + f + "' because of IOException");
	            if (in != null) {
	                  in.close();
                }
	            continue;
	          }
	        finally {
	            if (in != null) {
	              in.close();
	            }
	          }
	        Date timestamp = new Date();
	        
	        byte[] content = buffer.toString().getBytes();
	        String results = contConverter.convertContent(f.getAbsolutePath(), content, timestamp);
	        if(results != null) {
	            System.out.print(results);
	        } else {
	            System.out.printf("Could not process: %s\n", f.toString());
	        }
	    }
	}
}
