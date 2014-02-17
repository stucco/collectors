package gov.pnnl.stucco.doc_service_client;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class DocServiceClientTest {

    private DocServiceClient client;
     
    /**
     * Sets up connection information
     */
    @Before
    public void setUp() {
        String host = "10.10.10.100";
        int port = 8118;
        client = new DocServiceClient(host, port);
    }
    
    @Test(expected=DocServiceException.class)
    public void testBadConnectionInfot() throws DocServiceException {
        DocServiceClient badClient = new DocServiceClient("localhost123", 0);
        badClient.store("hello");
    }
    
    @Test
    public void testSpecifyId() {
        String text = "Hello World";
        String id1 = "test_id";
        try {
            String id2 = client.store(new DocumentObject(text), id1);
            assertTrue(id1.equals(id2));
        } catch (DocServiceException e) {
            fail("DocServiceException");
            e.printStackTrace();
        }
    }
    
    @Test
    public void testStoreAndFetchString() {
        String text = "Hello World";
        try {
            String id = client.store(text);
            DocumentObject doc = client.fetch(id);
            assertTrue(text.equals(doc.getDataAsString()));
        } catch (DocServiceException e) {
            fail("DocServiceException");
            e.printStackTrace();
        }
    }
    
    @Test
    public void testStoreAndFetchJson() {
        JSONObject json = new JSONObject();
        try {
            json.append("primes", Arrays.asList(2,3,5,7,11));
            String id = client.store(new DocumentObject(json));
            DocumentObject doc = client.fetch(id);
            assertTrue(json.toString().equals(doc.getDataAsString()));
        } catch (JSONException e) {
            fail("JSONException");
            e.printStackTrace();
        } catch (DocServiceException e) {
            fail("DocServiceException");
            e.printStackTrace();
        }
    }
}
