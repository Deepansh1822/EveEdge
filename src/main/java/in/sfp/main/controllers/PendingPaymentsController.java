package in.sfp.main.controllers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import in.sfp.main.model.Bookings;
import in.sfp.main.repo.BookingsRepository;

@Controller
@RequestMapping("/admin")
public class PendingPaymentsController {

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private in.sfp.main.service.EmailService emailService;

    @Autowired
    private in.sfp.main.service.WhatsAppService whatsAppService;

    /**
     * Show pending cash payments page
     */
    @GetMapping("/pending-payments")
    public String showPendingPayments(Model model) {
        List<Bookings> pendingPayments = bookingsRepository.findByStatusAndPaymentMode("Pending", "Cash");
        model.addAttribute("pendingPayments", pendingPayments);
        return "pending-payments";
    }

    /**
     * Confirm cash payment (Admin action)
     */
    @PostMapping("/api/confirm-payment/{bookingId}")
    @ResponseBody
    public ResponseEntity<?> confirmPayment(
            @PathVariable Integer bookingId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false, defaultValue = "Admin") String adminName) {

        Optional<Bookings> bookingOpt = bookingsRepository.findById(bookingId);

        if (!bookingOpt.isPresent()) {
            return ResponseEntity.badRequest().body(createResponse(false, "Booking not found"));
        }

        Bookings booking = bookingOpt.get();

        // Verify it's a cash payment
        if (!"Cash".equalsIgnoreCase(booking.getPaymentMode())) {
            return ResponseEntity.badRequest().body(createResponse(false, "Not a cash payment"));
        }

        // Verify it's pending
        if (!"Pending".equalsIgnoreCase(booking.getStatus())) {
            return ResponseEntity.badRequest().body(createResponse(false, "Booking is not pending"));
        }

        // Update booking status
        booking.setStatus("Confirmed");
        booking.setVerifiedBy(adminName);
        booking.setVerifiedAt(LocalDateTime.now());
        if (notes != null && !notes.trim().isEmpty()) {
            booking.setPaymentNotes(notes);
        }

        bookingsRepository.save(booking);

        // Send confirmation and ticket
        emailService.sendBookingConfirmationWithTicket(
                booking.getCustomerEmail(),
                booking.getCustomerName(),
                booking.getEvent().getTitle(),
                booking.getEvent().getEventDate() != null ? booking.getEvent().getEventDate().toString() : "TBD",
                booking.getEvent().getLocation(),
                booking.getTotalAmount(),
                booking.getBookingId(),
                booking.getOrganization(),
                booking.getUserType(),
                booking.getMobileNumber(),
                booking.getEvent().getStartTime(),
                booking.getEvent().getEndTime());

        whatsAppService.sendBookingConfirmationMessage(
                booking.getMobileNumber(),
                booking.getCustomerName(),
                booking.getEvent().getTitle(),
                booking.getBookingId());

        return ResponseEntity.ok(createResponse(true, "Payment confirmed successfully"));
    }

    /**
     * Cancel booking (Admin action)
     */
    @PostMapping("/api/cancel-booking/{bookingId}")
    @ResponseBody
    public ResponseEntity<?> cancelBooking(
            @PathVariable Integer bookingId,
            @RequestParam(required = false) String reason) {

        Optional<Bookings> bookingOpt = bookingsRepository.findById(bookingId);

        if (!bookingOpt.isPresent()) {
            return ResponseEntity.badRequest().body(createResponse(false, "Booking not found"));
        }

        Bookings booking = bookingOpt.get();

        // Update booking status
        booking.setStatus("Cancelled");
        if (reason != null && !reason.trim().isEmpty()) {
            booking.setPaymentNotes("Cancelled: " + reason);
        }

        bookingsRepository.save(booking);

        // Send cancellation notification
        emailService.sendBookingCancellationNotification(
                booking.getCustomerEmail(),
                booking.getCustomerName(),
                booking.getEvent().getTitle(),
                reason,
                booking.getPaymentMode(),
                booking.getTotalAmount());

        whatsAppService.sendBookingCancellationMessage(
                booking.getMobileNumber(),
                booking.getCustomerName(),
                booking.getEvent().getTitle(),
                reason,
                booking.getPaymentMode(),
                booking.getTotalAmount());

        return ResponseEntity.ok(createResponse(true, "Booking cancelled successfully"));
    }

    /**
     * Get pending payments count (for dashboard badge)
     */
    @GetMapping("/api/pending-payments/count")
    @ResponseBody
    public ResponseEntity<?> getPendingPaymentsCount() {
        List<Bookings> pendingPayments = bookingsRepository.findByStatusAndPaymentMode("Pending", "Cash");
        return ResponseEntity.ok(Map.of("count", pendingPayments.size()));
    }

    private Map<String, Object> createResponse(boolean success, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        return response;
    }
}
