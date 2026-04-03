package com.mockhub.ticket.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.mockhub.auth.entity.User;
import com.mockhub.event.entity.Event;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.ticket.dto.TicketPdfData;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.Venue;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketPdfService {

    private static final Logger log = LoggerFactory.getLogger(TicketPdfService.class);

    private static final DateTimeFormatter EVENT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/New_York");

    private final TicketSigningService ticketSigningService;
    private final QrCodeService qrCodeService;
    private final String verificationBaseUrl;

    public TicketPdfService(TicketSigningService ticketSigningService,
                            QrCodeService qrCodeService,
                            @Value("${mockhub.ticket.verification-base-url}") String verificationBaseUrl) {
        this.ticketSigningService = ticketSigningService;
        this.qrCodeService = qrCodeService;
        this.verificationBaseUrl = verificationBaseUrl;
    }

    @Transactional(readOnly = true)
    public byte[] generateTicketPdf(OrderItem orderItem) {
        log.debug("Generating ticket PDF for order item with ticket ID {}",
                orderItem.getTicket().getId());

        Order order = orderItem.getOrder();
        Ticket ticket = orderItem.getTicket();
        Listing listing = orderItem.getListing();
        Event event = listing.getEvent();
        Venue venue = event.getVenue();
        User buyer = order.getUser();

        String orderNumber = order.getOrderNumber();
        Long ticketId = ticket.getId();
        String eventSlug = event.getSlug();
        String sectionName = ticket.getSection().getName();

        String rowLabel = null;
        String seatNumber = null;
        Seat seat = ticket.getSeat();
        if (seat != null) {
            rowLabel = seat.getRow().getRowLabel();
            seatNumber = seat.getSeatNumber();
        }

        String eventDateFormatted = formatInstant(event.getEventDate());

        Instant doorsOpenAt = event.getDoorsOpenAt();
        String doorsOpenFormatted = doorsOpenAt != null ? formatInstant(doorsOpenAt) : null;

        String token = ticketSigningService.generateToken(
                orderNumber, ticketId, eventSlug, sectionName, rowLabel, seatNumber);

        String verificationUrl = verificationBaseUrl + "/api/v1/tickets/verify?token=" + token;

        byte[] qrCodeImage = qrCodeService.generateQrCode(verificationUrl, 250);

        TicketPdfData ticketPdfData = new TicketPdfData(
                event.getName(),
                eventDateFormatted,
                doorsOpenFormatted,
                venue.getName(),
                venue.getCity() + ", " + venue.getState(),
                sectionName,
                rowLabel,
                seatNumber,
                ticket.getTicketType(),
                "$" + orderItem.getPricePaid().toPlainString(),
                orderNumber,
                buyer.getFirstName() + " " + buyer.getLastName(),
                qrCodeImage
        );

        return renderPdf(ticketPdfData);
    }

    private String formatInstant(Instant instant) {
        return EVENT_DATE_FORMATTER.format(instant.atZone(DISPLAY_ZONE));
    }

    private byte[] renderPdf(TicketPdfData data) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            float pageWidth = PDRectangle.LETTER.getWidth();
            float pageHeight = PDRectangle.LETTER.getHeight();
            float margin = 60;
            float contentWidth = pageWidth - 2 * margin;
            float yPosition = pageHeight - margin;

            try (PDPageContentStream contentStream =
                         new PDPageContentStream(document, page)) {

                // Header: "MOCKHUB"
                yPosition = drawText(contentStream, "MOCKHUB", fontBold, 12,
                        margin, yPosition, 0.5f, 0.5f, 0.5f);
                yPosition -= 25;

                // Event name
                yPosition = drawText(contentStream, data.eventName(), fontBold, 20,
                        margin, yPosition, 0f, 0f, 0f);
                yPosition -= 25;

                // Date/time
                yPosition = drawText(contentStream, data.eventDateFormatted(), fontRegular, 12,
                        margin, yPosition, 0.2f, 0.2f, 0.2f);
                yPosition -= 18;

                // Doors open (only if non-null)
                if (data.doorsOpenFormatted() != null) {
                    yPosition = drawText(contentStream, "Doors open: " + data.doorsOpenFormatted(),
                            fontRegular, 11, margin, yPosition, 0.3f, 0.3f, 0.3f);
                    yPosition -= 18;
                }

                // Venue + location
                yPosition = drawText(contentStream,
                        data.venueName() + " \u2014 " + data.venueLocation(),
                        fontRegular, 12, margin, yPosition, 0.2f, 0.2f, 0.2f);
                yPosition -= 25;

                // Horizontal line separator
                contentStream.setStrokingColor(0.8f, 0.8f, 0.8f);
                contentStream.setLineWidth(1f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(pageWidth - margin, yPosition);
                contentStream.stroke();
                yPosition -= 25;

                // Section
                yPosition = drawLabelValue(contentStream, "Section: ", data.sectionName(),
                        fontBold, fontRegular, 12, margin, yPosition);
                yPosition -= 18;

                // Row (if present)
                if (data.rowLabel() != null) {
                    yPosition = drawLabelValue(contentStream, "Row: ", data.rowLabel(),
                            fontBold, fontRegular, 12, margin, yPosition);
                    yPosition -= 18;
                }

                // Seat (if present)
                if (data.seatNumber() != null) {
                    yPosition = drawLabelValue(contentStream, "Seat: ", data.seatNumber(),
                            fontBold, fontRegular, 12, margin, yPosition);
                    yPosition -= 18;
                }

                // General Admission indicator when no seat
                if (data.rowLabel() == null && data.seatNumber() == null) {
                    yPosition = drawText(contentStream, "General Admission",
                            fontRegular, 12, margin, yPosition, 0.3f, 0.3f, 0.3f);
                    yPosition -= 18;
                }

                yPosition -= 5;

                // Ticket type + Price
                yPosition = drawLabelValue(contentStream, "Type: ",
                        data.ticketType().substring(0, 1).toUpperCase() + data.ticketType().substring(1),
                        fontBold, fontRegular, 12, margin, yPosition);
                yPosition -= 18;

                yPosition = drawLabelValue(contentStream, "Price: ", data.pricePaid(),
                        fontBold, fontRegular, 12, margin, yPosition);
                yPosition -= 18;

                // Order number
                yPosition = drawLabelValue(contentStream, "Order: ", data.orderNumber(),
                        fontBold, fontRegular, 11, margin, yPosition);
                yPosition -= 18;

                // Buyer name
                yPosition = drawLabelValue(contentStream, "Attendee: ", data.buyerName(),
                        fontBold, fontRegular, 11, margin, yPosition);
                yPosition -= 30;

                // QR code image (bottom-right area)
                float qrSize = 150;
                float qrX = pageWidth - margin - qrSize;
                float qrY = yPosition - qrSize;

                PDImageXObject qrImage = PDImageXObject.createFromByteArray(
                        document, data.qrCodeImage(), "qr-code");
                contentStream.drawImage(qrImage, qrX, qrY, qrSize, qrSize);

                // Footer text below QR code
                float footerY = qrY - 15;
                drawText(contentStream, "Scan QR code at venue entrance for verification",
                        fontRegular, 9, margin, footerY, 0.5f, 0.5f, 0.5f);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();

        } catch (IOException exception) {
            throw new java.io.UncheckedIOException("Failed to generate ticket PDF", exception);
        }
    }

    private float drawText(PDPageContentStream contentStream, String text,
                           PDType1Font font, float fontSize,
                           float x, float y,
                           float r, float g, float b) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(r, g, b);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y;
    }

    private float drawLabelValue(PDPageContentStream contentStream,
                                 String label, String value,
                                 PDType1Font labelFont, PDType1Font valueFont,
                                 float fontSize, float x, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(labelFont, fontSize);
        contentStream.setNonStrokingColor(0f, 0f, 0f);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(label);
        contentStream.endText();

        float labelWidth = labelFont.getStringWidth(label) / 1000 * fontSize;

        contentStream.beginText();
        contentStream.setFont(valueFont, fontSize);
        contentStream.setNonStrokingColor(0.2f, 0.2f, 0.2f);
        contentStream.newLineAtOffset(x + labelWidth, y);
        contentStream.showText(value);
        contentStream.endText();

        return y;
    }
}
