package com.datagrok.testtask.chatapp.controller;

import com.datagrok.testtask.chatapp.model.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class MessageController {

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public Message getMessages(Message message) {
        return message;
    }
}
