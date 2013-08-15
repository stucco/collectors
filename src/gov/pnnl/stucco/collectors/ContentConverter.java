package gov.pnnl.stucco.collectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

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
	public String convertContent(String filename, String content, Date timestamp) {
		String msg = null;
		
		// extract the filename's extension
		String extension = extractExtension(filename);
		
		// call the appropriate collector converter
		if(extension.compareTo("xml") == 0) {
			convertXML(filename, timestamp, content);
			msg = jsonConverter.toString();
		} else if( extension.compareTo("html") == 0) {
			
		} else if( extension.compareTo("json") == 0) {
			
		}
		return msg;
	}
	
	/**
	 * identifies the extension of the form <name.ext>
	 * @param filename 
	 * @return - returns the extension of the file
	 */
	private String extractExtension(String filename) {
		String extension = null;
		String[] parts = filename.split("\\.");
		extension = parts[parts.length-1];  // take the last one
		return extension;
	}
	
	/**
	 * converts XML content into our JSON format
	 * @param filename
	 * @param timestamp
	 * @param content
	 */
	private void convertXML(String filename, Date timestamp, String content) {
		jsonConverter.put("dateCollected", timestamp.toString());
		
		JSONObject jsonSource = new JSONObject();
		jsonSource.put("name", filename);
		jsonSource.put("URL", filename);
		jsonConverter.put("source", jsonSource);
		
		String encodedContent = encodeContent(content);
		jsonConverter.put("content", encodedContent);
	}

	/**
	 * encodes the content to fit within a json value, this means we're assuming only double quotation marks need to be escaped
	 * @param content
	 * @return - a json value encoded string
	 */
	private String encodeContent(String content) {
		String newContent = new String();
		newContent = content;
		newContent.replaceAll("\\\"", "\\\\\"");
		return newContent;
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
	        
	        String results = contConverter.convertContent(f.getAbsolutePath(), buffer.toString(), timestamp);
	        if(results != null) {
	            System.out.print(results);
	        } else {
	            System.out.printf("Could not process: %s\n", f.toString());
	        }
	    }
	}
}
