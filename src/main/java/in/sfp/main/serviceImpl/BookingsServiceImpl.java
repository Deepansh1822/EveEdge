package in.sfp.main.serviceImpl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.sfp.main.model.Bookings;
import in.sfp.main.repo.BookingsRepository;
import in.sfp.main.service.BookingsService;

@Service
public class BookingsServiceImpl implements BookingsService {

    @Autowired
    private BookingsRepository bookingsRepo;

    @Autowired
    private in.sfp.main.repo.EventsRepository eventsRepo;

    @Autowired
    private in.sfp.main.service.EmailService emailService;

    @Autowired
    private in.sfp.main.service.WhatsAppService whatsAppService;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private void logToFile(String message) {
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("debug_log.txt"),
                    (new java.util.Date() + ": " + message + "\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Bookings addBooking(Bookings booking) {
        logToFile("addBooking called for: " + booking.getCustomerEmail());
        if (booking.getEvent() != null) {
            in.sfp.main.model.Events event = eventsRepo.findById(booking.getEvent().getEventId()).orElse(null);
            if (event != null) {
                System.out.println("DEBUG: BookingsServiceImpl - Processing event: " + event.getTitle());
                int currentAvailable = (event.getAvailableSeats() != null) ? event.getAvailableSeats() : 0;
                int requestedQty = (booking.getQuantity() != null) ? booking.getQuantity() : 0;

                if (currentAvailable >= requestedQty && requestedQty > 0) {
                    event.setAvailableSeats(currentAvailable - requestedQty);
                    eventsRepo.save(event);
                    Bookings savedBooking = bookingsRepo.save(booking);
                    System.out.println(
                            "DEBUG: BookingsServiceImpl - Booking saved with ID: " + savedBooking.getBookingId());

                    String eventDateStr = event.getEventDate() != null ? event.getEventDate().toString() : "TBD";

                    if ("Cash".equalsIgnoreCase(savedBooking.getPaymentMode())) {
                        // For Cash: Send Pending Notification
                        String deadlineStr = savedBooking.getPaymentDeadline() != null
                                ? savedBooking.getPaymentDeadline()
                                        .format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                                : "within 48 hours";

                        emailService.sendPendingCashBookingNotification(
                                savedBooking.getCustomerEmail(),
                                savedBooking.getCustomerName(),
                                event.getTitle(),
                                savedBooking.getTotalAmount(),
                                deadlineStr);

                        whatsAppService.sendPendingCashBookingMessage(
                                savedBooking.getMobileNumber(),
                                savedBooking.getCustomerName(),
                                event.getTitle(),
                                savedBooking.getTotalAmount(),
                                deadlineStr);
                    } else {
                        // For Online: Send Ticket Notification
                        logToFile("Executing Online Payment path for " + savedBooking.getCustomerEmail());
                        System.out.println("DEBUG: BookingsServiceImpl - Calling sendBookingConfirmationWithTicket for "
                                + savedBooking.getCustomerEmail());
                        emailService.sendBookingConfirmationWithTicket(
                                savedBooking.getCustomerEmail(),
                                savedBooking.getCustomerName(),
                                event.getTitle(),
                                eventDateStr,
                                event.getLocation(),
                                savedBooking.getTotalAmount(),
                                savedBooking.getBookingId(),
                                savedBooking.getOrganization(),
                                savedBooking.getUserType(),
                                savedBooking.getMobileNumber(),
                                event.getStartTime(),
                                event.getEndTime());
                    }

                    return savedBooking;
                } else if (requestedQty <= 0) {
                    throw new RuntimeException("Invalid ticket quantity!");
                } else {
                    throw new RuntimeException("Not enough seats available! Only " + currentAvailable + " left.");
                }
            } else {
                System.err.println("DEBUG: BookingsServiceImpl - Event with ID " + booking.getEvent().getEventId()
                        + " not found!");
            }
        } else {
            System.err.println("DEBUG: BookingsServiceImpl - Booking has no associated event!");
        }
        return bookingsRepo.save(booking);
    }

    @Override
    public boolean updateBookingStatus(int id, String status) {
        Bookings booking = bookingsRepo.findById(id).orElse(null);
        if (booking != null) {
            booking.setStatus(status);
            bookingsRepo.save(booking);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteBooking(int id) {
        if (bookingsRepo.existsById(id)) {
            bookingsRepo.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<Bookings> getAllBookings() {
        return bookingsRepo.findAll();
    }

    @Override
    public Optional<Bookings> getBookingById(int id) {
        return bookingsRepo.findById(id);
    }

    @Override
    public List<Bookings> getBookingsByEventId(int eventId) {
        return bookingsRepo.findAll().stream()
                .filter(b -> b.getEvent().getEventId() == eventId)
                .collect(Collectors.toList());
    }

    @Override
    public int getTotalBookings() {
        return (int) bookingsRepo.count();
    }

    @Override
    public List<Bookings> getBookingsByEmail(String email) {
        return bookingsRepo.findByCustomerEmailOrderByBookingDateDesc(email);
    }

    @Override
    public boolean shareTicket(int bookingId, String recipientName, String recipientEmail, String recipientMobile) {
        try {
            Optional<Bookings> bookingOptional = bookingsRepo.findById(bookingId);
            if (bookingOptional.isPresent()) {
                Bookings booking = bookingOptional.get();
                in.sfp.main.model.Events event = booking.getEvent();

                if (event != null) {
                    boolean sentAny = false;
                    // Send via Email if provided
                    if (recipientEmail != null && !recipientEmail.trim().isEmpty()) {
                        emailService.sendBookingConfirmationWithTicket(
                                recipientEmail,
                                recipientName,
                                event.getTitle(),
                                event.getEventDate() != null ? event.getEventDate().toString() : "TBD",
                                event.getLocation(),
                                booking.getTotalAmount(),
                                booking.getBookingId(),
                                booking.getOrganization(),
                                booking.getUserType(),
                                recipientMobile != null && !recipientMobile.trim().isEmpty() ? recipientMobile
                                        : booking.getMobileNumber(),
                                event.getStartTime(),
                                event.getEndTime());
                        sentAny = true;
                    }

                    // However, if only mobile is provided and no email, we should call WhatsApp
                    // directly here with PDF
                    if (!sentAny && recipientMobile != null && !recipientMobile.trim().isEmpty()) {
                        byte[] pdfBytes = emailService.generateTicketPdfBytes(
                                recipientName,
                                event.getTitle(),
                                event.getEventDate() != null ? event.getEventDate().toString() : "TBD",
                                event.getLocation(),
                                booking.getTotalAmount(),
                                booking.getBookingId(),
                                booking.getOrganization(),
                                booking.getUserType(),
                                event.getStartTime(),
                                event.getEndTime());

                        whatsAppService.sendBookingConfirmationWithPDF(recipientMobile, recipientName,
                                event.getTitle(), booking.getBookingId(), pdfBytes);
                        sentAny = true;
                    }

                    return sentAny;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean userCancelBooking(int id) {
        Optional<Bookings> bookingOpt = bookingsRepo.findById(id);
        if (bookingOpt.isPresent()) {
            Bookings booking = bookingOpt.get();

            // Safety check: Don't cancel if already cancelled or expired
            if ("Cancelled".equalsIgnoreCase(booking.getStatus()) || "Expired".equalsIgnoreCase(booking.getStatus())) {
                throw new RuntimeException("Booking is already " + booking.getStatus());
            }

            in.sfp.main.model.Events event = booking.getEvent();

            // Check if cancellation is allowed (before event date)
            if (event != null && event.getEventDate() != null) {
                try {
                    java.time.LocalDate eventDate = java.time.LocalDate.parse(event.getEventDate());
                    if (!java.time.LocalDate.now().isBefore(eventDate)) {
                        throw new RuntimeException("Cancellation is only allowed before the day of the event!");
                    }
                } catch (Exception e) {
                    if (e instanceof RuntimeException)
                        throw (RuntimeException) e;
                }
            }

            // Update status
            booking.setStatus("Cancelled");
            booking.setPaymentNotes("Cancelled by user on " + java.time.LocalDateTime.now());
            bookingsRepo.save(booking);

            // Restore seats
            if (event != null) {
                int currentAvailable = (event.getAvailableSeats() != null) ? event.getAvailableSeats() : 0;
                event.setAvailableSeats(currentAvailable + (booking.getQuantity() != null ? booking.getQuantity() : 0));
                eventsRepo.save(event);
            }

            // Trigger Notifications
            String reason = "Ticket cancelled by your request via 'My Tickets'.";
            emailService.sendBookingCancellationNotification(
                    booking.getCustomerEmail(),
                    booking.getCustomerName(),
                    event != null ? event.getTitle() : "Event",
                    reason,
                    booking.getPaymentMode(),
                    booking.getTotalAmount());

            whatsAppService.sendBookingCancellationMessage(
                    booking.getMobileNumber(),
                    booking.getCustomerName(),
                    event != null ? event.getTitle() : "Event",
                    reason,
                    booking.getPaymentMode(),
                    booking.getTotalAmount());

            // Refund Logic
            System.out.println("=== REFUND LOGIC START ===");
            System.out.println("Booking ID: " + id);
            System.out.println("Total Amount: " + booking.getTotalAmount());
            System.out.println("Payment Mode: " + booking.getPaymentMode());
            System.out.println("Transaction ID: " + booking.getTransactionId());

            if (booking.getTotalAmount() > 0) {
                if ("Cash".equalsIgnoreCase(booking.getPaymentMode())) {
                    System.out.println("LOG: Cash payment - Customer should collect refund from office.");
                    booking.setPaymentNotes(booking.getPaymentNotes() + " | Refund: Physical collection required.");
                } else if (booking.getTransactionId() != null && !booking.getTransactionId().isEmpty()) {
                    // Actual Razorpay Refund Logic
                    System.out
                            .println("LOG: Attempting Razorpay refund for transaction: " + booking.getTransactionId());
                    try {
                        com.razorpay.RazorpayClient razorpay = new com.razorpay.RazorpayClient(razorpayKeyId,
                                razorpayKeySecret);
                        System.out.println("LOG: Razorpay client initialized successfully");

                        com.razorpay.Refund refund = razorpay.payments.refund(booking.getTransactionId());
                        String refundId = refund.get("id");

                        System.out.println("✓ SUCCESS: Razorpay Refund Successful for booking #" + id);
                        System.out.println("✓ Refund ID: " + refundId);
                        System.out.println("✓ Amount: ₹" + booking.getTotalAmount());

                        booking.setPaymentNotes(
                                booking.getPaymentNotes() + " | Razorpay Refund Initiated: " + refundId);
                    } catch (com.razorpay.RazorpayException e) {
                        System.err.println("✗ ERROR: Razorpay Refund failed!");
                        System.err.println("✗ Error Message: " + e.getMessage());
                        System.err.println("✗ Error Code: " + e.getClass().getSimpleName());
                        e.printStackTrace();
                        booking.setPaymentNotes(booking.getPaymentNotes() + " | Refund Error: " + e.getMessage());
                    }
                } else {
                    System.out.println("WARNING: Digital payment but no transaction ID found!");
                    System.out.println("This booking cannot be refunded automatically.");
                    booking.setPaymentNotes(
                            booking.getPaymentNotes() + " | Refund: Manual processing required (No transaction ID)");
                }
                bookingsRepo.save(booking);
            } else {
                System.out.println("LOG: No refund needed (Amount: 0)");
            }
            System.out.println("=== REFUND LOGIC END ===");

            return true;
        }
        return false;
    }
}
