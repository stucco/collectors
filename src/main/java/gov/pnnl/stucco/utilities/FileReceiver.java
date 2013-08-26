package gov.pnnl.stucco.utilities;
/**
 * $OPEN_SOURCE_DISCLAIMER$
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class FileReceiver {
  /** RabbitMQ queue name. */
  private final static String QUEUE_NAME = "Test Queue";

  private final static String EOL = System.getProperty("line.separator");

  /** Directory in which to write received files. */
  private File directory;
  
  
  public FileReceiver(File dir) {
    directory = dir;
  }
  
  public void receive() {
    try {
      QueueingConsumer consumer = initConsumer();
      processMessagesForever(consumer);
    }
    catch (IOException ioe) {
      ioe.printStackTrace();
    }
    catch (InterruptedException e) {
      // Terminated
    }
  }

  private QueueingConsumer initConsumer() throws IOException {
    // Create a connection with one channel
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    // Set up a queue for the channel
    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    System.out.println(" [*] Waiting for messages. Interrupt to stop.");

    // TODO: Replace this? The RabbitMQ documentation says it's deprecated.
    // (though it isn't actually tagged with @deprecated.) 
    QueueingConsumer consumer = new QueueingConsumer(channel);
    channel.basicConsume(QUEUE_NAME, true, consumer);
    
    return consumer;
  }
  
  /** Process received messages until there's an interrupt. */
  private void processMessagesForever(QueueingConsumer consumer) throws InterruptedException {
    while (true) {
      // Get a message's contents
      String[] messagePart = unwrapMessage(consumer);
      String filename = messagePart[0];
      String content  = messagePart[1];
      
      // Save the received file
      File f = new File(directory, filename);
      try {
        System.out.println(" [x] Received file '" + filename + "'");
        writeFile(f, content);
      }
      catch (IOException e) {
        System.err.println("Unable to save file '" + f + "' because of IOException");
      }
      
    }
  }

  /** Extract the content from a message. */
  private String[] unwrapMessage(QueueingConsumer consumer) throws InterruptedException {
    // Get a message from a delivery
    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
    String message = new String(delivery.getBody());
    int messageLength = message.length();

    // Extract the filename
    int filenameLength = message.indexOf(EOL);
    String filename = message.substring(0, filenameLength);

    // Extract the content
    int contentStart = filenameLength + EOL.length();
    String content = message.substring(contentStart, messageLength);
    
    return new String[] { filename, content };
  }

  /** 
   * Writes a file to the Receive directory. 
   * 
   * @param f        File to write
   * @param content  Content of file
   */
  private void writeFile(File f, String content) throws IOException {
    BufferedWriter out = null;
    
    try {      
      out = new BufferedWriter(new FileWriter(f));
      out.write(content);
    }
    finally {
      if (out != null) {
        out.close();
      }
    }
  }

  public static void main(String[] argv) throws Exception {
    File receiveDir = new File("Receive");
    FileReceiver receiver = new FileReceiver(receiveDir);
    receiver.receive();
  }

}
