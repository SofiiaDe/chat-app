package com.datagrok.testtask.chatapp.model;

import lombok.*;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Message {

    private String from;
    private String text;
}
