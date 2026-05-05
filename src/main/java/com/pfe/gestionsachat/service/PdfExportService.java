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

    public byte[] generateGrnPdf(com.pfe.gestionsachat.model.GrnHeader grn) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph title = new Paragraph("BON DE RÉCEPTION", fontTitle);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontValue = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("N° Réception: " + grn.getId(), fontLabel));
            document.add(new Paragraph("N° BL: " + (grn.getDeliveryNoteNumber() != null ? grn.getDeliveryNoteNumber() : "—"), fontValue));
            document.add(new Paragraph("Date: " + grn.getReceiptDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontValue));
            if (grn.getSupplier() != null) document.add(new Paragraph("Fournisseur: " + grn.getSupplier().getNom(), fontLabel));
            if (grn.getPurchaseOrder() != null) document.add(new Paragraph("N° PO: " + grn.getPurchaseOrder().getIdPo(), fontValue));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{5f, 2.5f, 2.5f});
            
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(new Color(255, 152, 0)); // Amber
            cell.setPadding(5);
            Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            cell.setPhrase(new Phrase("Désignation", f)); table.addCell(cell);
            cell.setPhrase(new Phrase("Qté Commandée", f)); table.addCell(cell);
            cell.setPhrase(new Phrase("Qté Acceptée", f)); table.addCell(cell);

            Font fontData = FontFactory.getFont(FontFactory.HELVETICA, 11);
            if (grn.getDetails() != null) {
                for (com.pfe.gestionsachat.model.GrnDetails d : grn.getDetails()) {
                    table.addCell(new Phrase(d.getItemName(), fontData));
                    table.addCell(new Phrase(String.valueOf(d.getOrderedQuantity()), fontData));
                    table.addCell(new Phrase(String.valueOf(d.getAcceptedQuantity()), fontData));
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating GRN PDF", e);
        }
    }

    public byte[] generateGrcPdf(com.pfe.gestionsachat.model.GrcHeader grc) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph title = new Paragraph("BON DE VALORISATION (GRC)", fontTitle);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontValue = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("N° Valorisation: " + grc.getId(), fontLabel));
            if (grc.getCostingDate() != null) {
                document.add(new Paragraph("Date: " + grc.getCostingDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontValue));
            }
            if (grc.getGrnHeader() != null) {
                document.add(new Paragraph("N° Réception (GRN): " + grc.getGrnHeader().getId(), fontValue));
                if (grc.getGrnHeader().getPurchaseOrder() != null) {
                    document.add(new Paragraph("N° Bon de Commande: " + grc.getGrnHeader().getPurchaseOrder().getIdPo(), fontValue));
                }
            }
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 2f, 2f, 2f});
            
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(new Color(76, 175, 80)); // Green
            cell.setPadding(5);
            Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            cell.setPhrase(new Phrase("Désignation / Article", f)); table.addCell(cell);
            cell.setPhrase(new Phrase("Qté Acceptée", f)); table.addCell(cell);
            cell.setPhrase(new Phrase("Prix Unitaire", f)); table.addCell(cell);
            cell.setPhrase(new Phrase("Montant TTC", f)); table.addCell(cell);

            Font fontData = FontFactory.getFont(FontFactory.HELVETICA, 11);
            if (grc.getDetails() != null) {
                for (com.pfe.gestionsachat.model.GrcDetails d : grc.getDetails()) {
                    String label = (d.getItemCode() != null ? d.getItemCode() : "") + 
                                   (d.getGrnDetail() != null && d.getGrnDetail().getItemName() != null ? " - " + d.getGrnDetail().getItemName() : "");
                    table.addCell(new Phrase(label.isEmpty() ? "—" : label, fontData));
                    table.addCell(new Phrase(String.valueOf(d.getAcceptedQuantity()), fontData));
                    table.addCell(new Phrase(String.format("%.2f MAD", d.getUnitCost() != null ? d.getUnitCost() : 0.0), fontData));
                    table.addCell(new Phrase(String.format("%.2f MAD", d.getMontantTTC() != null ? d.getMontantTTC() : 0.0), fontData));
                }
            }
            document.add(table);

            Paragraph total = new Paragraph("Montant Total TTC: " + (grc.getTotalAmount() != null ? grc.getTotalAmount() : "0.00") + " MAD", fontTitle);
            total.setAlignment(Paragraph.ALIGN_RIGHT);
            total.setSpacingBefore(20);
            document.add(total);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating GRC PDF", e);
        }
    }
    public byte[] generateInvoicePdf(com.pfe.gestionsachat.model.Invoice invoice) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph title = new Paragraph("FACTURE FOURNISSEUR", fontTitle);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontValue = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("N° Facture: " + invoice.getInvoiceNumber(), fontLabel));
            if (invoice.getInvoiceDate() != null) {
                document.add(new Paragraph("Date: " + invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontValue));
            }
            if (invoice.getPurchaseOrder() != null) {
                document.add(new Paragraph("N° Bon de Commande: " + invoice.getPurchaseOrder().getIdPo(), fontValue));
            }
            document.add(new Paragraph("Statut: " + invoice.getStatus(), fontValue));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(new Color(33, 150, 243)); // Blue
            cell.setPadding(5);
            Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            cell.setPhrase(new Phrase("Description", f)); table.addCell(cell);
            cell.setPhrase(new Phrase("Montant", f)); table.addCell(cell);

            Font fontData = FontFactory.getFont(FontFactory.HELVETICA, 11);
            table.addCell(new Phrase("Montant Total Hors Taxe", fontData));
            table.addCell(new Phrase((invoice.getMontantHT() != null ? invoice.getMontantHT() : "0.00") + " MAD", fontData));
            
            table.addCell(new Phrase("Montant Total TTC", fontData));
            table.addCell(new Phrase((invoice.getMontantTTC() != null ? invoice.getMontantTTC() : "0.00") + " MAD", fontData));

            document.add(table);

            Paragraph total = new Paragraph("NET À PAYER: " + (invoice.getMontantTTC() != null ? invoice.getMontantTTC() : "0.00") + " MAD", fontTitle);
            total.setAlignment(Paragraph.ALIGN_RIGHT);
            total.setSpacingBefore(20);
            document.add(total);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Invoice PDF", e);
        }
    }
}
