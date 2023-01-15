package com.datagrok.testtask.chatapp.controller;

import com.datagrok.testtask.chatapp.model.Message;
import com.datagrok.testtask.chatapp.model.OutputMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Log4j2
@Controller
public class MessageController {

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public OutputMessage getMessages(Message message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        log.info(message.getFrom() + " sent a message");
        return new OutputMessage(message.getFrom(), message.getText(), time);
    }
}
