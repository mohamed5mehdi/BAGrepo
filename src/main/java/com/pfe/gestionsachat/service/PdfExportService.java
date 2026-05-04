package com.pfe.gestionsachat.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pfe.gestionsachat.model.PurchaseOrder;
import com.pfe.gestionsachat.model.DaDetails;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfExportService {

    public byte[] generatePurchaseOrderPdf(PurchaseOrder po) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // Title
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph title = new Paragraph("BON DE COMMANDE", fontTitle);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Header Info
            Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontValue = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("N° Commande: " + po.getIdPo(), fontLabel));
            document.add(new Paragraph("Date: " + po.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontValue));
            document.add(new Paragraph("Statut: " + po.getStatut(), fontValue));
            
            if (po.getDaHeader() != null) {
                document.add(new Paragraph("N° Demande: " + po.getDaHeader().getOidDa(), fontValue));
                // On cherche le fournisseur dans le premier détail
                if (!po.getDaHeader().getDetails().isEmpty()) {
                    com.pfe.gestionsachat.model.Supplier supplier = po.getDaHeader().getDetails().get(0).getFournisseur();
                    if (supplier != null) {
                        document.add(new Paragraph("Fournisseur: " + supplier.getNom(), fontLabel));
                    }
                }
            }
            document.add(new Paragraph(" ")); // Spacer

            // Items Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setWidths(new float[]{4f, 1.5f, 2f, 2.5f});

            writeTableHeader(table);
            writeTableData(table, po);

            document.add(table);

            // Total
            Paragraph total = new Paragraph("Montant Total TTC: " + po.getMontantTotal() + " MAD", fontTitle);
            total.setAlignment(Paragraph.ALIGN_RIGHT);
            total.setSpacingBefore(20);
            document.add(total);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private void writeTableHeader(PdfPTable table) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(63, 81, 181)); // Indigo
        cell.setPadding(5);

        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);

        cell.setPhrase(new Phrase("Désignation", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Quantité", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Prix Unitaire", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Total HT", font));
        table.addCell(cell);
    }

    private void writeTableData(PdfPTable table, PurchaseOrder po) {
        if (po.getDaHeader() == null || po.getDaHeader().getDetails() == null) return;

        Font font = FontFactory.getFont(FontFactory.HELVETICA, 11);

        for (DaDetails detail : po.getDaHeader().getDetails()) {
            table.addCell(new Phrase(detail.getItemName() != null ? detail.getItemName() : detail.getDescription(), font));
            table.addCell(new Phrase(String.valueOf(detail.getQuantite()), font));
            table.addCell(new Phrase(detail.getPrixUnitaire() != null ? detail.getPrixUnitaire().toString() : "0", font));
            table.addCell(new Phrase(detail.getTotalPrice() != null ? detail.getTotalPrice().toString() : "0", font));
        }
    }
}
