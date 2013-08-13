package gov.pnnl.stucco.collectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/** 
 * Class that scans a directory and sends the files as messages.
 * Each file has its filename prepended to the content in the message.
 * 
 * @author Grant Nakamura, August 2013
 */
public class DirectorySender {

  private final static String QUEUE_NAME = "Test Queue";
  
  private final static String EOL = System.getProperty("line.separator");
  

  /** Directory to collect files from. */
  private File directory;
  
  
  /** Sets up a sender for a directory. */
  public DirectorySender(File dir) {
    if (!dir.isDirectory()) {
      throw new IllegalArgumentException(dir + "is not a directory");
    }
    
    directory = dir;
  }
  
  /** Collects and sends files from the directory. */
  public void send() {
    File[] files = directory.listFiles();
    for (File f : files) {
      try {
        sendFile(f, QUEUE_NAME);
      }
      catch (IOException e) {
        System.err.println("Unable to collect and send file '" + f + "' because of IOException");
      }
    }
  }
  
  /** Sends a file to the specified queue. */
  private void sendFile(File f, String queueName) throws IOException {
    //TODO: Refactor to reuse the connection/channel instead of creating anew each time
    
    // Set up the connection
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    
    // Set up the channel with one queue
    Channel channel = connection.createChannel();
    channel.queueDeclare(queueName, false, false, false, null);
    
    // Read the file
    String content = readFile(f);
    byte[] messageBytes = content.getBytes();
    
    // Send the file as a message
    channel.basicPublish("", queueName, null, messageBytes);
    System.out.println(" [x] Sent file '" + f + "'");

    // Close the connection/channel
    channel.close();
    connection.close();
  }
  
  
  /** 
   * Reads a file. 
   * 
   * @return String consisting of filename + EOL + content
   */
  private String readFile(File f) throws IOException {
    BufferedReader in = null;
    StringBuffer buffer = new StringBuffer();
    
    try {      
      in = new BufferedReader(new FileReader(f));
      
      // Write the filename
      String filename = f.getName();
      buffer.append(filename);
      buffer.append(EOL);
      
      String line;
      while ((line = in.readLine()) != null) {
        buffer.append(line);
        buffer.append(EOL);
      }
    }
    finally {
      if (in != null) {
        in.close();
      }
    }
    
    return buffer.toString();
  }
  
  
  static public void main(String[] args) {
    File dir = new File("Send");
    DirectorySender sender = new DirectorySender(dir);
    sender.send();
  }

}
