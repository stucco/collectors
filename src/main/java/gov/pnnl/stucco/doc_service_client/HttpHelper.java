package gov.pnnl.stucco.doc_service_client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

/**
 * This is a helper class to perform HTTP requests.
 */
public class HttpHelper {
    
    /**
     * Sends HTTP GET request
     * @param url the URL to connect to
     * @return Streamed response from server
     * @throws IOException
     * @throws ProtocolException
     */
    public static InputStream get(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("GET");
        con.setRequestProperty("accept", "application/octet-stream");
        return con.getInputStream();
    }
    
    /**
     * Sends HTTP PUT request
     * @param url the URL to connect to
     * @param contentType the data's content type
     * @param data the data to send to the server
     * @return Streamed response from server
     * @throws IOException
     * @throws ProtocolException
     */
    public static InputStream put(URL url, String contentType, byte[] data) 
            throws IOException, ProtocolException {
        
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("PUT");
        con.setRequestProperty("content-Type", contentType);

        OutputStream out = con.getOutputStream();
        try {
            out.write(data, 0, data.length);
            out.flush();
        } finally {
            out.close();
        }
        return con.getInputStream();
    }
}
