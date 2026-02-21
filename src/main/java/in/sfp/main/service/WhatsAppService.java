package in.sfp.main.service;

public interface WhatsAppService {
        void sendPendingCashBookingMessage(String mobileNumber, String customerName, String eventTitle, double amount,
                        String deadline);

        void sendBookingConfirmationMessage(String mobileNumber, String customerName, String eventTitle, int bookingId);

        void sendBookingConfirmationWithPDF(String mobileNumber, String customerName, String eventTitle, int bookingId,
                        byte[] pdfData);

        void sendBookingCancellationMessage(String mobileNumber, String customerName, String eventTitle, String reason,
                        String paymentMode, double amount);

        void sendBookingExpirationMessage(String mobileNumber, String customerName, String eventTitle);

        void sendEventUpdateMessage(String mobileNumber, String customerName, String eventTitle, String details);

        void sendEventUpdateWithPDF(String mobileNumber, String customerName, String eventTitle, int bookingId,
                        byte[] pdfData, String changeType);

        void sendEventCancellationMessage(String mobileNumber, String customerName, String eventTitle, String reason);
}
