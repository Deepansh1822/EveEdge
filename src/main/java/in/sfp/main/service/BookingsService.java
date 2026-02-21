package in.sfp.main.service;

import java.util.List;
import java.util.Optional;
import in.sfp.main.model.Bookings;

public interface BookingsService {

    public Bookings addBooking(Bookings booking);

    public boolean updateBookingStatus(int id, String status);

    // only admin should be able to delete booking
    public boolean deleteBooking(int id);

    // only admin should be able to get all bookings
    public List<Bookings> getAllBookings();

    // for user to get booking by id
    public Optional<Bookings> getBookingById(int id);

    // majorly for admin to get bookings by event id
    public List<Bookings> getBookingsByEventId(int eventId);

    public int getTotalBookings();

    public List<Bookings> getBookingsByEmail(String email);

    public boolean shareTicket(int bookingId, String recipientName, String recipientEmail, String recipientMobile);

    public boolean userCancelBooking(int id);
}
