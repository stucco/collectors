package gov.pnnl.stucco.utilities;

import gov.pnnl.stucco.collectors.Config;
import gov.pnnl.stucco.doc_service_client.DocServiceClient;
import gov.pnnl.stucco.doc_service_client.DocServiceException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;


/**
 * Example class to demonstrate receiving of messages from RabbitMQ. 
 * 
 * <p> Two kinds of messages are supported, based on the custom HasContent 
 * header. For HasContent = true, the body is saved as a file to disk.
 * For HasContent = false, the body is interpreted as a list of UUIDs with
 * optional URLs. The UUIDs and URLs are simply logged.
 */
public class FileReceiver {
    private static final Logger logger = LoggerFactory.getLogger(FileReceiver.class);

    /** Client to fetch documents from document-service */
    DocServiceClient docServiceClient;

    /** Configuration for RabbitMQ. */
    private Map<String, Object> rabbitMq;

    /** Directory in which to write received files. */
    private File directory;   


    @SuppressWarnings("unchecked")
    public FileReceiver(File dir) {
        directory = dir;

        Map<String, Object> configMap = (Map<String, Object>) Config.getMap();
        rabbitMq = (Map<String, Object>) configMap.get("rabbitmq");

        Map<String, Object> stuccoConfig = (Map<String, Object>) configMap.get("stucco");
        Map<String, Object> docServiceConfig = (Map<String, Object>) stuccoConfig.get("document-service");

        try {
            docServiceClient = new DocServiceClient(docServiceConfig);
        } catch (DocServiceException e) {
            e.printStackTrace();
            Exit.exit(1);
        }
    }

    /** Activates this FileReceiver to start pulling from the queue. */
    public void receive() {
        try {
            QueueingConsumer consumer = initConsumer();
            processMessagesForever(consumer);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException e) {
            // Terminated
        }
    }

    /** Sets up the connection to the queue. */
    private QueueingConsumer initConsumer() throws IOException {
        // Create a connection with one channel
        ConnectionFactory factory = new ConnectionFactory();
        String host = (String) rabbitMq.get("host");
        factory.setHost(host);

        // Set port from config
        try {
            String portStr = (String) rabbitMq.get("port");
            if (portStr != null) {
                int port = Integer.parseInt(portStr);
                factory.setPort(port);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid port number from configuration; using RabbitMQ default", e);
        }

        String username = (String) rabbitMq.get("login");
        if (username != null) {
            factory.setUsername(username);
        }

        String password = (String) rabbitMq.get("password");
        if (password != null) {
            factory.setPassword(password);
        }

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Set up a queue for the channel
        final String EXCHANGE_NAME = (String) rabbitMq.get("exchange");
        channel.exchangeDeclare(EXCHANGE_NAME, "topic", true, false, false, null);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "stucco.in.#");

        logger.info(" [*] Waiting for messages. Interrupt to stop.");

        // TODO: Replace this? The RabbitMQ documentation says it's deprecated.
        // (though it isn't actually tagged with @deprecated.)
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);

        return consumer;
    }

    /** Process received messages until there's an interrupt. */
    private void processMessagesForever(QueueingConsumer consumer) throws InterruptedException {
        while (true) {
            // Wait for a message and extract or report its contents
            unwrapMessage(consumer);
        }
    }

    /** Extract the content from a message. */
    private void unwrapMessage(QueueingConsumer consumer) throws InterruptedException {
        // Get a message from a delivery
        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        String message = new String(delivery.getBody());
        String routingKey = delivery.getEnvelope().getRoutingKey();

        String printedStr = message;
        if (printedStr.length() > 20) {
            printedStr = printedStr.substring(0, 20) + "...";
        }
        logger.info(" [x] Received '" + routingKey + "':'" + printedStr + "'");

        BasicProperties properties = delivery.getProperties();
        Map<String, Object> headers = properties.getHeaders();

        if (headers == null) {
            logger.error("null headers");
            return;
        }

        String hasContent = headers.get("HasContent").toString();
        if (hasContent.equals("false")) {
            // Message consists of a listing of one or more lines,
            // where each line contains a UUID and an optional URL,
            // separated by whitespace.
            processNoncontentMessage(message);
        } else {
            // Has full content
            processContentMessage(message);
        }
    }
    
    /** Processes a message containing the actual file content. */
    private void processContentMessage(String message) {
        // Use a UUID for the filename
        String filename = UUID.randomUUID().toString();
        
        // Get the content
        byte[] content = message.getBytes();

        // Save the received file
        File f = new File(directory, filename);
        try {
            String indent = "     ";
            logger.info(indent + String.format("Writing file '%s'", filename));
            
            writeFile(f, content);
        } 
        catch (IOException e) {
            logger.error(String.format("Unable to save file '%s' because of IOException", f));
        }        
    }

    /** Processes a message containing document UUIDs with optional URLs. */
    private void processNoncontentMessage(String message) {
        // Extract the lines of the message
        String[] lines = message.split("\n");
        
        // Log the count
        String indent = "     ";
        String lineCountStr = pluralize(lines.length, "line"); 
        logger.info(indent + String.format("Message consists of %s", lineCountStr));
        
        indent += "    ";
        
        // For each line
        for (String line : lines) {
            // Default values
            String docId = "NONE";
            String url = "NONE";
            
            // Tokenize on whitespace
            String[] token = line.split("\\s+");
            
            // UUID
            if (token.length > 0) {
                docId = token[0];
            }
            
            // URL
            if (token.length > 1) {
                url = token[1];
            }
            
            // Log the UUID and URL
            logger.info(indent + String.format("ID: %s  URL: %s", docId, url));
            
            // NOTE: This is sufficient to demonstrate the message is received
            // correctly. We no longer retrieve the file from the document store.
        }
    }
    
    /** 
     * Gets the singular or plural form depending on the count.
     * 
     * @param n         The count
     * @param singular  Singular form of unit
     */
    static private String pluralize(int n, String singular) {
        String str = String.format("%d %s%s", n, singular, (n == 1)? "":"s");
        return str;
    }

    /**
     * Writes a file to the Receive directory.
     * 
     * @param f
     *            File to write
     * @param content
     *            Content of file
     */
    private void writeFile(File f, byte[] content) throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(f));
            out.write(content);

        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            CommandLine parser = new CommandLine();
            parser.add1("-file");
            parser.parse(args);

            if (parser.found("-file")) {
                // Set up config to be from file
                String configFilename = parser.getValue();
                Config.setConfigFile(new File(configFilename));
            }
            else {
                throw new CommandLine.UsageException("-file switch is required");
            }

            // Receive content. Uses config, so must be done after config source
            // is established.
            File receiveDir = new File("data/Receive");
            FileReceiver receiver = new FileReceiver(receiveDir);
            receiver.receive();
        } catch (CommandLine.UsageException e) {
            System.err.println("Usage: FileReceiver -file configFile");
            System.exit(1);
        }
    }

}
