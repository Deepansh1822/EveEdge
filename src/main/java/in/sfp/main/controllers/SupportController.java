package in.sfp.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.sfp.main.service.EmailService;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SupportController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/contact")
    public ResponseEntity<?> sendContactMessage(@RequestBody ContactRequest request) {
        try {
            emailService.sendContactMessage(
                    request.getName(),
                    request.getEmail(),
                    request.getSubject(),
                    request.getMessage());
            return ResponseEntity.ok(Map.of("message", "Message sent successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to send message: " + e.getMessage()));
        }
    }

    @PostMapping("/support")
    public ResponseEntity<?> sendSupportTicket(@RequestBody SupportRequest request) {
        try {
            // Use authenticated user info if available, but for now trust the form data or
            // session
            // Assuming the frontend sends email or we extract it from context if needed.
            // For now, simpler to take from request as it might be a guest support request
            // or logged in.

            // If email is not provided in request, maybe use "Anonymous" or handle logic.
            // The support form in support.html doesn't explicitly ask for email if logged
            // in,
            // but the modal in support.html shows fields: Issue Type, Subject, Details.
            // It might be better to inject the current user's email if logged in.
            // For simplicity, let's assume the frontend will send the email or we'll
            // default to "System User".

            String fromEmail = request.getEmail() != null ? request.getEmail() : "User (from session)";

            emailService.sendSupportTicket(
                    request.getIssueType(),
                    request.getSubject(),
                    request.getDetails(),
                    fromEmail);
            return ResponseEntity.ok(Map.of("message", "Ticket submitted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to submit ticket: " + e.getMessage()));
        }
    }

    // DTOs
    public static class ContactRequest {
        private String name;
        private String email;
        private String subject;
        private String message;

        // getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class SupportRequest {
        private String issueType;
        private String subject;
        private String details;
        private String email; // Optional, might come from session

        public String getIssueType() {
            return issueType;
        }

        public void setIssueType(String issueType) {
            this.issueType = issueType;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
