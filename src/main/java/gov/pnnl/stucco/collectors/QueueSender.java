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

public class QueueSender {
    private static final Logger logger = LoggerFactory.getLogger(QueueSender.class);

    /** Configuration for RabbitMQ. */
    private Map<String, Object> rabbitMq;

    private DocServiceClient docServiceClient = null;

    /** Sets up a sender */
    @SuppressWarnings("unchecked")
    public QueueSender() {
        Map<String, Object> defaultSection = (Map<String, Object>) Config.getMap();
        rabbitMq = (Map<String, Object>) defaultSection.get("rabbitmq");
    }

    /** Sends a file to the specified queue. */
    public void send(Map<String, String> metadata, byte[] rawContent) {
        // TODO: Refactor to reuse the connection/channel instead of creating
        // anew each time

        String maxMessageSizeStr = (String) rabbitMq.get("message_size_limit");
        int maxMessageSize = 10000000;
        try {
            maxMessageSize = Integer.parseInt(maxMessageSizeStr);
        } catch (NumberFormatException e) {
            // Just use the default
            logger.warn("Message size limit invalid; using default");
        }

        // Determine if the data should be sent straight to the queue or to the document service
        if (rawContent.length > maxMessageSize) {
            if (docServiceClient == null) {
                logger.error("Cannot send data: document-service client has not been set");
                return;
            }
            try {
                String docId = docServiceClient.store(rawContent, metadata.get("contentType"));
                metadata.put("content", "false");
                prepareQueueAndSend(metadata, docId.getBytes());
            } catch (DocServiceException e) {
                logger.error("Cannot send data", e);
            }
        } else {
            metadata.put("content", "true");
            prepareQueueAndSend(metadata, rawContent);
        }
    }

    static public void main(String[] args) {
        QueueSender sender = new QueueSender();
        // sender.send("test.txt\nA test message from the queue sender\n");
        // //TODO
    }

    public void setDocService(DocServiceClient docServiceClient) {
        this.docServiceClient = docServiceClient;
    }

    /** Sets up message queue and sends the data */
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
            Connection connection = factory.newConnection();

            // Set up the channel with exchange and queue
            String exchangeName = (String) rabbitMq.get("exchange");
            String dataSource = metadata.get("sourceName");
            String sensorName = metadata.get("sensorName");
            String routingKey = "stucco.in." + dataSource;
            if (sensorName != null) {
                routingKey = routingKey + "." + sensorName;
            }

            Channel channel = connection.createChannel();
            channel.exchangeDeclare(exchangeName, "topic", true, false, false, null);

            // Send the file as a message
            channel.basicPublish(exchangeName, routingKey, builder.build(), messageBytes);

            // TODO: write to log the first N bytes of the message
            logger.info(" [x] Sent message ");

            // Close the connection/channel
            channel.close();
            connection.close();
        } catch (IOException e) {
            logger.error("Error sending data though message queue", e);
        }
    }

}
