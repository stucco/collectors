package gov.pnnl.stucco.utilities;

import gov.pnnl.stucco.collectors.PostProcessingException;
import gov.pnnl.stucco.utilities.TextExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.BoilerpipeContentHandler;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: add comments on class
 * @author d3e145
 *
 */
public class TextExtractor {
    private static final Logger logger = LoggerFactory.getLogger(TextExtractor.class);
    
    /**
     * Combines the metadata and extracted text content into a JSON document
     * The document uses the names as TIKA defines for the key value pairs in the metadata structure
     * and this method add the "content" key and the value from the content.
     * @param content - text extracted (or filtered) from the document
     * @param metadata - A map of extracted metadata from the document (author, title, pubdate) as found in the document
     * @return - as JSON document
     */
    private String createJSONObject(String content, Metadata metadata)
    {
        String jsonString = "";
        String JSONCONTENT = "content";
        
        try{
            JSONObject document = new JSONObject();
            
            document.put(JSONCONTENT, new String(content));
            String[] names = metadata.names();
            for (String name : names)
            {
                String[] value = metadata.getValues(name);
                document.put(name, value);
            }
            jsonString = document.toString(4);

        }catch(JSONException e){
            logger.error("Error putting content from TIKA into JSON object: " + e);
        }
        return jsonString;
    }
    
    /**
     * Puts the metadata and extracted text content into a Map.
     * The document uses the names that TIKA defines for the key value pairs in 
     * the metadata structure and this method adds the "content" key and the 
     * value from the content.
     * 
     * @param content - text extracted (or filtered) from the document
     * @param metadata - A map of extracted metadata from the document (author, title, pubdate) as found in the document
     * @return Map containing metadata and content
     */
    private Map<String, String> createMap(String content, Metadata metadata)
    {
        Map<String, String> dataMap = new HashMap<String, String>();
        dataMap.put("content", content);
        
        String[] names = metadata.names();
        for (String name : names) {
            String[] value = metadata.getValues(name);
            if (value.length > 0) {
                dataMap.put(name, value[0]);
            }
        }
        
        return dataMap;
    }
    
    /**
     * Parses an HTML document and filters all the HTML markup out and returns metadata that was found in the document
     * 
     * @param is - the input stream containing the document
     * @return - a Map representing the document and metadata
     * @throws PostProcessingException  if TIKA fails for whatever reason
     */
    public Map<String, String> parseHTML(InputStream is) throws PostProcessingException {
        Map<String, String> rtnValue = Collections.emptyMap();
        try {
            // Wrap regular handler with Boilerpipe handler
            BoilerpipeContentHandler contentHandler = new BoilerpipeContentHandler(new BodyContentHandler());
            
            // Parse content and metadata
            Metadata metadata = new Metadata();
            Parser parser = new AutoDetectParser();
            parser.parse(is, contentHandler, metadata, new ParseContext());
            
            // Bundle them
            String content = contentHandler.getTextDocument().getText(true, false);
            rtnValue = createMap(content, metadata);     
        }
        catch (IOException | TikaException  | SAXException e) {
            throw new PostProcessingException(e);
        }
        
        return rtnValue;
    }
    
    /**
     * Main for testing
     * @param args
     */
    public static void main(String... args) {
        File file = new File("test.html");
        try {
            InputStream is = new FileInputStream(file);
            TextExtractor te = new TextExtractor();
            Map<String, String> rtnValue = te.parseHTML(is);
            System.out.println(rtnValue);
        } catch (Exception e) {
           logger.error("Error "+ e); 
        }

    }

}
