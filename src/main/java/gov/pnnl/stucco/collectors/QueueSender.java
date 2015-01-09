package gov.pnnl.stucco.collectors;

/**
 * $OPEN_SOURCE_DISCLAIMER$
 */
import gov.pnnl.stucco.doc_service_client.DocServiceClient;
import gov.pnnl.stucco.doc_service_client.DocServiceException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for sending RabbitMQ messages.
 */
public class QueueSender {
    private static final Logger logger = LoggerFactory.getLogger(QueueSender.class);

    /** Configuration for RabbitMQ. */
    private Map<String, Object> rabbitMq;

    private DocServiceClient docServiceClient = null;

    private int maxMessageSize;

    /** Sets up a sender */
    @SuppressWarnings("unchecked")
    public QueueSender() {
        Map<String, Object> defaultSection = (Map<String, Object>) Config.getMap();
        rabbitMq = (Map<String, Object>) defaultSection.get("rabbitmq");
        determineMaxMessageSize();
    }

    /** 
     * Sends a content message. Depending on content size, this either sends
     * the content directly, or it first stores the document and sends the ID.
     *  
     * @deprecated 
     * Use {@link #sendIdMessage(Map, byte[])} instead. Retained for now for 
     * backward compatibility.
     */
    public void send(Map<String, String> metadata, byte[] rawContent) {
        // TODO: Refactor to reuse the connection/channel instead of creating
        // anew each time

        try {
            // Determine if the data should be sent straight to the queue or to the document service
            if (rawContent.length > maxMessageSize) {
                String docId = saveToDocumentStore(rawContent, metadata.get("contentType"));
                if (!docId.isEmpty()) {
                    sendIdMessage(metadata, docId.getBytes());
                }
            } else {
                sendRawContentMessage(metadata, rawContent);
            }
        } catch (DocServiceException e) {
            logger.error("Cannot send data", e);
        }
    }

    /** Determines and saves the threshold for message size. */
    private void determineMaxMessageSize() {
        String maxMessageSizeStr = (String) rabbitMq.get("message_size_limit");
        maxMessageSize = 10000000;
        try {
            maxMessageSize = Integer.parseInt(maxMessageSizeStr);
        } catch (NumberFormatException e) {
            // Just use the default
            logger.warn("Message size limit invalid; using default");
        }
    }

    public void setDocService(DocServiceClient docServiceClient) {
        this.docServiceClient = docServiceClient;
    }

    /** Sends an ID-based message to the message queue. */
    public void sendIdMessage(Map<String, String> metadata, byte[] messageBytes) {
        metadata.put("content", "false");
        prepareQueueAndSend(metadata, messageBytes);
    }
    
    /** Saves content to the document store, getting back the ID. */
    private String saveToDocumentStore(byte[] rawContent, String contentType) throws DocServiceException {
        if (docServiceClient == null) {
            logger.error("Cannot send data: document-service client has not been set");
            return "";
        }
        String docId = docServiceClient.store(rawContent, contentType);
        
        return docId;
    }
    
    /** Sends a raw-content message to the message queue. */
    private void sendRawContentMessage(Map<String, String> metadata, byte[] messageBytes) {
        metadata.put("content", "true");
        prepareQueueAndSend(metadata, messageBytes);        
    }
    
    /** Sets up message queue and sends a message. */
    private void prepareQueueAndSend(Map<String, String> metadata, byte[] messageBytes) {
        try {
            // Build AMPQ Basic Properties
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
            Map<String, Object> headers = new HashMap<String, Object>();
            headers.put("HasContent", metadata.get("content"));

            builder.contentType(metadata.get("contentType"));
            builder.deliveryMode(2 /* persistent */);
            builder.headers(headers);
            builder.timestamp(new java.util.Date());

            // Set up the connection
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

            // Set up the channel with exchange and queue
            String exchangeName = (String) rabbitMq.get("exchange");
            String dataType = metadata.get("dataType");
            String dataSource = metadata.get("sourceName");
            String sensorName = metadata.get("sensorName");
            String routingKey = "stucco.in." + dataType + "." + dataSource;
            if (sensorName != null) {
                routingKey = routingKey + "." + sensorName;
            }

            Channel channel = connection.createChannel();
            channel.exchangeDeclare(exchangeName, "topic", true, false, false, null);

            // Send the file as a message
            channel.basicPublish(exchangeName, routingKey, builder.build(), messageBytes);

            // write to log the first N bytes of the message
            int messSize = Math.min(messageBytes.length, 100); 
            String messageSubString = new String(messageBytes, 0, messSize, "UTF-8");
            if (messageBytes.length > 100) {
                messageSubString += "...";
            }
            logger.info("Sent message for: {}, {}",dataSource, messageSubString);
            
            logger.debug("RABBITMQ -> exchangeName: "+exchangeName+
                    "  dataType: "+dataType+
                    "  sensorName: "+sensorName+"  routingKey: "+ routingKey);

            // Close the connection/channel
            channel.close();
            connection.close();
        } catch (IOException e) {
            logger.error("Error sending data though message queue", e);
        }
    }

}
