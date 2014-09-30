package gov.pnnl.stucco.collectors;

import gov.pnnl.stucco.utilities.CommandLine;
import gov.pnnl.stucco.utilities.CommandLine.UsageException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.io.StringWriter;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML extractor of CVE records from NVD XML structure
 * uses the STaX processing approach with events to find entry we are looking for.
 * when we have found a record we fire an event to send those record on their way to those that need them.
 * @author shawn
 *
 */
public class NVDXMLParser {

    private XMLInputFactory mfactory = null;
    
    /** logging for this class */
    protected static final Logger logger = LoggerFactory.getLogger(NVDXMLParser.class);
    
    /** listeners who want to know when we have identified a record */
    private Collection<NVDListener> mEventListeners = new HashSet<NVDListener>();
    
    /** how many records has this parser parsed, per call to parseForRecords*/
    private int numRecordsParsed = 0;
    
    /** 
     * Creates the XMLExtractor.
     * 
     * @param filename  
     * the filename which contains the XML data to be iterator over
     */
    public NVDXMLParser( )
    {
        mfactory = XMLInputFactory.newInstance();
        mfactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
    }
    
    /**
     * a total of records parsed after call to parseForRecords
     * @return
     */
    public int getNumRecordsParsed()
    {
        return numRecordsParsed;
    }
    /**
     * Extracts NVDCVE records from XML in a streaming event driven approach (STAX parsing)
     * ISSUE: the current parsing includes the namespace within the content, whereas before it was just in the header
     * @param input - xml stream
     */
    @SuppressWarnings({"unchecked"})
    public void parseForRecords(InputStream input) {
        
        // We need to walk each event to capture all the content within the record
        StringWriter writer = new StringWriter();
        StringWriter nvdWriter = new StringWriter();
        String recordID = "";
        String nvdHeader = "";
        boolean recording = false;
        
        // reset record count
        numRecordsParsed = 0;
        
        try {    
            // Setup a new eventReader
            XMLEventReader eventReader = mfactory.createXMLEventReader(input);
            
            // read the XML document
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
    
                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    // If we have an record element, we create a new record (NOTE: this value is NVD specific)
                    if (startElement.getName().getLocalPart() == ("entry")) {
                        writer = new StringWriter();
                        // We read the attributes from this tag and add the date
                        // attribute to our object
                        Iterator<Attribute> attributes = startElement.getAttributes();
                        while (attributes.hasNext()) {
                            Attribute attribute = attributes.next();
                            if (attribute.getName().toString().equals("id")) {
                                recordID = attribute.getValue().toString();
                                // we are only recording if we can get an ID
                                recording = true;
                            }
                        }
                    }
                    else if (startElement.getName().getLocalPart() == ("nvd")) {
                        event.writeAsEncodedUnicode(nvdWriter);
                        nvdHeader = new String(nvdWriter.toString());
                    
                    }
                }
                
                // Capture all content with this record
                if(recording) {
                    event.writeAsEncodedUnicode(writer);
                }
                
                // If we reach the end of an item element, capture the last element and fire event with record in tow
                if (event.isEndElement()) {
                    EndElement endElement = event.asEndElement();
                    if (endElement.getName().getLocalPart() == ("entry") && recording) {
                        logger.info("Identified NVD Record: " + recordID);
                        numRecordsParsed++;
                        recording = false;
                        writer.flush();
                        String record = new String(nvdHeader+"\n");
                        record = record.concat(writer.toString());
                        record = record.concat("\n</nvd>\n");
                        fireEvent(record.getBytes("UTF-8"));
                      
                    }
                }
            }
        } catch (XMLStreamException e) {
            logger.error("Hit exception processing XML stream: " + e.toString());
        } catch (UnsupportedEncodingException e) {
            logger.error("Unsupported Encoding: " + e.toString()); 
        }
    }
    
    /**
     * Listeners that are interested in events fired by this class
     * @param listener
     */
    public void addListener(NVDListener listener) {
        this.mEventListeners.add(listener);
    }
    
    /**
     * Fire off an event that we have a record
     * @param record - the contents of the record (in XML, without the header, and namespace info)
     */
    void fireEvent(byte[] record) {
        //construct the event
        NVDEvent event = new NVDEvent(record);
        for (NVDListener listener : mEventListeners) {
            listener.recordFound(event);
        }
    }
    
    /**
     * Extract content starting with a filename rather than a stream
     * @param filename
     */
    public void extract(String filename) {
        
        try {
            InputStream in = new FileInputStream(filename);
            parseForRecords(in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
    }
    
    /**
     * print out a record it that has been found (debugging only)
     * @param record - the XML record
     */
    public static void displayRecord(byte[] record) {
        try {
            String rString = new String(record, "UTF-8");
            System.out.println();
            System.out.print(rString);
            System.out.println();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } 
    }
    
    /**
     * @param args
     * @throws XMLStreamException
     */
    public static void main(String[] args) throws XMLStreamException {
        String filename = null;
        try {
            CommandLine parser = new CommandLine();
            parser.add1("-file");
            parser.parse(args);

            if (parser.found("-file")) {
                filename = parser.getValue();
            }
            
            // get the content and play it
            NVDXMLParser extractor = new NVDXMLParser();
            extractor.addListener(new NVDListener() 
            {
                public void recordFound(NVDEvent e) {
                    byte[] src = e.getRecord();
                    displayRecord(src);
                }
            } );
            
            extractor.extract(filename);
        } 
        catch (UsageException e) {
            System.err.println("Usage: NVDXMLExtractor -file filename");
            System.exit(1);
        }
    }
}
