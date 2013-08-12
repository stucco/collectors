package gov.pnnl.stucco.collectors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class FileReceiver {
  private final static String QUEUE_NAME = "Test Queue";

  private final static String EOL = System.getProperty("line.separator");

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
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    System.out.println(" [*] Waiting for messages. Interrupt to stop.");

    QueueingConsumer consumer = new QueueingConsumer(channel);
    channel.basicConsume(QUEUE_NAME, true, consumer);
    
    return consumer;
  }
  
  private void processMessagesForever(QueueingConsumer consumer) throws InterruptedException {
    while (true) {
      String[] messagePart = unwrapMessage(consumer);
      String filename = messagePart[0];
      String content  = messagePart[1];
      
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

  private String[] unwrapMessage(QueueingConsumer consumer) throws InterruptedException {
    QueueingConsumer.Delivery delivery = consumer.nextDelivery();

    String message = new String(delivery.getBody());
    int messageLength = message.length();

    int filenameLength = message.indexOf(EOL);
    String filename = message.substring(0, filenameLength);

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
