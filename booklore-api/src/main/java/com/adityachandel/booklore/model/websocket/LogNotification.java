package com.adityachandel.booklore.model.websocket;

import lombok.Getter;

import java.time.Instant;


@Getter
public class LogNotification {

    private final Instant timestamp = Instant.now();
    private final String message;

    public LogNotification(String message) {
        this.message = message;
    }

    public static LogNotification createLogNotification(String message) {
        return new LogNotification(message);
    }
}
