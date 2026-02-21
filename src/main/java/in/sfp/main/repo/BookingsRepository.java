package in.sfp.main.repo;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import in.sfp.main.model.Bookings;

@Repository
public interface BookingsRepository extends JpaRepository<Bookings, Integer> {

    // Find all pending cash payments
    List<Bookings> findByStatusAndPaymentMode(String status, String paymentMode);

    // Find expired pending bookings
    @Query("SELECT b FROM Bookings b WHERE b.status = 'Pending' AND b.paymentMode = 'Cash' AND b.paymentDeadline < :now")
    List<Bookings> findExpiredPendingBookings(@Param("now") LocalDateTime now);

    // Count booked seats for an event (Pending + Confirmed)
    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM Bookings b WHERE b.event.eventId = :eventId AND b.status IN ('Pending', 'Confirmed')")
    Integer countBookedSeatsByEventId(@Param("eventId") int eventId);

    // Find bookings by customer email
    List<Bookings> findByCustomerEmailOrderByBookingDateDesc(String email);
}
