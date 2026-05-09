package com.pfe.gestionsachat.chatbot.service;

import com.pfe.gestionsachat.chatbot.model.ChatSessionStatut;
import com.pfe.gestionsachat.chatbot.repository.ChatSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChatSessionCleanupService {

    @Autowired
    private ChatSessionRepository sessionRepository;

    // Toutes les heures
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void nettoyerSessionsAbandonees() {
        LocalDateTime limite30min = LocalDateTime.now().minusMinutes(30);
        LocalDateTime limite3mois = LocalDateTime.now().minusMonths(3);

        // Abandonner les sessions actives inactives depuis plus de 30 minutes
        sessionRepository.findByStatutAndDateDernierMsgBefore(ChatSessionStatut.ACTIVE, limite30min)
                .forEach(s -> {
                    s.setStatut(ChatSessionStatut.ABANDONNEE);
                    sessionRepository.save(s);
                });

        // Supprimer physiquement les sessions abandonnées depuis plus de 3 mois
        sessionRepository.purgeSessionsPhysiquement(ChatSessionStatut.ABANDONNEE, limite3mois);
    }
}
