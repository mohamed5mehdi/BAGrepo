package com.pfe.gestionsachat.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NotificationEventListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWorkflowNotification(WorkflowNotificationEvent event) {
        log.info("Envoi de notification asynchrone pour la DA {} : {}", 
                 event.getDa().getId(), event.getMessage());
        
        // Simuler l'envoi d'email ou WebSocket
        if (event.getTargetUser() != null && event.getTargetUser().getEmail() != null) {
            log.info("Notification envoyée à {}", event.getTargetUser().getEmail());
        }
    }
}
