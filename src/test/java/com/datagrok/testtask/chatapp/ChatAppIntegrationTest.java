package com.datagrok.testtask.chatapp;

import com.datagrok.testtask.chatapp.model.Message;
import com.datagrok.testtask.chatapp.model.OutputMessage;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Log4j2
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ChatAppIntegrationTest {

    private static final String WEBSOCKET_TOPIC = "/topic/messages";
    private static final String USER_NAME = "Sofiia";
    private static final String MESSAGE_TEXT = "Hello Test!";
    private static final String DESTINATION = "/app/chat";

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private WebSocketHttpHeaders headers;

    @BeforeEach
    public void setup() {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        headers = new WebSocketHttpHeaders();

        this.stompClient = new WebSocketStompClient(sockJsClient);
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    public void testWebSocket_whenCorrectMessage_thenVerifyMessage() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        StompSessionHandler handler = new TestSessionHandler(failure) {

            @Override
            public void afterConnected(final StompSession session, StompHeaders connectedHeaders) {
                log.info("Connected to the WebSocket ...");

                session.subscribe(WEBSOCKET_TOPIC, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return OutputMessage.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        OutputMessage outputMessage = (OutputMessage) payload;
                        try {
                            assertThat(payload).isNotNull();
                            assertThat(payload).isInstanceOf(OutputMessage.class);
                            assertEquals(USER_NAME, outputMessage.getFrom());
                            assertEquals(MESSAGE_TEXT, outputMessage.getText());
                        } catch (Throwable t) {
                            failure.set(t);
                            log.error("There is an exception: ", t);
                        } finally {
                            session.disconnect();
                            latch.countDown();
                        }
                    }
                });
                log.info("Subscribed to receive messages from " + WEBSOCKET_TOPIC);

                try {
                    session.send(DESTINATION, new Message().setFrom(USER_NAME).setText(MESSAGE_TEXT));
                } catch (Throwable t) {
                    failure.set(t);
                    latch.countDown();
                }
            }
        };

        this.stompClient.connect("ws://localhost:{port}/chat", this.headers, handler, port);

        if (latch.await(3, TimeUnit.SECONDS)) {
            if (failure.get() != null) {
                throw new AssertionError("Assertion failed", failure.get());
            }
        } else {
            fail("Could not receive the message on time");
        }

    }

    private static class TestSessionHandler extends StompSessionHandlerAdapter {

        private final AtomicReference<Throwable> failure;

        public TestSessionHandler(AtomicReference<Throwable> failure) {
            this.failure = failure;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            this.failure.set(new Exception(headers.toString()));
        }

        @Override
        public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
            this.failure.set(ex);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable ex) {
            this.failure.set(ex);
        }
    }
}
