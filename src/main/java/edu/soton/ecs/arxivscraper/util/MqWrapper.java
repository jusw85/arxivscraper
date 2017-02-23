package edu.soton.ecs.arxivscraper.util;

import com.google.common.base.Preconditions;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.validation.constraints.NotNull;

public class MqWrapper implements AutoCloseable {

    private ConnectionFactory factory;
    private String clientId;
    private String destinationName;
    private boolean isTopic;

    private Connection connection = null;
    private Session session = null;
    private MessageProducer producer = null;
    private MessageConsumer consumer = null;

    private boolean isConnectionOpen = false;
    private boolean isConnectionStarted = false;

    public MqWrapper(ConnectionFactory factory, String clientId,
                     String destinationName, boolean isTopic) {
        this.factory = factory;
        this.clientId = clientId;
        this.destinationName = destinationName;
        this.isTopic = isTopic;
    }

    public void open() throws JMSException {
        if (!isConnectionOpen) {
            connection = factory.createConnection();
            connection.setClientID(clientId);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination;
            if (isTopic) {
                destination = session.createTopic(destinationName);
            } else {
                destination = session.createQueue(destinationName);
            }
            producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            consumer = session.createConsumer(destination);
            isConnectionOpen = true;
        }
    }

    public void sendTextMessage(@NotNull Object message) throws JMSException {
        Preconditions.checkNotNull(message);

        if (!isConnectionOpen) {
            open();
        }
        producer.send(session.createTextMessage(message.toString()));
    }

    public String receiveTextMessage() throws JMSException {
        startIncomingMessages();
        Message message = consumer.receive();
        if (message != null) {
            TextMessage textMessage = ((TextMessage) message);
            return textMessage.getText();
        }
        return null;
    }

    private void startIncomingMessages() throws JMSException {
        if (!isConnectionOpen) {
            open();
        }
        if (!isConnectionStarted) {
            connection.start();
            isConnectionStarted = true;
        }
    }

    public void stopIncomingMessages() throws JMSException {
        if (isConnectionOpen && isConnectionStarted) {
            connection.stop();
            isConnectionStarted = false;
        }
    }

    @Override
    public void close() throws Exception {
        if (producer != null)
            producer.close();
        if (consumer != null)
            consumer.close();
        if (session != null)
            session.close();
        if (connection != null) {
            connection.stop();
            connection.close();
        }
        isConnectionOpen = false;
        isConnectionStarted = false;
    }

}
