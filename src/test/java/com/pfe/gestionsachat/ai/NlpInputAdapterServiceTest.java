package com.pfe.gestionsachat.ai;

import com.pfe.gestionsachat.model.DemandeAchatInterne;
import com.pfe.gestionsachat.model.UrgenceDemande;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.UserRepository;
import com.pfe.gestionsachat.service.DemandeAchatInterneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour NlpInputAdapterService.
 * Vérifie : extraction NLP, respect des règles métier (délégation pure),
 * isolation transactionnelle et gestion des cas limites.
 */
@ExtendWith(MockitoExtension.class)
class NlpInputAdapterServiceTest {

    @Mock
    private DemandeAchatInterneService demandeService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NlpInputAdapterService nlpInputAdapterService;

    private User mockUser;
    private DemandeAchatInterne mockDemande;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setOidUser(1);

        mockDemande = new DemandeAchatInterne();
        mockDemande.setId(42L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 1 — Règle métier : délégation pure au Core Domain
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("T1 - Le service délègue SANS recoder la logique métier (createDemande appelé 1 fois)")
    void shouldDelegateToCoreDomainExactlyOnce() {
        // GIVEN
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(demandeService.createDemande(any(), any())).thenReturn(mockDemande);

        // WHEN
        DemandeAchatInterne result = nlpInputAdapterService.createDemandeFromPrompt("5 PC pour IT", 1);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(42L);
        // La règle métier est dans createDemande — on vérifie qu'il est appelé exactement 1 fois
        verify(demandeService, times(1)).createDemande(any(DemandeAchatInterne.class), eq(mockUser));
        verifyNoMoreInteractions(demandeService);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 2 — Extraction NLP : Quantité correctement parsée
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("T2 - Extraction quantité : '5 PC urgents' → quantite=5")
    void shouldExtractQuantityFromPrompt() {
        // GIVEN
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        ArgumentCaptor<DemandeAchatInterne> captor = ArgumentCaptor.forClass(DemandeAchatInterne.class);
        when(demandeService.createDemande(captor.capture(), any())).thenReturn(mockDemande);

        // WHEN
        nlpInputAdapterService.createDemandeFromPrompt("Il me faut 5 PC pour le service IT", 1);

        // THEN
        DemandeAchatInterne captured = captor.getValue();
        assertThat(captured.getQuantite()).isEqualTo(5);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 3 — Extraction NLP : Urgence URGENTE quand mot-clé détecté
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("T3 - Détection urgence : prompt contenant 'urgent' → UrgenceDemande.URGENTE")
    void shouldDetectUrgentKeyword() {
        // GIVEN
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        ArgumentCaptor<DemandeAchatInterne> captor = ArgumentCaptor.forClass(DemandeAchatInterne.class);
        when(demandeService.createDemande(captor.capture(), any())).thenReturn(mockDemande);

        // WHEN
        nlpInputAdapterService.createDemandeFromPrompt("3 cartouches encre URGENT pour demain", 1);

        // THEN
        assertThat(captor.getValue().getUrgence()).isEqualTo(UrgenceDemande.URGENTE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 4 — Extraction NLP : Urgence NORMALE quand aucun mot-clé
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("T4 - Pas de mot-clé urgence → UrgenceDemande.NORMALE")
    void shouldDefaultToNormalUrgence() {
        // GIVEN
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        ArgumentCaptor<DemandeAchatInterne> captor = ArgumentCaptor.forClass(DemandeAchatInterne.class);
        when(demandeService.createDemande(captor.capture(), any())).thenReturn(mockDemande);

        // WHEN
        nlpInputAdapterService.createDemandeFromPrompt("2 stylos pour le bureau", 1);

        // THEN
        assertThat(captor.getValue().getUrgence()).isEqualTo(UrgenceDemande.NORMALE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 5 — Quantité par défaut = 1 si aucun chiffre dans le prompt
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("T5 - Aucun chiffre dans le prompt → quantite=1 par défaut")
    void shouldDefaultQuantityToOneWhenNoNumberFound() {
        // GIVEN
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        ArgumentCaptor<DemandeAchatInterne> captor = ArgumentCaptor.forClass(DemandeAchatInterne.class);
        when(demandeService.createDemande(captor.capture(), any())).thenReturn(mockDemande);

        // WHEN
        nlpInputAdapterService.createDemandeFromPrompt("j'ai besoin d'un bureau", 1);

        // THEN
        assertThat(captor.getValue().getQuantite()).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 6 — Cas limite : userId inconnu → RuntimeException (aucune DA créée)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("T6 - userId introuvable → RuntimeException, createDemande JAMAIS appelé")
    void shouldThrowWhenUserNotFound() {
        // GIVEN
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() ->
            nlpInputAdapterService.createDemandeFromPrompt("5 PC urgents", 999)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("999");

        // La règle métier ne doit JAMAIS être atteinte si l'utilisateur est invalide
        verifyNoInteractions(demandeService);
    }
}
