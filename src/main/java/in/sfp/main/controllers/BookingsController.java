package in.sfp.main.controllers;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.sfp.main.model.Bookings;
import in.sfp.main.service.BookingsService;

@RestController
@RequestMapping("/api/bookings")
public class BookingsController {

    @Autowired
    private BookingsService bookingsService;

    @Autowired
    private in.sfp.main.service.EmailService emailService;

    @PostMapping("/add")
    public ResponseEntity<?> addBooking(@RequestBody Bookings booking) {
        try {
            Bookings savedBooking = bookingsService.addBooking(booking);
            return ResponseEntity.ok(savedBooking);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/getall")
    public List<Bookings> getAllBookings() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof in.sfp.main.security.UserDetailsImpl) {
            in.sfp.main.security.UserDetailsImpl userDetails = (in.sfp.main.security.UserDetailsImpl) auth
                    .getPrincipal();
            // Show only personal tickets for everyone on this page
            return bookingsService.getBookingsByEmail(userDetails.getEmail());
        }
        return new java.util.ArrayList<>();
    }

    @GetMapping("/admin/all")
    public List<Bookings> adminGetAllBookings() {
        // Restricted to admins via SecurityConfig if needed, but for now just a
        // separate endpoint
        return bookingsService.getAllBookings();
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<Bookings> getBookingById(@PathVariable int id) {
        return bookingsService.getBookingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/updateStatus/{id}")
    public ResponseEntity<String> updateStatus(@PathVariable int id, @RequestParam String status) {
        if (bookingsService.updateBookingStatus(id, status)) {
            return ResponseEntity.ok("Booking status updated to: " + status);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteBooking(@PathVariable int id) {
        if (bookingsService.deleteBooking(id)) {
            return ResponseEntity.ok("Booking deleted successfully!");
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/byEvent/{eventId}")
    public List<Bookings> getBookingsByEvent(@PathVariable int eventId) {
        return bookingsService.getBookingsByEventId(eventId);
    }

    // Controller for fetching total categories

    @GetMapping("/get/count")
    public ResponseEntity<?> getTotalBookings() {
        int count = bookingsService.getTotalBookings();
        return ResponseEntity.ok(java.util.Collections.singletonMap("count", count));
    }

    @PostMapping("/share")
    public ResponseEntity<String> shareTicket(@RequestBody java.util.Map<String, Object> request) {
        try {
            int bookingId = Integer.parseInt(request.get("bookingId").toString());
            String recipientName = request.get("recipientName").toString();
            String recipientEmail = request.containsKey("recipientEmail") ? request.get("recipientEmail").toString()
                    : "";
            String recipientMobile = request.containsKey("recipientMobile") ? request.get("recipientMobile").toString()
                    : "";

            boolean sent = bookingsService.shareTicket(bookingId, recipientName, recipientEmail, recipientMobile);
            if (sent) {
                return ResponseEntity.ok("Ticket shared successfully!");
            } else {
                return ResponseEntity.badRequest().body("Failed to share ticket");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/user-cancel/{id}")
    public ResponseEntity<String> userCancel(@PathVariable int id) {
        try {
            if (bookingsService.userCancelBooking(id)) {
                return ResponseEntity.ok("Ticket cancelled successfully!");
            }
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/validate/{id}")
    public org.springframework.web.servlet.ModelAndView validateTicket(@PathVariable int id) {
        org.springframework.web.servlet.ModelAndView mav = new org.springframework.web.servlet.ModelAndView(
                "validation-result");

        try {
            Bookings booking = bookingsService.getBookingById(id).orElse(null);

            if (booking == null) {
                mav.addObject("valid", false);
                mav.addObject("message", "Invalid Ticket: Booking ID not found in system.");
                return mav;
            }

            // Check if cancelled or pending
            if ("Cancelled".equalsIgnoreCase(booking.getStatus()) || "Pending".equalsIgnoreCase(booking.getStatus())) {
                mav.addObject("valid", false);
                mav.addObject("message", "Ticket is " + booking.getStatus() + " and cannot be used.");
                mav.addObject("booking", booking);
                return mav;
            }

            // Check if already checked in
            if (booking.isCheckedIn()) {
                mav.addObject("valid", false);
                mav.addObject("alreadyCheckedIn", true);
                mav.addObject("message", "This ticket was already scanned.");
                mav.addObject("booking", booking);
                return mav;
            }

            // Perform Check-in
            booking.setCheckedIn(true);
            booking.setCheckInAt(java.time.LocalDateTime.now());
            bookingsService.addBooking(booking); // Save changes

            mav.addObject("valid", true);
            mav.addObject("message", "Ticket successfully validated. Welcome, " + booking.getCustomerName() + "!");
            mav.addObject("booking", booking);
            return mav;

        } catch (Exception e) {
            mav.addObject("valid", false);
            mav.addObject("message", "System Error: " + e.getMessage());
            return mav;
        }
    }

    @GetMapping("/download-ticket/{id}")
    public ResponseEntity<?> downloadTicket(@PathVariable int id) {
        try {
            in.sfp.main.model.Bookings b = bookingsService.getBookingById(id).orElse(null);
            if (b == null) {
                return ResponseEntity.notFound().build();
            }

            in.sfp.main.model.Events event = b.getEvent();
            byte[] pdfBytes = emailService.generateTicketPdfBytes(
                    b.getCustomerName(),
                    event.getTitle(),
                    event.getEventDate(),
                    event.getLocation(),
                    b.getTotalAmount(),
                    b.getBookingId(),
                    b.getOrganization(),
                    b.getUserType(),
                    event.getStartTime(),
                    event.getEndTime());

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=Ticket-" + id + ".pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error generating ticket: " + e.getMessage());
        }
    }
}
