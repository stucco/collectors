package gov.pnnl.stucco.utilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.pnnl.stucco.collectors.CollectorWebPageImpl;
import gov.pnnl.stucco.collectors.Config;
import gov.pnnl.stucco.utilities.CommandLine.UsageException;

/** 
 * Debug class for testing out a regex scraping of a web page. 
 */
public class RegExScrapingTester {

    private String content;
    
    
    public RegExScrapingTester() {
    }

    /** Retrieves the webpage. */
    public void getWebPage(String uri) throws IOException
    {
        // Set up request
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setReadTimeout(15 * 1000);

        // Make request
        connection.connect();
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Get the content as a byte array
            try (
                    BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                    ByteArrayOutputStream out = new ByteArrayOutputStream()
            ) {
                // Get a chunk at a time 
                byte[] buffer = new byte[8192]; // 8K
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }

                byte[] bytes = out.toByteArray();
                content = new String(bytes);
            }
        }
    }

    /** Parses the entry URLs from the page content. */
    public List<String> scrapeUrls(String sourceUrl, String regEx) {
        List<String> urlList = new ArrayList<String>();
    
        try {
            // Prepare the regex to find the URLs
            Pattern pattern = Pattern.compile(regEx);
            Matcher matcher = pattern.matcher(content);
    
            URI source = new URI(sourceUrl);
            
            // For each match of the regex
            while (matcher.find()) {
                // Grab the matching URL
                String match = getFirstCapture(matcher);
                
                // If it's a relative path, convert to absolute
                match = source.resolve(match).toString();
                
                // Save it
                urlList.add(match);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        
        return urlList;
    }

    /** Gets the Matcher's first captured group (or null if nothing got captured. */
    private static String getFirstCapture(Matcher matcher) {
        // Check each capturing group
        int groupCount = matcher.groupCount();
        for (int i = 1; i <= groupCount; i++) {
            String group = matcher.group(i);
            
            if (group != null) {
                // Return the first one that captured anything
                return group;
            }
        }
        
        // Nothing captured
        return null;
    }

    public static void main(String[] args) {
        try {
            System.err.println("Args:");
            for (String arg : args) {
                System.err.println(arg);
            }
            System.err.println();
            
            CommandLine parser = new CommandLine();
            parser.add1("-regex");
            parser.require();
            parser.add1("-url");
            parser.require();
            parser.parse(args);
            
            String regex = parser.getValue("-regex");
            String url = parser.getValue("-url");
            
            System.err.printf("Regex: %s%n", regex);
            System.err.printf("URL: %s%n%n", url);
            System.err.println("Scraped URLs:");
            
            RegExScrapingTester tester = new RegExScrapingTester();
            tester.getWebPage(url);
            List<String> urlList = tester.scrapeUrls(url, regex);
            
            for (String scrapedUrl : urlList) {
                System.err.println(scrapedUrl);
            }
        } 
        catch (UsageException e) {
            System.err.println("Usage: -regex <regex> -url <url>");
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        
    }

}
