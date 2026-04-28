package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.DaDetails;
import com.pfe.gestionsachat.model.DaHeader;
import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.model.SubFamily;
import com.pfe.gestionsachat.repository.DaHeaderRepository;
import com.pfe.gestionsachat.repository.FamilyRepository;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ArticleAdditionTest {

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private SubFamilyRepository subFamilyRepository;

    @Autowired
    private DaHeaderRepository daHeaderRepository;

    @Test
    @Transactional
    public void testVerifieFamilleSousFamilleEtAjoutArticle() {
        // 1. Verifier et creer la famille et sous-famille
        Family family = new Family("Famille Test", new BigDecimal("10000"));
        family = familyRepository.save(family);
        assertNotNull(family.getIdFamily(), "La famille doit etre sauvegardee");

        SubFamily subFamily = new SubFamily("Sous-Famille Test", new BigDecimal("5000"), family);
        subFamily = subFamilyRepository.save(subFamily);
        assertNotNull(subFamily.getOidSub(), "La sous-famille doit etre sauvegardee");

        // 2. Fais un test interne d'ajout d'un article (DaDetails)
        DaHeader daHeader = new DaHeader("DA Test Ajout Article", null);

        DaDetails article = new DaDetails(daHeader, subFamily, 10, "Article Test", new BigDecimal("100"));
        article.setItemCode("ITEM-123");
        article.setItemName("Nom Article Test");

        daHeader.setDetails(Collections.singletonList(article));
        
        daHeader = daHeaderRepository.save(daHeader);

        assertNotNull(daHeader.getOidDa(), "Le DA doit etre sauvegarde");
        assertFalse(daHeader.getDetails().isEmpty(), "Les articles doivent etre presents");
        
        DaDetails savedArticle = daHeader.getDetails().get(0);
        assertNotNull(savedArticle.getOidDetail(), "L'article doit avoir un ID");
        assertEquals("Article Test", savedArticle.getDescription());
        assertEquals(subFamily.getOidSub(), savedArticle.getSubFamily().getOidSub());
    }
}
