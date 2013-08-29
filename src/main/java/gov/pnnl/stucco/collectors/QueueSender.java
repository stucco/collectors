package gov.pnnl.stucco.collectors;
/**
 * $OPEN_SOURCE_DISCLAIMER$
 */
import java.io.IOException;
import java.util.Map;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class QueueSender {
    /** Configuration for RabbitMQ. */
    private Map<String, Object> rabbitMq;
    
    
    /** Sets up a sender*/
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
      
      // Set up the connection
      ConnectionFactory factory = new ConnectionFactory();
      String host = (String) rabbitMq.get("host");
      factory.setHost(host);
      Connection connection = factory.newConnection();
      
      // Set up the channel with one queue
      Channel channel = connection.createChannel();
      channel.queueDeclare(queueName, false, false, false, null);
      
      //convert content into bytes
      byte[] messageBytes = msg.getBytes();
      
      // Send the file as a message
      channel.basicPublish("", queueName, null, messageBytes);
      
      //TODO: write to log the first N bytes of the message
      System.out.println(" [x] Sent message ");

      // Close the connection/channel
      channel.close();
      connection.close();
    }  
    
    static public void main(String[] args) {
      QueueSender sender = new QueueSender();
      sender.send("test.txt\nA test message from the queue sender\n");
    }
    
    
}
