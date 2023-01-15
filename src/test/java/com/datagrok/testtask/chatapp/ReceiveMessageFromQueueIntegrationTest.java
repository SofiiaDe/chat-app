package com.datagrok.testtask.chatapp;

import com.datagrok.testtask.chatapp.model.Message;
import com.datagrok.testtask.chatapp.model.OutputMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReceiveMessageFromQueueIntegrationTest {

    private static final String WEBSOCKET_TOPIC = "/topic/messages";
    private static final String USER_NAME = "John";
    private static final String MESSAGE_TEXT = "Some text here";
    private static final int QUEUE_CAPACITY = 5;
    @LocalServerPort
    private int port;

    private BlockingQueue<OutputMessage> blockingQueue;
    private WebSocketStompClient stompClient;
    private Message message;

    @BeforeEach
    public void setup() {

        blockingQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));

        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        message = new Message().setFrom(USER_NAME).setText(MESSAGE_TEXT);

    }

    @Test
    public void shouldReceiveAMessageFromTheServer() throws Exception {
        StompSession session = stompClient
                .connect(getWsPath(), new StompSessionHandlerAdapter() {
                })
                .get(1, SECONDS);
        session.subscribe(WEBSOCKET_TOPIC, new DefaultStompFrameHandler());

        session.send(WEBSOCKET_TOPIC, message);
        assertEquals(MESSAGE_TEXT, blockingQueue.poll(1, SECONDS).getText());
    }

    class DefaultStompFrameHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            return OutputMessage.class;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object object) {
            blockingQueue.offer((OutputMessage) object);
        }
    }

    private String getWsPath() {
        return String.format("ws://localhost:%d/chat", port);
    }
}
