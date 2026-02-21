package in.sfp.main.service;

public interface EmailService {
        void sendBookingConfirmation(String to, String customerName, String eventTitle, String eventDate,
                        String location,
                        double amount);

        void sendBookingConfirmationWithTicket(String to, String customerName, String eventTitle, String eventDate,
                        String location, double amount, int bookingId, String organization, String userType,
                        String mobile, String startTime, String endTime);

        void sendEventUpdateNotification(String to, String customerName, String eventTitle, String changeType,
                        String details);

        void sendEventUpdateWithTicket(String to, String customerName, String eventTitle, String eventDate,
                        String location, double amount, int bookingId, String organization, String userType,
                        String mobile, String startTime, String endTime, String changeType);

        void sendEventCancellationNotification(String to, String customerName, String eventTitle, String reason);

        void sendPendingCashBookingNotification(String to, String customerName, String eventTitle, double amount,
                        String deadline);

        void sendBookingCancellationNotification(String to, String customerName, String eventTitle, String reason,
                        String paymentMode, double amount);

        void sendBookingExpirationNotification(String to, String customerName, String eventTitle);

        byte[] generateTicketPdfBytes(String customerName, String eventTitle, String eventDate,
                        String location, double amount, int bookingId, String organization, String userType,
                        String startTime, String endTime) throws Exception;

        void sendContactMessage(String name, String email, String subject, String message);

        void sendSupportTicket(String issueType, String subject, String details, String fromEmail);
}
