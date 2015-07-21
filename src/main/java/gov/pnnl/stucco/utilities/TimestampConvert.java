package gov.pnnl.stucco.utilities;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/** 
 * Utilities for converting to/from the HTTP 1.&nbsp;0 timestamp formats.
 * They are:
 * <pre>
 * <ul>
 * <li>Sun, 06 Nov 1994 08:49:37 GMT  ; RFC 822, updated by RFC 1123</li>
 * <li>Sunday, 06-Nov-94 08:49:37 GMT ; RFC 850, obsoleted by RFC 1036</li>
 * <li>Sun Nov  6 08:49:37 1994       ; ANSI C's asctime() format</li>
 * </ul>
 * </pre>
 * HTTP 1.1 requires clients and servers to read these formats, though they 
 * should only write RFC 1123 format. 
 * 
 * <p>Source: http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html 
 * (via Google search "http timestamp format").
 */
public class TimestampConvert {

    private static final SimpleDateFormat rfc1123Format;
    private static final SimpleDateFormat rfc1036Format;
    private static final SimpleDateFormat asctimeFormat;
    
    static {
        rfc1123Format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        rfc1123Format.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        rfc1036Format = new SimpleDateFormat("EEEEEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US);
        rfc1036Format.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        asctimeFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US);
        asctimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    /** Converts an RFC-1123-style timestamp to a Date. */
    public static Date rfc1123ToDate(String timestamp) throws ParseException {
        Date date = rfc1123Format.parse(timestamp);
        return date;
    }
    
    /** Converts a Date to an RFC-1123-style timestamp. */
    public static String dateToRfc1123(Date date) {
        String timestamp = rfc1123Format.format(date);
        return timestamp;
    }
    
    /** Converts an RFC-1036-style timestamp to a Date. */
    public static Date rfc1036ToDate(String timestamp) throws ParseException {
        Date date = rfc1036Format.parse(timestamp);
        return date;
    }
    
    /** Converts a Date to an RFC-1036-style timestamp. */
    public static String dateToRfc1036(Date date) {
        String timestamp = rfc1036Format.format(date);
        return timestamp;
    }
    
    /** Converts an asctime-style timestamp to a Date. */
    public static Date asctimeToDate(String timestamp) throws ParseException {
        Date date = asctimeFormat.parse(timestamp);
        return date;
    }
    
    /** Converts a Date to an asctime-style timestamp. */
    public static String dateToAsctime(Date date) {
        String timestamp = asctimeFormat.format(date);
        return timestamp;
    }
    
    /** 
     * Converts an HTTP-style timestamp to a Date. The timestamp could be any of
     * RFC 1123, RFC 1036, or C's asctime(). 
     * 
     * @throws ParseException if none of the formats apply. 
     */
    public static Date httpToDate(String timestamp) throws ParseException {
        Date date = null;
        
        // Try the different formats in order of preference
        try {
            date = rfc1123ToDate(timestamp);
        }
        catch (ParseException e) {
            try {                 
                date = rfc1036ToDate(timestamp);
            }
            catch (ParseException e1) {
                date = asctimeToDate(timestamp);
            }
        }
        
        return date;
    }
    
    /** Small test driver. */
    public static void main(String[] args) {
        try {
            // Convert RFC1123 to Date and back
            String before = "Sun, 08 Jun 2014 16:42:34 GMT";
            Date date = TimestampConvert.rfc1123ToDate(before);
            String after = TimestampConvert.dateToRfc1123(date);
            System.err.printf("Before: %s%n", before);
            System.err.printf("After : %s%n", after);
            
            // Convert RFC1036 to Date and back
            before = "Sunday, 08-Jun-14 16:42:34 GMT";
            date = TimestampConvert.rfc1036ToDate(before);
            after = TimestampConvert.dateToRfc1036(date);
            System.err.printf("Before: %s%n", before);
            System.err.printf("After : %s%n", after);
            
            // Convert asctime to Date and back
            before = "Sun Jun  8 16:42:34 2014";
            date = TimestampConvert.asctimeToDate(before);
            after = TimestampConvert.dateToAsctime(date);
            System.err.printf("Before: %s%n", before);
            System.err.printf("After : %s%n", after);
            date = TimestampConvert.asctimeToDate(after);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

    }

}
