package gov.pnnl.stucco.collectors;

import gov.pnnl.stucco.utilities.CommandLine;
import gov.pnnl.stucco.utilities.CommandLine.UsageException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.io.StringWriter;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;

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
        String recordID = "";
        
        // reset record count
        numRecordsParsed = 0;
        
        try {    
            // Setup a new eventReader
            XMLEventReader eventReader    = mfactory.createXMLEventReader(input);
            StartElement rootStartElement = eventReader.nextTag().asStartElement();
            XMLEventFactory eventFactory  = XMLEventFactory.newFactory();
            StartDocument startDocument   = eventFactory.createStartDocument();
            EndDocument endDocument       = eventFactory.createEndDocument();
            
            XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
            
            // we cycle on the most outer loop as the repeating elements to capture
            while(eventReader.hasNext() && !eventReader.peek().isEndDocument()) {
                XMLEvent event = eventReader.nextTag();
                if(!event.isStartElement()) {
                    break;
                }
                
                StartElement breakStartElement = event.asStartElement();
                
                // this structure is used if we need to descend into a record and grab only a portion of the structure
                // as we would need to capture each "sub-event" within the structure
                // In our case we won't need to do that, yet..."
                List<XMLEvent> cachedXMLEvents = new ArrayList<XMLEvent>();
                cachedXMLEvents.add(breakStartElement);
                
                // get the CVE ID
                if (breakStartElement.getName().getLocalPart() == ("entry")) {
                    // We read the attributes from this tag
                    Iterator<Attribute> attributes = breakStartElement.getAttributes();
                    while (attributes.hasNext()) {
                        Attribute attribute = attributes.next();
                        if (attribute.getName().toString().equals("id")) {
                            recordID = attribute.getValue().toString();
                        }
                    }
                }
                
                // Create a buffer for where to write the fragment
                StringWriter stringWriter = new StringWriter();
                
                // A StAX XMLEventWriter will be used to write the XML fragment
                XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(stringWriter);
                eventWriter.add(startDocument);
                eventWriter.add(rootStartElement);
                
                // Write the XMLEvents that were cached while when we were
                // checking the fragment for some "sub-structure" we wanted, in our case we should
                // only have the startBreakElement
                for(XMLEvent cachedEvent : cachedXMLEvents) {
                    eventWriter.add(cachedEvent);
                }
                
                // Write the XMLEvents that we still need to parse from this fragment
                event = eventReader.nextEvent();
                while(eventReader.hasNext() && !(event.isEndElement() && event.asEndElement().getName().equals(breakStartElement.getName()))) {
                    eventWriter.add(event);
                    event = eventReader.nextEvent();
                }
                eventWriter.add(event);
                
                // Complete the record
                eventWriter.add(eventFactory.createEndElement(rootStartElement.getName(), null));
                eventWriter.add(endDocument);
                
                // fire event to unload record
                logger.info("Identified NVD Record: " + recordID);
                numRecordsParsed++;
                stringWriter.flush();
                String record = new String(stringWriter.toString());
                fireEvent(record.getBytes("UTF-8"));
                
            }
        } catch (XMLStreamException e) {
            // we skip this record if there is an error with this record
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
