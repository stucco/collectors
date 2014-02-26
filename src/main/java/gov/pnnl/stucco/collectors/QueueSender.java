package gov.pnnl.stucco.collectors;
/**
 * $OPEN_SOURCE_DISCLAIMER$
 */
import gov.pnnl.stucco.doc_service_client.DocServiceClient;
import gov.pnnl.stucco.doc_service_client.DocServiceException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONException;
import org.json.JSONObject;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class QueueSender {
    /** Configuration for RabbitMQ. */
    private Map<String, Object> rabbitMq;
    
    private DocServiceClient docServiceClient = null;
    
    /** Sets up a sender*/
    @SuppressWarnings("unchecked")
    public QueueSender() {
        Map<String, Object> defaultSection = (Map<String, Object>) Config.getMap().get("default");
        rabbitMq = (Map<String, Object>) defaultSection.get("rabbitmq");
    }
    
    /** Sends whatever a messages it receives */
    public void send(String msg) {
        try {
            String queueName = (String) rabbitMq.get("queue"); 
            sendFile(msg, queueName);   
        }
        catch (IOException e) {
          System.err.println("Unable to send message because of IOException");
        }
    }
    
    /** Sends a file to the specified queue. */
    private void sendFile(String msg, String queueName) throws IOException {
      //TODO: Refactor to reuse the connection/channel instead of creating anew each time
      
      try {
          String msgString = null; // will be set as either content string or document ID 
          
          // Build AMPQ Basic Properties
          AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
          Map<String, Object> headers = new HashMap<String, Object>();
          
          // Unpack the JSON, check the data size, send to doc service if above threshold 
          JSONObject json = new JSONObject(msg);
          
          String contentType = json.getString("contentType");
          String contentBase64 = json.getString("content");
          byte[] rawContent = DatatypeConverter.parseBase64Binary(contentBase64);
          
          //TODO: get MAX_CONTENT_SIZE from configuration
          final int MAX_CONTENT_SIZE = 1048576;
          if (rawContent.length > MAX_CONTENT_SIZE) {
              String docId = docServiceClient.store(rawContent, contentType);              
              msgString = docId;
              headers.put("content", "false");
          } else {
              msgString = msg;
              headers.put("content", "true");
          }              
          
          builder.contentType(contentType);
          builder.deliveryMode(2 /*persistent*/);
          builder.headers(headers);
          
          // Set up the connection
          ConnectionFactory factory = new ConnectionFactory();
          String host = (String) rabbitMq.get("host");
          factory.setHost(host);
          Connection connection = factory.newConnection();
          
          // Set up the channel with one queue
          Channel channel = connection.createChannel();
          channel.queueDeclare(queueName, false, false, false, null);
          
          //convert content into bytes
          byte[] messageBytes = msgString.getBytes();
          
          // Send the file as a message
          channel.basicPublish("", queueName, builder.build(), messageBytes);
          
          //TODO: write to log the first N bytes of the message
          System.out.println(" [x] Sent message ");

          // Close the connection/channel
          channel.close();
          connection.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }  
    
    static public void main(String[] args) {
      QueueSender sender = new QueueSender();
      sender.send("test.txt\nA test message from the queue sender\n");
    }

    public void setDocService(DocServiceClient docServiceClient) {
        this.docServiceClient = docServiceClient;
    }
    
    
}
