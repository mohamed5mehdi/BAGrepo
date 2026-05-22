package com.pfe.gestionsachat.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pfe.gestionsachat.model.TransferHeader;
import com.pfe.gestionsachat.model.TransferLine;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateLtoPdf(TransferHeader transfer) {
        return generateTransferDocument(transfer, "Bon de Sortie (Local Transfer Out)", transfer.getLtoNumber(), "LTO");
    }

    public byte[] generateLtiPdf(TransferHeader transfer) {
        return generateTransferDocument(transfer, "Bon d'Entrée (Local Transfer In)", transfer.getLtiNumber(), "LTI");
    }

    private byte[] generateTransferDocument(TransferHeader transfer, String titleStr, String docNumber, String type) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // En-tête : [Espace Logo] et Titre
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1f, 2f});

            // Espace Logo BAG
            PdfPCell logoCell = new PdfPCell(new Phrase("[ LOGO BAG ]\n(Espace réservé)", BOLD_FONT));
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(logoCell);

            // Titre du document
            Paragraph title = new Paragraph(titleStr, TITLE_FONT);
            title.setAlignment(Element.ALIGN_RIGHT);
            Paragraph subTitle = new Paragraph("N° " + docNumber, SUBTITLE_FONT);
            subTitle.setAlignment(Element.ALIGN_RIGHT);
            
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.addElement(title);
            titleCell.addElement(subTitle);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(titleCell);

            document.add(headerTable);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Informations générales
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10f);
            infoTable.setSpacingAfter(20f);

            infoTable.addCell(getCell("Demandeur : " + transfer.getRequestedBy().getNom(), NORMAL_FONT));
            infoTable.addCell(getCell("Date Demande : " + transfer.getCreatedAt().format(DATE_FORMATTER), NORMAL_FONT));
            
            infoTable.addCell(getCell("Entrepôt Source : " + transfer.getWarehouseSource().getName(), NORMAL_FONT));
            infoTable.addCell(getCell("Expédié le : " + (transfer.getShippedAt() != null ? transfer.getShippedAt().format(DATE_FORMATTER) : "N/A"), NORMAL_FONT));
            
            infoTable.addCell(getCell("Entrepôt Destination : " + transfer.getWarehouseDest().getName(), NORMAL_FONT));
            infoTable.addCell(getCell("Réceptionné le : " + (transfer.getReceivedAt() != null ? transfer.getReceivedAt().format(DATE_FORMATTER) : "N/A"), NORMAL_FONT));

            document.add(infoTable);

            // Lignes d'articles
            PdfPTable itemsTable = new PdfPTable(3);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{2f, 4f, 2f});

            itemsTable.addCell(getHeaderCell("Code Article"));
            itemsTable.addCell(getHeaderCell("Désignation"));
            itemsTable.addCell(getHeaderCell("Quantité Transférée"));

            for (TransferLine line : transfer.getLines()) {
                itemsTable.addCell(getCell(line.getStockItem().getItemCode(), NORMAL_FONT));
                itemsTable.addCell(getCell(line.getStockItem().getItemName(), NORMAL_FONT));
                itemsTable.addCell(getCell(String.valueOf(line.getQuantityRequested()), NORMAL_FONT));
            }

            document.add(itemsTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Zone de signatures
            PdfPTable signatureTable = new PdfPTable(2);
            signatureTable.setWidthPercentage(100);
            signatureTable.setSpacingBefore(30f);

            PdfPCell visaExp = new PdfPCell(new Phrase("Visa Expéditeur (" + transfer.getWarehouseSource().getName() + ")\n\n\n\n_______________________", NORMAL_FONT));
            visaExp.setBorder(Rectangle.NO_BORDER);
            visaExp.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            PdfPCell visaDest = new PdfPCell(new Phrase("Visa Destinataire (" + transfer.getWarehouseDest().getName() + ")\n\n\n\n_______________________", NORMAL_FONT));
            visaDest.setBorder(Rectangle.NO_BORDER);
            visaDest.setHorizontalAlignment(Element.ALIGN_CENTER);

            signatureTable.addCell(visaExp);
            signatureTable.addCell(visaDest);

            document.add(signatureTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    private PdfPCell getCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(8f);
        return cell;
    }

    private PdfPCell getHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BOLD_FONT));
        cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
        cell.setPadding(6f);
        return cell;
    }
}
