package com.adityachandel.booklore.service;

import com.adityachandel.booklore.config.security.AuthenticationService;
import com.adityachandel.booklore.model.websocket.Topic;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AuthenticationService authenticationService;

    public void sendMessage(Topic topic, Object message) {
        try {
            var user = authenticationService.getAuthenticatedUser();
            if (user == null) {
                log.warn("No authenticated user found. Message not sent: {}", topic);
                return;
            }
            String username = user.getUsername();
            messagingTemplate.convertAndSendToUser(username, topic.getPath(), message);
        } catch (Exception e) {
            log.error("Error sending message to topic {}: {}", topic, e.getMessage(), e);
        }
    }
}