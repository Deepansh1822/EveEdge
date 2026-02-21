package in.sfp.main.service;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import in.sfp.main.model.Bookings;
import in.sfp.main.repo.BookingsRepository;

@Service
public class BookingExpirationService {

    private static final Logger logger = LoggerFactory.getLogger(BookingExpirationService.class);

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private in.sfp.main.service.WhatsAppService whatsAppService;

    /**
     * Auto-cancel expired pending cash bookings
     * Runs every 10 minutes
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void cancelExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();

        // Find all expired pending bookings
        List<Bookings> expiredBookings = bookingsRepository.findExpiredPendingBookings(now);

        if (expiredBookings.isEmpty()) {
            logger.debug("No expired bookings found");
            return;
        }

        int totalSeatsReleased = 0;

        for (Bookings booking : expiredBookings) {
            // Update status to Expired
            booking.setStatus("Expired");
            bookingsRepository.save(booking);

            totalSeatsReleased += booking.getQuantity();

            // Send notification
            emailService.sendBookingExpirationNotification(
                    booking.getCustomerEmail(),
                    booking.getCustomerName(),
                    booking.getEvent().getTitle());

            whatsAppService.sendBookingExpirationMessage(
                    booking.getMobileNumber(),
                    booking.getCustomerName(),
                    booking.getEvent().getTitle());

            logger.info("Booking #{} expired - {} seats released for event #{}",
                    booking.getBookingId(),
                    booking.getQuantity(),
                    booking.getEvent().getEventId());
        }

        logger.info("Auto-cancelled {} expired bookings, released {} seats",
                expiredBookings.size(), totalSeatsReleased);
    }
}
