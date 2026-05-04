package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    public void notifyUser(User user, String title, String message) {
        if (user == null || user.getEmail() == null) return;
        // Real-time WebSocket notification
        messagingTemplate.convertAndSendToUser(user.getEmail(), "/topic/notifications", message);
        
        // Email notification (optional logic to only send emails for key steps)
        sendEmail(user.getEmail(), title, message);
    }

    public void notifyTopic(String topic, String message) {
        if (topic == null) return;
        messagingTemplate.convertAndSend("/topic/" + topic, message);
    }

    private void sendEmail(String to, String subject, String content) {
        if (mailSender == null) {
            System.out.println("Email non envoyé (pas de serveur SMTP configuré) à : " + to);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            Context context = new Context();
            context.setVariable("message", content);
            String html = templateEngine.process("email-template", context);
            
            helper.setText(html != null ? html : "", true);
            if (to != null) helper.setTo(to);
            if (subject != null) helper.setSubject(subject);
            helper.setFrom("pfe.gestionsachat@bag-group.com");
            
            // mailSender.send(mimeMessage); // Commuté en commentaire car pas de serveur SMTP configuré
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }
}
