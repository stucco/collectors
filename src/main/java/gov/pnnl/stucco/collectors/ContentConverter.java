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
 * @author Shawn Bohn
 *
 */

public class ContentConverter { 

	JSONObject jsonConverter;
	
	public ContentConverter() {
		jsonConverter = new JSONObject();
	}
	
	/**
	 * converts the content into the correct content message
	 * @param filename - name of the file the content came from (we need extension)
	 * @param content - the content of the file
	 * @param timestamp - the timestamp when the content was retrieved (or loaded)
	 * @return
	 */
	public String convertContent(String filename, byte[] content, Date timestamp) {
	    
	    String base64EncodedContent = DatatypeConverter.printBase64Binary(content);    

		String msg = null;
		
		// extract the filename's extension
		String extension = extractExtension(filename);
		
		// call the appropriate collector converter
		if (extension.equalsIgnoreCase("xml")) {
			convertXML(filename, timestamp, base64EncodedContent);
			msg = jsonConverter.toString();
//		} else if (extension.equalsIgnoreCase("html")) {
//			//TODO:
//	        msg = base64EncodedContent;
//		} else if (extension.equalsIgnoreCase("json")) {
//			//TODO:
//	        msg = base64EncodedContent;
		} else {
		    // Default handling of a file
		    convertXML(filename, timestamp, base64EncodedContent);
		    msg = jsonConverter.toString();
		}
		return msg;
	}
	
	/**
	 * identifies the extension of the form <name.ext>
	 * @param filename 
	 * @return - returns the extension of the file (without the dot)
	 */
	private String extractExtension(String filename) {
		String extension = null;
		String[] parts = filename.split("\\.");
		extension = parts[parts.length-1];  // take the last one
		return extension;
	}
	
	// TODO: Make timestamp semantics more consistent. Files use collection 
	// (read) time, while web pages use last modification time.
	/**
	 * converts XML content into our JSON format
	 * @param filename   Name of file being sent
	 * @param timestamp  Timestamp for file
	 * @param base64EncodedContent
	 */
	private void convertXML(String filename, Date timestamp, String base64EncodedContent) {
	    
	    try {
	        jsonConverter.put("dateCollected", timestamp.toString());

	        JSONObject jsonSource = new JSONObject();
	        jsonSource.put("name", filename);
	        jsonSource.put("URL", filename);
	        jsonConverter.put("source", jsonSource);

	        jsonConverter.put("contentType", "text/xml");
	        jsonConverter.put("content", base64EncodedContent);
	    } catch (JSONException e) {
	        e.printStackTrace();
	        //TODO: need to resolve this!!!!
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
