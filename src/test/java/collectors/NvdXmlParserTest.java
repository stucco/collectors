package collectors;

import gov.pnnl.stucco.collectors.NVDEvent;
import gov.pnnl.stucco.collectors.NVDListener;
import gov.pnnl.stucco.collectors.NVDXMLParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class NvdXmlParserTest extends TestCase
{
    private static int recordCount = 0;
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public NvdXmlParserTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( NvdXmlParserTest.class );
    }

    public static void recordCheck(byte[] record) {
        recordCount++;
    }
    
    public static void recordCheck2(byte[] record) {
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
     * Tests parsing 
     */
    public void testParse()
    {

        String xmlTestData = "<?xml version='1.0' encoding='UTF-8'?>" + 
                "<nvd xmlns:cvss=\"http://scap.nist.gov/schema/cvss-v2/0.2\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns=\"http://scap.nist.gov/schema/feed/vulnerability/2.0\" " +
                "xmlns:patch=\"http://scap.nist.gov/schema/patch/0.1\" " +
                "xmlns:scap-core=\"http://scap.nist.gov/schema/scap-core/0.1\" "+
                "xmlns:vuln=\"http://scap.nist.gov/schema/vulnerability/0.4\" "+
                "xmlns:cpe-lang=\"http://cpe.mitre.org/language/2.0\" nvd_xml_version=\"2.0\" "+
                "pub_date=\"2014-04-10T15:24:38\" " +
                "xsi:schemaLocation=\"http://scap.nist.gov/schema/patch/0.1 http://nvd.nist.gov/schema/patch_0.1.xsd http://scap.nist.gov/schema/scap-core/0.1 http://nvd.nist.gov/schema/scap-core_0.1.xsd http://scap.nist.gov/schema/feed/vulnerability/2.0 http://nvd.nist.gov/schema/nvd-cve-feed_2.0.xsd\">" +
                "<entry id=\"CVE-2003-0001\">" +
                "<vuln:vulnerable-configuration id=\"http://nvd.nist.gov/\">"+
                " <cpe-lang:logical-test negate=\"false\" operator=\"OR\">" +
                "<cpe-lang:fact-ref name=\"cpe:/o:freebsd:freebsd:4.2\"/>"+
                "<cpe-lang:fact-ref name=\"cpe:/o:linux:linux_kernel:2.4.1\"/>"+
                "<cpe-lang:fact-ref name=\"cpe:/o:microsoft:windows_2000:::advanced_server\"/>"+
                "<cpe-lang:fact-ref name=\"cpe:/o:microsoft:windows_2000_terminal_services::sp2\"/>"+
                "<cpe-lang:fact-ref name=\"cpe:/o:netbsd:netbsd:1.6\"/>" +
                "</cpe-lang:logical-test>" +
                "</vuln:vulnerable-configuration>" +
                "<vuln:vulnerable-software-list>" +
                "<vuln:product>cpe:/o:freebsd:freebsd:4.6</vuln:product>" +
                "<vuln:product>cpe:/o:microsoft:windows_2000::sp1:server</vuln:product>" +
                "</vuln:vulnerable-software-list>" +
                "<vuln:cve-id>CVE-2003-0001</vuln:cve-id>" +
                "<vuln:published-datetime>2003-01-17T00:00:00.000-05:00</vuln:published-datetime>"+
                "<vuln:last-modified-datetime>2008-09-10T15:17:21.290-04:00</vuln:last-modified-datetime>"+
                "<vuln:cvss>"+
                "<cvss:base_metrics upgraded-from-version=\"1.0\">"+
                "<cvss:score>5.0</cvss:score>"+
                "<cvss:access-vector approximated=\"true\">NETWORK</cvss:access-vector>"+
                "<cvss:access-complexity>LOW</cvss:access-complexity>"+
                "<cvss:authentication>NONE</cvss:authentication>"+
                "<cvss:confidentiality-impact>PARTIAL</cvss:confidentiality-impact>"+
                "<cvss:integrity-impact>NONE</cvss:integrity-impact>"+
                "<cvss:availability-impact>NONE</cvss:availability-impact>"+
                "<cvss:source>http://nvd.nist.gov</cvss:source>"+
                "<cvss:generated-on-datetime>2004-01-01T00:00:00.000-05:00</cvss:generated-on-datetime>"+
                " </cvss:base_metrics>"+
                "</vuln:cvss>"+
                " <vuln:assessment_check name=\"oval:org.mitre.oval:def:2665\" href=\"http://oval.mitre.org/repository/data/getDef?id=oval:org.mitre.oval:def:2665\" system=\"http://oval.mitre.org/XMLSchema/oval-definitions-5\"/>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"VENDOR_ADVISORY\">"+
                " <vuln:source>CERT-VN</vuln:source>"+
                "<vuln:reference href=\"http://www.kb.cert.org/vuls/id/412115\" xml:lang=\"en\">VU#412115</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:scanner>"+
                "<vuln:definition name=\"oval:org.mitre.oval:def:2665\" href=\"http://oval.mitre.org/repository/data/DownloadDefinition?id=oval:org.mitre.oval:def:2665\" system=\"http://oval.mitre.org/XMLSchema/oval-definitions-5\"/>"+
                "</vuln:scanner>"+
                "<vuln:summary>Multiple ethernet Network Interface Card (NIC) device drivers do not pad frames with null bytes, which allows remote attackers to obtain information from previous packets or kernel memory by using malformed packets, as demonstrated by Etherleak.</vuln:summary>"+
                "</entry>"+
                "<entry id=\"CVE-2003-0002\">"+
                "<vuln:vulnerable-configuration id=\"http://nvd.nist.gov/\">"+
                "<cpe-lang:logical-test negate=\"false\" operator=\"OR\">"+
                "<cpe-lang:fact-ref name=\"cpe:/a:microsoft:content_management_server:2001\"/>"+
                "<cpe-lang:fact-ref name=\"cpe:/a:microsoft:content_management_server:2001:sp1\"/>"+
                "</cpe-lang:logical-test>"+
                "</vuln:vulnerable-configuration>"+
                "<vuln:vulnerable-software-list>"+
                "<vuln:product>cpe:/a:microsoft:content_management_server:2001</vuln:product>"+
                "<vuln:product>cpe:/a:microsoft:content_management_server:2001:sp1</vuln:product>"+
                "</vuln:vulnerable-software-list>"+
                "<vuln:cve-id>CVE-2003-0002</vuln:cve-id>"+
                "<vuln:published-datetime>2003-02-07T00:00:00.000-05:00</vuln:published-datetime>"+
                "<vuln:last-modified-datetime>2008-09-10T20:05:22.087-04:00</vuln:last-modified-datetime>"+
                "<vuln:cvss>"+
                "<cvss:base_metrics upgraded-from-version=\"1.0\">"+
                "<cvss:score>6.8</cvss:score>"+
                "<cvss:access-vector approximated=\"true\">NETWORK</cvss:access-vector>"+
                "<cvss:access-complexity>MEDIUM</cvss:access-complexity>"+
                "<cvss:authentication>NONE</cvss:authentication>"+
                "<cvss:confidentiality-impact>PARTIAL</cvss:confidentiality-impact>"+
                "<cvss:integrity-impact>PARTIAL</cvss:integrity-impact>"+
                "<cvss:availability-impact>PARTIAL</cvss:availability-impact>"+
                "<cvss:source>http://nvd.nist.gov</cvss:source>"+
                "<cvss:generated-on-datetime>2004-01-01T00:00:00.000-05:00</cvss:generated-on-datetime>"+
                "</cvss:base_metrics>"+
                "</vuln:cvss>"+
                "<vuln:security-protection>ALLOWS_OTHER_ACCESS</vuln:security-protection>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"VENDOR_ADVISORY\">"+
                "<vuln:source>MS</vuln:source>"+
                "<vuln:reference href=\"http://www.microsoft.com/technet/security/bulletin/ms03-002.asp\" xml:lang=\"en\">MS03-002</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"VENDOR_ADVISORY\">"+
                "<vuln:source>XF</vuln:source>"+
                "<vuln:reference href=\"http://www.iss.net/security_center/static/10318.php\" xml:lang=\"en\">mcms-manuallogin-reasontxt-xss (10318)</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"VENDOR_ADVISORY\">"+
                "<vuln:source>BUGTRAQ</vuln:source>"+
                "<vuln:reference href=\"http://marc.theaimsgroup.com/?l=bugtraq&amp;m=103417794800719&amp;w=2\" xml:lang=\"en\">20021007 CSS on Microsoft Content Management Server</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"UNKNOWN\">"+
                "<vuln:source>BID</vuln:source>"+
                "<vuln:reference href=\"http://www.securityfocus.com/bid/5922\" xml:lang=\"en\">5922</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:summary>Cross-site scripting vulnerability (XSS) in ManualLogin.asp script for Microsoft Content Management Server (MCMS) 2001 allows remote attackers to execute arbitrary script via the REASONTXT parameter.</vuln:summary>"+
                "</entry>" +
                "</nvd>";
          
        // reset the count
        recordCount = 0;
        
        // get the content and play it
        NVDXMLParser parser = new NVDXMLParser();
        parser.addListener(new NVDListener() 
        {
            public void recordFound(NVDEvent e) {
                byte[] src = e.getRecord();
                recordCheck(src);
            }
        } );

        InputStream input = new ByteArrayInputStream(xmlTestData.getBytes());
        parser.parseForRecords(input);

        assertEquals("Record Count was not equal", recordCount, parser.getNumRecordsParsed());

    }
    
    public void testBadEntry()
    {

        String xmlTestData = "<?xml version='1.0' encoding='UTF-8'?>" + 
                "<nvd xmlns:cvss=\"http://scap.nist.gov/schema/cvss-v2/0.2\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns=\"http://scap.nist.gov/schema/feed/vulnerability/2.0\" " +
                "xmlns:patch=\"http://scap.nist.gov/schema/patch/0.1\" " +
                "xmlns:scap-core=\"http://scap.nist.gov/schema/scap-core/0.1\" "+
                "xmlns:vuln=\"http://scap.nist.gov/schema/vulnerability/0.4\" "+
                "xmlns:cpe-lang=\"http://cpe.mitre.org/language/2.0\" nvd_xml_version=\"2.0\" "+
                "pub_date=\"2014-04-10T15:24:38\" " +
                "xsi:schemaLocation=\"http://scap.nist.gov/schema/patch/0.1 http://nvd.nist.gov/schema/patch_0.1.xsd http://scap.nist.gov/schema/scap-core/0.1 http://nvd.nist.gov/schema/scap-core_0.1.xsd http://scap.nist.gov/schema/feed/vulnerability/2.0 http://nvd.nist.gov/schema/nvd-cve-feed_2.0.xsd\">" +
                "<entry id=\"CVE-2003-0001\">" +
                "<vuln:vulnerable-configuration id=\"http://nvd.nist.gov/\">"+
                " <cpe-lang:logical-test negate=\"false\" operator=\"OR\">" +
                "<cpe-lang:fact-ref name=\"cpe:/o:freebsd:freebsd:4.2\"/>"+
                "<cpe-lang:fact-ref name=\"cpe:/o:linux:linux_kernel:2.4.1\"/>"+
                "<cpe-lang:fact-ref name=\"cpe:/o:microsoft:windows_2000:::advanced_server\"/>"+
                "<cpe-lang:fact-ref name=\"cpe:/o:microsoft:windows_2000_terminal_services::sp2\"/>"+
                "<cpe-lang:fact-ref name=\"cpe:/o:netbsd:netbsd:1.6\"/>" +
                "</cpe-lang:logical-test>" +
                "</vuln:vulnerable-configuration>" +
                "<vuln:vulnerable-software-list>" +
                "<vuln:product>cpe:/o:freebsd:freebsd:4.6</vuln:product>" +
                "<vuln:product>cpe:/o:microsoft:windows_2000::sp1:server</vuln:product>" +
                "</vuln:vulnerable-software-list>" +
                "<vuln:cve-id>CVE-2003-0001</vuln:cve-id>" +
                "<vuln:published-datetime>2003-01-17T00:00:00.000-05:00</vuln:published-datetime>"+
                "<vuln:last-modified-datetime>2008-09-10T15:17:21.290-04:00</vuln:last-modified-datetime>"+
                "<vuln:cvss>"+
                "<cvss:base_metrics upgraded-from-version=\"1.0\">"+
                "<cvss:score>5.0</cvss:score>"+
                "<cvss:access-vector approximated=\"true\">NETWORK</cvss:access-vector>"+
                "<cvss:access-complexity>LOW</cvss:access-complexity>"+
                "<cvss:authentication>NONE</cvss:authentication>"+
                "<cvss:confidentiality-impact>PARTIAL</cvss:confidentiality-impact>"+
                "<cvss:integrity-impact>NONE</cvss:integrity-impact>"+
                "<cvss:availability-impact>NONE</cvss:availability-impact>"+
                "<cvss:source>http://nvd.nist.gov</cvss:source>"+
                "<cvss:generated-on-datetime>2004-01-01T00:00:00.000-05:00</cvss:generated-on-datetime>"+
                " </cvss:base_metrics>"+
                "</vuln:cvss>"+
                " <vuln:assessment_check name=\"oval:org.mitre.oval:def:2665\" href=\"http://oval.mitre.org/repository/data/getDef?id=oval:org.mitre.oval:def:2665\" system=\"http://oval.mitre.org/XMLSchema/oval-definitions-5\"/>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"VENDOR_ADVISORY\">"+
                " <vuln:source>CERT-VN</vuln:source>"+
                "<vuln:reference href=\"http://www.kb.cert.org/vuls/id/412115\" xml:lang=\"en\">VU#412115</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:scanner>"+
                "<vuln:definition name=\"oval:org.mitre.oval:def:2665\" href=\"http://oval.mitre.org/repository/data/DownloadDefinition?id=oval:org.mitre.oval:def:2665\" system=\"http://oval.mitre.org/XMLSchema/oval-definitions-5\"/>"+
                "</vuln:scanner>"+
                "<vuln:summary>Multiple ethernet Network Interface Card (NIC) device drivers do not pad frames with null bytes, which allows remote attackers to obtain information from previous packets or kernel memory by using malformed packets, as demonstrated by Etherleak.</vuln:summary>"+
                "</entry>"+
                "<entry1>"+
                "<vuln:vulnerable-configuration id=\"http://nvd.nist.gov/\">"+
                "<cpe-lang:logical-test negate=\"false\" operator=\"OR\">"+
                "<cpe-lang:fact-ref name=\"cpe:/a:microsoft:content_management_server:2001\"/>"+
                "<cpe-lang:fact-ref name=\"cpe:/a:microsoft:content_management_server:2001:sp1\"/>"+
                "</cpe-lang:logical-test>"+
                "</vuln:vulnerable-configuration>"+
                "<vuln:vulnerable-software-list>"+
                "<vuln:product>cpe:/a:microsoft:content_management_server:2001</vuln:product>"+
                "<vuln:product>cpe:/a:microsoft:content_management_server:2001:sp1</vuln:product>"+
                "</vuln:vulnerable-software-list>"+
                "<vuln:cve-id>CVE-2003-0002</vuln:cve-id>"+
                "<vuln:published-datetime>2003-02-07T00:00:00.000-05:00</vuln:published-datetime>"+
                "<vuln:last-modified-datetime>2008-09-10T20:05:22.087-04:00</vuln:last-modified-datetime>"+
                "<vuln:cvss>"+
                "<cvss:base_metrics upgraded-from-version=\"1.0\">"+
                "<cvss:score>6.8</cvss:score>"+
                "<cvss:access-vector approximated=\"true\">NETWORK</cvss:access-vector>"+
                "<cvss:access-complexity>MEDIUM</cvss:access-complexity>"+
                "<cvss:authentication>NONE</cvss:authentication>"+
                "<cvss:confidentiality-impact>PARTIAL</cvss:confidentiality-impact>"+
                "<cvss:integrity-impact>PARTIAL</cvss:integrity-impact>"+
                "<cvss:availability-impact>PARTIAL</cvss:availability-impact>"+
                "<cvss:source>http://nvd.nist.gov</cvss:source>"+
                "<cvss:generated-on-datetime>2004-01-01T00:00:00.000-05:00</cvss:generated-on-datetime>"+
                "</cvss:base_metrics>"+
                "</vuln:cvss>"+
                "<vuln:security-protection>ALLOWS_OTHER_ACCESS</vuln:security-protection>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"VENDOR_ADVISORY\">"+
                "<vuln:source>MS</vuln:source>"+
                "<vuln:reference href=\"http://www.microsoft.com/technet/security/bulletin/ms03-002.asp\" xml:lang=\"en\">MS03-002</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"VENDOR_ADVISORY\">"+
                "<vuln:source>XF</vuln:source>"+
                "<vuln:reference href=\"http://www.iss.net/security_center/static/10318.php\" xml:lang=\"en\">mcms-manuallogin-reasontxt-xss (10318)</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"VENDOR_ADVISORY\">"+
                "<vuln:source>BUGTRAQ</vuln:source>"+
                "<vuln:reference href=\"http://marc.theaimsgroup.com/?l=bugtraq&amp;m=103417794800719&amp;w=2\" xml:lang=\"en\">20021007 CSS on Microsoft Content Management Server</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"UNKNOWN\">"+
                "<vuln:source>BID</vuln:source>"+
                "<vuln:reference href=\"http://www.securityfocus.com/bid/5922\" xml:lang=\"en\">5922</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:summary>Cross-site scripting vulnerability (XSS) in ManualLogin.asp script for Microsoft Content Management Server (MCMS) 2001 allows remote attackers to execute arbitrary script via the REASONTXT parameter.</vuln:summary>"+
                "</entry>" +
                "</nvd>";
          
        // reset the count
        recordCount = 0;
        
        // get the content and play it
        NVDXMLParser parser = new NVDXMLParser();
        parser.addListener(new NVDListener() 
        {
            public void recordFound(NVDEvent e) {
                byte[] src = e.getRecord();
                recordCheck2(src);
            }
        } );

        InputStream input = new ByteArrayInputStream(xmlTestData.getBytes());
        parser.parseForRecords(input);

        assertEquals("Record Count was not equal", 1, parser.getNumRecordsParsed());

    }
    
    public void testSchemaChange()
    {

        String xmlTestData = "<?xml version='1.0' encoding='UTF-8'?>" + 
                "<nvd2 xmlns:cvss=\"http://scap.nist.gov/schema/cvss-v2/0.2\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns=\"http://scap.nist.gov/schema/feed/vulnerability/2.0\" " +
                "xmlns:patch=\"http://scap.nist.gov/schema/patch/0.1\" " +
                "xmlns:scap-core=\"http://scap.nist.gov/schema/scap-core/0.1\" "+
                "xmlns:vuln=\"http://scap.nist.gov/schema/vulnerability/0.4\" "+
                "xmlns:cpe-lang=\"http://cpe.mitre.org/language/2.0\" nvd_xml_version=\"2.0\" "+
                "pub_date=\"2014-04-10T15:24:38\" " +
                "xsi:schemaLocation=\"http://scap.nist.gov/schema/patch/0.1 http://nvd.nist.gov/schema/patch_0.1.xsd http://scap.nist.gov/schema/scap-core/0.1 http://nvd.nist.gov/schema/scap-core_0.1.xsd http://scap.nist.gov/schema/feed/vulnerability/2.0 http://nvd.nist.gov/schema/nvd-cve-feed_2.0.xsd\">" +
                "<entry id=\"CVE-2003-0002\">"+
                "<vuln:vulnerable-configuration id=\"http://nvd.nist.gov/\">"+
                "<cpe-lang:logical-test negate=\"false\" operator=\"OR\">"+
                "<cpe-lang:fact-ref name=\"cpe:/a:microsoft:content_management_server:2001\"/>"+
                "<cpe-lang:fact-ref name=\"cpe:/a:microsoft:content_management_server:2001:sp1\"/>"+
                "</cpe-lang:logical-test>"+
                "</vuln:vulnerable-configuration>"+
                "<vuln:vulnerable-software-list>"+
                "<vuln:product>cpe:/a:microsoft:content_management_server:2001</vuln:product>"+
                "<vuln:product>cpe:/a:microsoft:content_management_server:2001:sp1</vuln:product>"+
                "</vuln:vulnerable-software-list>"+
                "<vuln:cve-id>CVE-2003-0002</vuln:cve-id>"+
                "<vuln:published-datetime>2003-02-07T00:00:00.000-05:00</vuln:published-datetime>"+
                "<vuln:last-modified-datetime>2008-09-10T20:05:22.087-04:00</vuln:last-modified-datetime>"+
                "<vuln:cvss>"+
                "<cvss:base_metrics upgraded-from-version=\"1.0\">"+
                "<cvss:score>6.8</cvss:score>"+
                "<cvss:access-vector approximated=\"true\">NETWORK</cvss:access-vector>"+
                "<cvss:access-complexity>MEDIUM</cvss:access-complexity>"+
                "<cvss:authentication>NONE</cvss:authentication>"+
                "<cvss:confidentiality-impact>PARTIAL</cvss:confidentiality-impact>"+
                "<cvss:integrity-impact>PARTIAL</cvss:integrity-impact>"+
                "<cvss:availability-impact>PARTIAL</cvss:availability-impact>"+
                "<cvss:source>http://nvd.nist.gov</cvss:source>"+
                "<cvss:generated-on-datetime>2004-01-01T00:00:00.000-05:00</cvss:generated-on-datetime>"+
                "</cvss:base_metrics>"+
                "</vuln:cvss>"+
                "<vuln:security-protection>ALLOWS_OTHER_ACCESS</vuln:security-protection>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"VENDOR_ADVISORY\">"+
                "<vuln:source>MS</vuln:source>"+
                "<vuln:reference href=\"http://www.microsoft.com/technet/security/bulletin/ms03-002.asp\" xml:lang=\"en\">MS03-002</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"VENDOR_ADVISORY\">"+
                "<vuln:source>XF</vuln:source>"+
                "<vuln:reference href=\"http://www.iss.net/security_center/static/10318.php\" xml:lang=\"en\">mcms-manuallogin-reasontxt-xss (10318)</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"VENDOR_ADVISORY\">"+
                "<vuln:source>BUGTRAQ</vuln:source>"+
                "<vuln:reference href=\"http://marc.theaimsgroup.com/?l=bugtraq&amp;m=103417794800719&amp;w=2\" xml:lang=\"en\">20021007 CSS on Microsoft Content Management Server</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:references xml:lang=\"en\" reference_type=\"UNKNOWN\">"+
                "<vuln:source>BID</vuln:source>"+
                "<vuln:reference href=\"http://www.securityfocus.com/bid/5922\" xml:lang=\"en\">5922</vuln:reference>"+
                "</vuln:references>"+
                "<vuln:summary>Cross-site scripting vulnerability (XSS) in ManualLogin.asp script for Microsoft Content Management Server (MCMS) 2001 allows remote attackers to execute arbitrary script via the REASONTXT parameter.</vuln:summary>"+
                "</entry>" +
                "</nvd2>";
          
        // reset the count
        recordCount = 0;
        
        // get the content and play it
        NVDXMLParser parser = new NVDXMLParser();
        parser.addListener(new NVDListener() 
        {
            public void recordFound(NVDEvent e) {
                byte[] src = e.getRecord();
                recordCheck(src);
            }
        } );

        InputStream input = new ByteArrayInputStream(xmlTestData.getBytes());
        parser.parseForRecords(input);

        assertEquals("Record Count was not equal", 1, parser.getNumRecordsParsed());

    }
    public void testNoEntries()
    {

        String xmlTestData = "<?xml version='1.0' encoding='UTF-8'?>" + 
                "<nvd xmlns:cvss=\"http://scap.nist.gov/schema/cvss-v2/0.2\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns=\"http://scap.nist.gov/schema/feed/vulnerability/2.0\" " +
                "xmlns:patch=\"http://scap.nist.gov/schema/patch/0.1\" " +
                "xmlns:scap-core=\"http://scap.nist.gov/schema/scap-core/0.1\" "+
                "xmlns:vuln=\"http://scap.nist.gov/schema/vulnerability/0.4\" "+
                "xmlns:cpe-lang=\"http://cpe.mitre.org/language/2.0\" nvd_xml_version=\"2.0\" "+
                "pub_date=\"2014-04-10T15:24:38\" " +
                "xsi:schemaLocation=\"http://scap.nist.gov/schema/patch/0.1 http://nvd.nist.gov/schema/patch_0.1.xsd http://scap.nist.gov/schema/scap-core/0.1 http://nvd.nist.gov/schema/scap-core_0.1.xsd http://scap.nist.gov/schema/feed/vulnerability/2.0 http://nvd.nist.gov/schema/nvd-cve-feed_2.0.xsd\">" +
                "</nvd>";
          
        // reset the count
        recordCount = 0;
        
        // get the content and play it
        NVDXMLParser parser = new NVDXMLParser();
        parser.addListener(new NVDListener() 
        {
            public void recordFound(NVDEvent e) {
                byte[] src = e.getRecord();
                recordCheck(src);
            }
        } );

        InputStream input = new ByteArrayInputStream(xmlTestData.getBytes());
        parser.parseForRecords(input);

        assertEquals("Record Count was not equal", 0, parser.getNumRecordsParsed());

    }
}
