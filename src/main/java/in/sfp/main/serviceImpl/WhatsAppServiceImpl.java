package in.sfp.main.serviceImpl;

import in.sfp.main.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

        private static final Logger logger = LoggerFactory.getLogger(WhatsAppServiceImpl.class);

        @Autowired
        private RestTemplate restTemplate;

        @Value("${whatsapp.api.url}")
        private String apiUrl;

        @Value("${whatsapp.api.token}")
        private String apiToken;

        @Value("${whatsapp.phone.number.id}")
        private String phoneNumberId;

        @Value("${whatsapp.enabled}")
        private boolean whatsappEnabled;

        @Override
        public void sendPendingCashBookingMessage(String mobileNumber, String customerName, String eventTitle,
                        double amount, String deadline) {
                String message = String.format(
                                "*EventPro Booking Pending* \n\n" +
                                                "Hi %s, your booking for *%s* is pending cash payment. \n\n" +
                                                "Amount to pay: *₹%.2f* \n" +
                                                "Payment Deadline: *%s* \n\n" +
                                                "Please visit the office to complete your payment.",
                                customerName, eventTitle, amount, deadline);
                sendWhatsApp(mobileNumber, message);
        }

        @Override
        public void sendBookingConfirmationMessage(String mobileNumber, String customerName, String eventTitle,
                        int bookingId) {
                String message = String.format(
                                "*EventPro Booking Confirmed!* \n\n" +
                                                "Hi %s, we have received your payment for *%s*. \n\n" +
                                                "Booking ID: #ORD-%d \n" +
                                                "Your ticket has been sent to your registered email.",
                                customerName, eventTitle, bookingId);
                sendWhatsApp(mobileNumber, message);
        }

        @Override
        public void sendBookingConfirmationWithPDF(String mobileNumber, String customerName, String eventTitle,
                        int bookingId, byte[] pdfData) {
                // First send text confirmation
                sendBookingConfirmationMessage(mobileNumber, customerName, eventTitle, bookingId);

                // Then attempt to send PDF document
                if (whatsappEnabled && pdfData != null) {
                        try {
                                sendWhatsAppDocument(mobileNumber, pdfData, "Ticket-" + bookingId + ".pdf");
                        } catch (Exception e) {
                                logger.error("Failed to send WhatsApp PDF: " + e.getMessage());
                        }
                }
        }

        @Override
        public void sendBookingCancellationMessage(String mobileNumber, String customerName, String eventTitle,
                        String reason, String paymentMode, double amount) {
                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append("*EventPro Booking Cancelled*\n\n");
                messageBuilder.append(String.format("Hi %s, your booking for *%s* has been cancelled.\n", customerName,
                                eventTitle));
                messageBuilder.append(
                                String.format("Reason: %s\n", (reason != null ? reason : "Administrative reasons")));

                if (amount > 0) {
                        messageBuilder.append(String.format("\n💰 *REFUND: ₹%.2f*\n", amount));
                        if ("Cash".equalsIgnoreCase(paymentMode)) {
                                messageBuilder.append(
                                                "Please visit our office to collect your refund. Bring a valid ID.");
                        } else {
                                messageBuilder.append(
                                                "Will be processed to your original payment method within 5-7 business days.");
                        }
                }

                sendWhatsApp(mobileNumber, messageBuilder.toString());
        }

        @Override
        public void sendBookingExpirationMessage(String mobileNumber, String customerName, String eventTitle) {
                String message = String.format(
                                "*EventPro Booking Expired* \n\n" +
                                                "Hi %s, your pending cash booking for *%s* has expired due to non-payment within the 48-hour window.",
                                customerName, eventTitle);
                sendWhatsApp(mobileNumber, message);
        }

        @Override
        public void sendEventUpdateMessage(String mobileNumber, String customerName, String eventTitle,
                        String details) {
                String message = String.format(
                                "*EventPro: Important Update* \n\n" +
                                                "Hi %s, there is an update for the event *%s* you are attending. \n\n"
                                                +
                                                "Details: %s \n\n" +
                                                "Please check the app for more info.",
                                customerName, eventTitle, details);
                sendWhatsApp(mobileNumber, message);
        }

        @Override
        public void sendEventUpdateWithPDF(String mobileNumber, String customerName, String eventTitle, int bookingId,
                        byte[] pdfData, String changeType) {
                // First send the clear update message
                String message = String.format(
                                "*EventPro: Important Update* \n\n" +
                                                "Hi %s, there is an update for *%s*. \n" +
                                                "Update: *%s* \n\n" +
                                                "We have attached your *updated ticket* below. Please use this new version for entry.",
                                customerName, eventTitle, changeType);
                sendWhatsApp(mobileNumber, message);

                // Then send the updated PDF
                if (whatsappEnabled && pdfData != null) {
                        try {
                                sendWhatsAppDocument(mobileNumber, pdfData, "Updated-Ticket-" + bookingId + ".pdf");
                        } catch (Exception e) {
                                logger.error("Failed to send WhatsApp update PDF: " + e.getMessage());
                        }
                }
        }

        @Override
        public void sendEventCancellationMessage(String mobileNumber, String customerName, String eventTitle,
                        String reason) {
                String message = String.format(
                                "*EventPro: Event Cancelled* \n\n" +
                                                "Hi %s, we regret to inform you that *%s* has been cancelled. \n" +
                                                "Reason: %s \n\n" +
                                                "Refunds (if any) will be processed soon.",
                                customerName, eventTitle, reason != null ? reason : "Unexpected circumstances");
                sendWhatsApp(mobileNumber, message);
        }

        private void sendWhatsApp(String mobileNumber, String message) {
                String cleanMobile = formatMobileNumber(mobileNumber);

                if (!whatsappEnabled) {
                        logger.info("\n--- SIMULATED WHATSAPP MESSAGE ---\nTO: {}\nCONTENT:\n{}\n-----------------------------------",
                                        cleanMobile, message);
                        return;
                }

                try {
                        String url = String.format("%s/%s/messages", apiUrl, phoneNumberId);

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setBearerAuth(apiToken);

                        Map<String, Object> body = new HashMap<>();
                        body.put("messaging_product", "whatsapp");
                        body.put("recipient_type", "individual");
                        body.put("to", cleanMobile);
                        body.put("type", "text");

                        Map<String, String> text = new HashMap<>();
                        text.put("body", message);
                        body.put("text", text);

                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                        restTemplate.postForEntity(url, entity, String.class);
                        logger.info("WhatsApp message sent successfully to {}", cleanMobile);
                } catch (Exception e) {
                        logger.error("Error sending WhatsApp message to {}: {}", cleanMobile, e.getMessage());
                }
        }

        private void sendWhatsAppDocument(String mobileNumber, byte[] pdfData, String fileName) {
                String cleanMobile = formatMobileNumber(mobileNumber);

                try {
                        // 1. Upload Media to WhatsApp Meta API
                        String uploadUrl = String.format("%s/%s/media", apiUrl, phoneNumberId);

                        HttpHeaders uploadHeaders = new HttpHeaders();
                        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
                        uploadHeaders.setBearerAuth(apiToken);

                        MultiValueMap<String, Object> uploadBody = new LinkedMultiValueMap<>();
                        uploadBody.add("messaging_product", "whatsapp");

                        // Attach PDF data
                        HttpHeaders fileHeaders = new HttpHeaders();
                        fileHeaders.setContentType(MediaType.APPLICATION_PDF);
                        HttpEntity<byte[]> fileEntity = new HttpEntity<>(pdfData, fileHeaders);
                        // Link as a file with filename
                        uploadBody.add("file", fileEntity);

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(uploadBody,
                                        uploadHeaders);
                        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl, requestEntity, Map.class);

                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                                String mediaId = (String) response.getBody().get("id");

                                // 2. Send Message with Media ID
                                String sendUrl = String.format("%s/%s/messages", apiUrl, phoneNumberId);
                                HttpHeaders sendHeaders = new HttpHeaders();
                                sendHeaders.setContentType(MediaType.APPLICATION_JSON);
                                sendHeaders.setBearerAuth(apiToken);

                                Map<String, Object> sendBody = new HashMap<>();
                                sendBody.put("messaging_product", "whatsapp");
                                sendBody.put("to", cleanMobile);
                                sendBody.put("type", "document");

                                Map<String, String> document = new HashMap<>();
                                document.put("id", mediaId);
                                document.put("filename", fileName);
                                sendBody.put("document", document);

                                HttpEntity<Map<String, Object>> sendEntity = new HttpEntity<>(sendBody, sendHeaders);
                                restTemplate.postForEntity(sendUrl, sendEntity, String.class);
                                logger.info("WhatsApp PDF ticket sent successfully to {}", cleanMobile);
                        }
                } catch (Exception e) {
                        logger.error("Error sending WhatsApp document to {}: {}", cleanMobile, e.getMessage());
                }
        }

        private String formatMobileNumber(String mobileNumber) {
                if (mobileNumber == null)
                        return "";
                // Meta API expects country code without '+' or '00'
                String clean = mobileNumber.replaceAll("\\D", "");
                // Assuming Indian numbers if length is 10 and doesn't start with 91
                if (clean.length() == 10) {
                        return "91" + clean;
                }
                return clean;
        }
}
