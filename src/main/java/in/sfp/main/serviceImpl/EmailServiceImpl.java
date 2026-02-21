package in.sfp.main.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import in.sfp.main.service.EmailService;

@Service
public class EmailServiceImpl implements EmailService {

        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailServiceImpl.class);

        @Autowired
        private JavaMailSender mailSender;

        @Autowired
        private in.sfp.main.service.WhatsAppService whatsAppService;

        @Value("${app.base.url}")
        private String baseUrl;

        @Value("${spring.mail.username}")
        private String fromEmail;

        private void logToFile(String message) {
                try {
                        java.nio.file.Files.write(java.nio.file.Paths.get("debug_log.txt"),
                                        (new java.util.Date() + ": [EMAIL] " + message + "\n").getBytes(),
                                        java.nio.file.StandardOpenOption.CREATE,
                                        java.nio.file.StandardOpenOption.APPEND);
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        @Override
        public void sendBookingConfirmation(String to, String customerName, String eventTitle, String eventDate,
                        String location, double amount) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject("Booking Confirmed: " + eventTitle);

                String content = String.format(
                                "Hello %s,\n\n" +
                                                "Your booking for the event '%s' has been confirmed!\n\n" +
                                                "Details:\n" +
                                                "Date: %s\n" +
                                                "Location: %s\n" +
                                                "Paid Amount: $%.2f\n\n" +
                                                "Thank you for choosing EventPro!\n\n" +
                                                "Best regards,\n" +
                                                "The EventPro Team",
                                customerName, eventTitle, eventDate, location, amount);

                message.setText(content);
                sendMail(message);
        }

        @Override
        public void sendEventUpdateNotification(String to, String customerName, String eventTitle, String changeType,
                        String details) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject("Important Update: " + eventTitle);

                String content = String.format(
                                "Hello %s,\n\n" +
                                                "There has been an update to the event '%s' that you are registered for.\n\n"
                                                +
                                                "Change Type: %s\n" +
                                                "New Details: %s\n\n" +
                                                "Please check your tickets section for more information.\n\n" +
                                                "Best regards\n" +
                                                "The EventPro Team",
                                customerName, eventTitle, changeType, details);

                message.setText(content);
                sendMail(message);
        }

        @Override
        public void sendEventCancellationNotification(String to, String customerName, String eventTitle,
                        String reason) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject("Event Cancelled: " + eventTitle);

                String content = String.format(
                                "Hello %s,\n\n" +
                                                "We regret to inform you that the event '%s' has been cancelled.\n\n" +
                                                "Reason: %s\n\n" +
                                                "If this was a paid event, your refund will be processed shortly.\n\n" +
                                                "Best regards,\n" +
                                                "The EventPro Team",
                                customerName, eventTitle, reason);

                message.setText(content);
                sendMail(message);
        }

        @Override
        public byte[] generateTicketPdfBytes(String customerName, String eventTitle, String eventDate,
                        String location, double amount, int bookingId, String organization, String userType,
                        String startTime, String endTime) throws Exception {
                java.io.ByteArrayOutputStream items = new java.io.ByteArrayOutputStream();
                com.lowagie.text.Document document = new com.lowagie.text.Document(
                                com.lowagie.text.PageSize.A4.rotate());
                com.lowagie.text.pdf.PdfWriter.getInstance(document, items);

                document.open();

                // -- COLORS --
                java.awt.Color primaryColor = new java.awt.Color(79, 70, 229);
                java.awt.Color bgColor = new java.awt.Color(248, 250, 252);
                java.awt.Color textColor = new java.awt.Color(17, 24, 39);
                java.awt.Color mutedColor = new java.awt.Color(107, 114, 128);
                java.awt.Color whiteColor = java.awt.Color.WHITE;

                // -- FONTS --
                com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory
                                .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 22, textColor);
                com.lowagie.text.Font headFont = com.lowagie.text.FontFactory
                                .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12, textColor);
                com.lowagie.text.Font subHeadFont = com.lowagie.text.FontFactory
                                .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 8, mutedColor);
                com.lowagie.text.Font bodyFont = com.lowagie.text.FontFactory
                                .getFont(com.lowagie.text.FontFactory.HELVETICA, 10, textColor);
                com.lowagie.text.Font smallFont = com.lowagie.text.FontFactory
                                .getFont(com.lowagie.text.FontFactory.HELVETICA, 8, mutedColor);

                com.lowagie.text.pdf.PdfPTable container = new com.lowagie.text.pdf.PdfPTable(2);
                container.setWidthPercentage(100);
                container.setWidths(new float[] { 2.5f, 1f });
                container.setSpacingBefore(10);

                // --- LEFT SIDE: MAIN TICKET ---
                com.lowagie.text.pdf.PdfPCell mainCell = new com.lowagie.text.pdf.PdfPCell();
                mainCell.setBackgroundColor(whiteColor);
                mainCell.setPadding(25);
                mainCell.setBorder(com.lowagie.text.Rectangle.BOX);
                mainCell.setBorderColor(new java.awt.Color(229, 231, 235));
                mainCell.setBorderWidth(1);

                com.lowagie.text.pdf.PdfPTable badgeTable = new com.lowagie.text.pdf.PdfPTable(1);
                badgeTable.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                badgeTable.setTotalWidth(80);
                badgeTable.setLockedWidth(true);
                com.lowagie.text.pdf.PdfPCell bCell = new com.lowagie.text.pdf.PdfPCell(
                                new com.lowagie.text.Phrase("OFFICIAL PASS",
                                                com.lowagie.text.FontFactory.getFont(
                                                                com.lowagie.text.FontFactory.HELVETICA_BOLD, 7,
                                                                primaryColor)));
                bCell.setBackgroundColor(new java.awt.Color(79, 70, 229, 25));
                bCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                bCell.setPadding(4);
                bCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                badgeTable.addCell(bCell);
                mainCell.addElement(badgeTable);

                com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph(eventTitle, titleFont);
                title.setSpacingBefore(10);
                title.setSpacingAfter(15);
                mainCell.addElement(title);

                com.lowagie.text.pdf.PdfPTable grid = new com.lowagie.text.pdf.PdfPTable(2);
                grid.setWidthPercentage(100);

                com.lowagie.text.pdf.PdfPCell g1 = new com.lowagie.text.pdf.PdfPCell();
                g1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                g1.addElement(new com.lowagie.text.Paragraph("DATE", subHeadFont));
                g1.addElement(new com.lowagie.text.Paragraph(eventDate, headFont));
                grid.addCell(g1);

                String timeStr = startTime != null ? (endTime != null ? startTime + " - " + endTime : startTime)
                                : "TBD";
                com.lowagie.text.pdf.PdfPCell g2 = new com.lowagie.text.pdf.PdfPCell();
                g2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                g2.addElement(new com.lowagie.text.Paragraph("TIME", subHeadFont));
                g2.addElement(new com.lowagie.text.Paragraph(timeStr, headFont));
                grid.addCell(g2);

                com.lowagie.text.pdf.PdfPCell g3 = new com.lowagie.text.pdf.PdfPCell();
                g3.setColspan(2);
                g3.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                g3.setPaddingTop(10);
                g3.addElement(new com.lowagie.text.Paragraph("VENUE", subHeadFont));
                g3.addElement(new com.lowagie.text.Paragraph(location, headFont));
                grid.addCell(g3);

                grid.setSpacingAfter(20);
                mainCell.addElement(grid);

                com.lowagie.text.pdf.PdfPTable attendeeTableWrap = new com.lowagie.text.pdf.PdfPTable(1);
                attendeeTableWrap.setWidthPercentage(100);
                com.lowagie.text.pdf.PdfPCell attendeeBoxCell = new com.lowagie.text.pdf.PdfPCell();
                attendeeBoxCell.setBackgroundColor(bgColor);
                attendeeBoxCell.setPadding(15);
                attendeeBoxCell.setBorder(com.lowagie.text.Rectangle.BOX);
                attendeeBoxCell.setBorderColor(new java.awt.Color(229, 231, 235));

                com.lowagie.text.pdf.PdfPTable aTable = new com.lowagie.text.pdf.PdfPTable(2);
                aTable.setWidthPercentage(100);

                com.lowagie.text.pdf.PdfPCell a1 = new com.lowagie.text.pdf.PdfPCell();
                a1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                a1.addElement(new com.lowagie.text.Paragraph("ATTENDEE NAME", subHeadFont));
                a1.addElement(new com.lowagie.text.Paragraph(customerName,
                                com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 14,
                                                primaryColor)));
                aTable.addCell(a1);

                com.lowagie.text.pdf.PdfPCell a2 = new com.lowagie.text.pdf.PdfPCell();
                a2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                a2.addElement(new com.lowagie.text.Paragraph("ORGANIZATION", subHeadFont));
                a2.addElement(new com.lowagie.text.Paragraph(organization != null ? organization : "-", bodyFont));
                aTable.addCell(a2);

                com.lowagie.text.pdf.PdfPCell a3 = new com.lowagie.text.pdf.PdfPCell();
                a3.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                a3.setPaddingTop(10);
                a3.addElement(new com.lowagie.text.Paragraph("ATTENDEE TYPE", subHeadFont));
                a3.addElement(new com.lowagie.text.Paragraph(userType != null ? userType : "Guest", bodyFont));
                aTable.addCell(a3);

                com.lowagie.text.pdf.PdfPCell a4 = new com.lowagie.text.pdf.PdfPCell();
                a4.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                a4.setPaddingTop(10);
                a4.addElement(new com.lowagie.text.Paragraph("PRICE / INFO", subHeadFont));
                String pText = amount > 0 ? String.format("₹%.2f", amount) : "FREE";
                a4.addElement(new com.lowagie.text.Paragraph(pText, bodyFont));
                aTable.addCell(a4);

                attendeeBoxCell.addElement(aTable);
                attendeeTableWrap.addCell(attendeeBoxCell);
                mainCell.addElement(attendeeTableWrap);
                container.addCell(mainCell);

                // --- RIGHT SIDE: STUB ---
                com.lowagie.text.pdf.PdfPCell stubCell = new com.lowagie.text.pdf.PdfPCell();
                stubCell.setBackgroundColor(new java.awt.Color(250, 250, 250));
                stubCell.setPadding(20);
                stubCell.setBorder(com.lowagie.text.Rectangle.BOX);
                stubCell.setBorderColor(new java.awt.Color(229, 231, 235));
                stubCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);

                try {
                        String validationUrl = baseUrl + "/api/bookings/validate/" + bookingId;
                        byte[] qrBytes = generateQRCodeImage(validationUrl, 150, 150);
                        com.lowagie.text.Image qrImg = com.lowagie.text.Image.getInstance(qrBytes);
                        qrImg.scaleAbsolute(100, 100);
                        qrImg.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                        stubCell.addElement(qrImg);
                } catch (Exception qre) {
                        stubCell.addElement(new com.lowagie.text.Paragraph("[QR CODE]", smallFont));
                }

                com.lowagie.text.Paragraph idLabel = new com.lowagie.text.Paragraph("ORDER ID", subHeadFont);
                idLabel.setSpacingBefore(15);
                idLabel.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                stubCell.addElement(idLabel);

                com.lowagie.text.Paragraph idVal = new com.lowagie.text.Paragraph("#ORD-" + bookingId,
                                com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.COURIER_BOLD, 10,
                                                mutedColor));
                idVal.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                stubCell.addElement(idVal);

                com.lowagie.text.Paragraph miniFooter = new com.lowagie.text.Paragraph(
                                "Valid for single entry only.\nPlease carry a valid ID.",
                                com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 6,
                                                mutedColor));
                miniFooter.setSpacingBefore(15);
                miniFooter.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                stubCell.addElement(miniFooter);
                container.addCell(stubCell);

                document.add(container);

                com.lowagie.text.Paragraph pageFooter = new com.lowagie.text.Paragraph(
                                "Managed by EventPro Systems. This is a computer generated ticket.", smallFont);
                pageFooter.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                pageFooter.setSpacingBefore(20);
                document.add(pageFooter);

                document.close();
                return items.toByteArray();
        }

        @Override
        public void sendBookingConfirmationWithTicket(String to, String customerName, String eventTitle,
                        String eventDate,
                        String location, double amount, int bookingId, String organization, String userType,
                        String mobile, String startTime, String endTime) {
                logToFile("sendBookingConfirmationWithTicket START for: " + to);
                try {
                        byte[] pdfBytes = generateTicketPdfBytes(customerName, eventTitle, eventDate, location, amount,
                                        bookingId, organization, userType, startTime, endTime);

                        jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
                        org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                                        mimeMessage, true, "UTF-8");

                        helper.setFrom(fromEmail);
                        helper.setTo(to);
                        helper.setSubject("Your Ticket for " + eventTitle);

                        String emailContent = String.format(
                                        "Hello %s,\n\n" +
                                                        "Your booking for '%s' is confirmed. Please find your official ticket attached below.\n\n"
                                                        +
                                                        "Event Details:\n" +
                                                        "- Date: %s\n" +
                                                        "- Location: %s\n\n" +
                                                        "See you there!\n" +
                                                        "EventPro Team",
                                        customerName, eventTitle, eventDate, location);

                        helper.setText(emailContent);

                        jakarta.mail.util.ByteArrayDataSource dataSource = new jakarta.mail.util.ByteArrayDataSource(
                                        pdfBytes, "application/pdf");
                        helper.addAttachment("Ticket-ORD-" + bookingId + ".pdf", dataSource);

                        mailSender.send(mimeMessage);

                        if (mobile != null && !mobile.trim().isEmpty()) {
                                try {
                                        whatsAppService.sendBookingConfirmationWithPDF(mobile, customerName, eventTitle,
                                                        bookingId, pdfBytes);
                                } catch (Exception we) {
                                        logger.error("WhatsApp delivery failed: {}", we.getMessage());
                                }
                        }
                        logToFile("Confirmation email sent successfully to " + to);

                } catch (Exception e) {
                        logToFile("CRITICAL ERROR: " + e.getMessage());
                        logger.error("CRITICAL: Failed to send ticket email to {}: {}", to, e.getMessage());
                        e.printStackTrace();
                }
        }

        @Override
        public void sendEventUpdateWithTicket(String to, String customerName, String eventTitle, String eventDate,
                        String location, double amount, int bookingId, String organization, String userType,
                        String mobile, String startTime, String endTime, String changeType) {
                logToFile("sendEventUpdateWithTicket START for: " + to + " (Type: " + changeType + ")");
                try {
                        byte[] pdfBytes = generateTicketPdfBytes(customerName, eventTitle, eventDate, location, amount,
                                        bookingId, organization, userType, startTime, endTime);

                        jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
                        org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                                        mimeMessage, true, "UTF-8");

                        helper.setFrom(fromEmail);
                        helper.setTo(to);
                        helper.setSubject("IMPORTANT UPDATE: " + eventTitle);

                        String emailContent = String.format(
                                        "Hello %s,\n\n" +
                                                        "This is an important update regarding the event '%s'.\n\n" +
                                                        "Update Details: %s\n\n" +
                                                        "We have attached your updated official ticket with the new details. Please use this updated version for entry.\n\n"
                                                        +
                                                        "Current Event Details:\n" +
                                                        "- New Date: %s\n" +
                                                        "- New Location: %s\n\n" +
                                                        "See you there!\n" +
                                                        "EventPro Team",
                                        customerName, eventTitle, changeType, eventDate, location);

                        helper.setText(emailContent);

                        jakarta.mail.util.ByteArrayDataSource dataSource = new jakarta.mail.util.ByteArrayDataSource(
                                        pdfBytes, "application/pdf");
                        helper.addAttachment("Updated-Ticket-ORD-" + bookingId + ".pdf", dataSource);

                        mailSender.send(mimeMessage);

                        if (mobile != null && !mobile.trim().isEmpty()) {
                                try {
                                        whatsAppService.sendEventUpdateWithPDF(mobile, customerName,
                                                        eventTitle, bookingId, pdfBytes, changeType);
                                } catch (Exception we) {
                                        logger.error("WhatsApp update failed: {}", we.getMessage());
                                }
                        }
                        logToFile("Update email sent successfully to " + to);

                } catch (Exception e) {
                        logToFile("UPDATE ERROR: " + e.getMessage());
                        logger.error("Failed to send update email to {}: {}", to, e.getMessage());
                }
        }

        @Override
        public void sendPendingCashBookingNotification(String to, String customerName, String eventTitle, double amount,
                        String deadline) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject("Action Required: Complete your booking for " + eventTitle);

                String content = String.format(
                                "Hello %s,\n\n" +
                                                "Your booking for the event '%s' is pending cash payment.\n\n" +
                                                "Please visit our office to pay the amount: ₹%.2f\n" +
                                                "Payment Deadline: %s (within 48 hours)\n\n" +
                                                "If payment is not received by the deadline, your booking will be automatically cancelled.\n\n"
                                                +
                                                "Best regards,\n" +
                                                "The EventPro Team",
                                customerName, eventTitle, amount, deadline);

                message.setText(content);
                sendMail(message);
        }

        @Override
        public void sendBookingCancellationNotification(String to, String customerName, String eventTitle,
                        String reason, String paymentMode, double amount) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject("Booking Cancelled: " + eventTitle);

                StringBuilder contentBuilder = new StringBuilder();
                contentBuilder.append(String.format("Hello %s,\n\n", customerName));
                contentBuilder.append(
                                String.format("Your booking for the event '%s' has been cancelled.\n\n", eventTitle));
                contentBuilder.append(
                                String.format("Reason: %s\n", (reason != null ? reason : "Administrative decision")));

                if (amount > 0) {
                        contentBuilder.append("\nREFUND INFORMATION:\n");
                        contentBuilder.append(String.format("Amount: ₹%.2f\n", amount));

                        if ("Cash".equalsIgnoreCase(paymentMode)) {
                                contentBuilder.append(
                                                "Please visit our office to collect your refund. Bring a valid ID for verification.\n");
                        } else {
                                contentBuilder.append(
                                                "Your refund will be processed to your original payment method within 5-7 business days.\n");
                        }
                }

                contentBuilder.append("\nIf you have any questions, please contact our support team.\n\n");
                contentBuilder.append("Best regards,\n");
                contentBuilder.append("The EventPro Team");

                message.setText(contentBuilder.toString());
                sendMail(message);
        }

        @Override
        public void sendBookingExpirationNotification(String to, String customerName, String eventTitle) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject("Booking Expired: " + eventTitle);

                String content = String.format(
                                "Hello %s,\n\n" +
                                                "Your pending cash booking for the event '%s' has expired as the payment was not received within the 48-hour window.\n\n"
                                                +
                                                "The reserved seats have been released. If you are still interested, please place a new booking.\n\n"
                                                +
                                                "Best regards,\n" +
                                                "The EventPro Team",
                                customerName, eventTitle);

                message.setText(content);
                sendMail(message);
        }

        private void sendMail(SimpleMailMessage message) {
                try {
                        mailSender.send(message);
                        if (message.getTo() != null && message.getTo().length > 0) {
                                logger.info("Email notification sent successfully to {}", message.getTo()[0]);
                        }
                } catch (Exception e) {
                        String recipient = "unknown";
                        if (message.getTo() != null && message.getTo().length > 0) {
                                recipient = message.getTo()[0];
                        }
                        logger.error("Failed to send email to {}: {}", recipient, e.getMessage());
                }
        }

        private byte[] generateQRCodeImage(String text, int width, int height) throws Exception {
                com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
                com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(text,
                                com.google.zxing.BarcodeFormat.QR_CODE, width, height);

                java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
                com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                return pngOutputStream.toByteArray();
        }

        @Override
        public void sendContactMessage(String name, String email, String subject, String messageContent) {
                try {
                        jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
                        org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                                        mimeMessage, true, "UTF-8");

                        helper.setFrom(fromEmail);
                        helper.setTo("deepanshshakya669@gmail.com");
                        helper.setSubject("New Contact Inquiry: " + subject);

                        String htmlContent = String.format(
                                        "<html><body style='font-family: sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto;'>"
                                                        +
                                                        "<div style='background: #4f46e5; padding: 20px; color: white; border-radius: 12px 12px 0 0;'>"
                                                        +
                                                        "<h2 style='margin: 0;'>New Contact Inquiry</h2>" +
                                                        "</div>" +
                                                        "<div style='padding: 20px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 12px 12px;'>"
                                                        +
                                                        "<p><strong>From:</strong> %s (%s)</p>" +
                                                        "<p><strong>Subject:</strong> %s</p>" +
                                                        "<hr style='border: 0; border-top: 1px solid #e5e7eb; margin: 20px 0;'>"
                                                        +
                                                        "<div style='background: #f9fafb; padding: 15px; border-radius: 8px;'>%s</div>"
                                                        +
                                                        "</div>" +
                                                        "<p style='font-size: 12px; color: #6b7280; text-align: center; margin-top: 20px;'>Sent via EventPro Contact Form</p>"
                                                        +
                                                        "</body></html>",
                                        name, email, subject, messageContent.replace("\n", "<br>"));

                        helper.setText(htmlContent, true);
                        mailSender.send(mimeMessage);
                        logger.info("Professional contact email sent successfully from {}", email);
                } catch (Exception e) {
                        logger.error("Failed to send HTML contact email: {}", e.getMessage());
                        // Fallback to simple mail if HTML fails
                        SimpleMailMessage simple = new SimpleMailMessage();
                        simple.setFrom(fromEmail);
                        simple.setTo("deepanshshakya669@gmail.com");
                        simple.setSubject("Contact Inquiry (Plain): " + subject);
                        simple.setText(String.format("From: %s (%s)\nSubject: %s\n\n%s", name, email, subject,
                                        messageContent));
                        mailSender.send(simple);
                }
        }

        @Override
        public void sendSupportTicket(String issueType, String subject, String details, String fromEmail) {
                try {
                        jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
                        org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                                        mimeMessage, true, "UTF-8");

                        helper.setFrom(this.fromEmail);
                        helper.setTo("deepanshshakya669@gmail.com");
                        helper.setSubject("SUPPORT TICKET [" + issueType.toUpperCase() + "]: " + subject);

                        String htmlContent = String.format(
                                        "<html><body style='font-family: sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto;'>"
                                                        +
                                                        "<div style='background: #ef4444; padding: 20px; color: white; border-radius: 12px 12px 0 0;'>"
                                                        +
                                                        "<h2 style='margin: 0;'>New Support Ticket</h2>" +
                                                        "<p style='margin: 5px 0 0 0; opacity: 0.9;'>Issue Type: %s</p>"
                                                        +
                                                        "</div>" +
                                                        "<div style='padding: 20px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 12px 12px;'>"
                                                        +
                                                        "<p><strong>From:</strong> %s</p>" +
                                                        "<p><strong>Subject:</strong> %s</p>" +
                                                        "<hr style='border: 0; border-top: 1px solid #e5e7eb; margin: 20px 0;'>"
                                                        +
                                                        "<div style='background: #fff1f2; padding: 15px; border-radius: 8px; border-left: 4px solid #ef4444;'>%s</div>"
                                                        +
                                                        "</div>" +
                                                        "<p style='font-size: 12px; color: #6b7280; text-align: center; margin-top: 20px;'>Urgent priority handling requested.</p>"
                                                        +
                                                        "</body></html>",
                                        issueType, fromEmail, subject, details.replace("\n", "<br>"));

                        helper.setText(htmlContent, true);
                        mailSender.send(mimeMessage);
                        logger.info("Professional support ticket sent successfully from {}", fromEmail);
                } catch (Exception e) {
                        logger.error("Failed to send HTML support ticket: {}", e.getMessage());
                        SimpleMailMessage simple = new SimpleMailMessage();
                        simple.setFrom(this.fromEmail);
                        simple.setTo("deepanshshakya669@gmail.com");
                        simple.setSubject("Support Ticket [" + issueType + "] (Plain): " + subject);
                        simple.setText(String.format("Type: %s\nFrom: %s\nSubject: %s\n\n%s", issueType, fromEmail,
                                        subject, details));
                        mailSender.send(simple);
                }
        }
}
