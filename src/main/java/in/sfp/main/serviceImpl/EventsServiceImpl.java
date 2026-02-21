package in.sfp.main.serviceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.sfp.main.model.Events;
import in.sfp.main.repo.EventsRepository;
import in.sfp.main.service.EventsService;
import in.sfp.main.service.EmailService;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventsServiceImpl implements EventsService {

	@Autowired
	private EventsRepository eveRepo;

	@Autowired
	private EmailService emailService;

	@Autowired
	private in.sfp.main.service.WhatsAppService whatsAppService;

	@Override
	public Events addEvents(Events eve) {
		if (eve.getMaxSeats() != null && !eve.getMaxSeats().isEmpty()) {
			try {
				int seats = Integer.parseInt(eve.getMaxSeats());
				if (seats < 1)
					seats = 1;
				eve.setMaxSeats(String.valueOf(seats));
				eve.setAvailableSeats(seats);
			} catch (NumberFormatException e) {
				eve.setMaxSeats("1");
				eve.setAvailableSeats(1);
			}
		} else if (eve.getAvailableSeats() == null) {
			eve.setMaxSeats("1");
			eve.setAvailableSeats(1);
		}

		if (!eve.isPaidEvent()) {
			eve.setPrice("0");
		}

		return eveRepo.save(eve);
	}

	@Override
	public boolean updateEvents(int id, Events eve) {
		Events existingEvent = eveRepo.findById(id).orElse(null);
		if (existingEvent != null) {
			String oldDate = existingEvent.getEventDate();
			String oldLocation = existingEvent.getLocation();

			existingEvent.setTitle(eve.getTitle());
			if (eve.getCategory() != null) {
				existingEvent.setCategory(eve.getCategory());
			}
			existingEvent.setDescription(eve.getDescription());
			existingEvent.setLocation(eve.getLocation());

			if (eve.getBanner_image() != null && eve.getBanner_image().length > 0) {
				existingEvent.setBanner_image(eve.getBanner_image());
			}

			if (eve.getStatus() != null) {
				existingEvent.setStatus(eve.getStatus());
			}
			if (eve.getStatusReason() != null) {
				existingEvent.setStatusReason(eve.getStatusReason());
			}

			existingEvent.setCreatedBy(eve.getCreatedBy());
			existingEvent.setEventDate(eve.getEventDate());
			existingEvent.setPaidEvent(eve.isPaidEvent());

			if (!eve.isPaidEvent()) {
				existingEvent.setPrice("0");
			} else {
				existingEvent.setPrice(eve.getPrice());
			}

			if (eve.getMaxSeats() != null && !eve.getMaxSeats().isEmpty()) {
				try {
					int newMax = Integer.parseInt(eve.getMaxSeats());
					if (newMax < 1)
						newMax = 1;
					int oldMax = (existingEvent.getMaxSeats() != null) ? Integer.parseInt(existingEvent.getMaxSeats())
							: 0;
					if (existingEvent.getAvailableSeats() == null)
						existingEvent.setAvailableSeats(oldMax);
					if (newMax != oldMax) {
						int diff = newMax - oldMax;
						existingEvent.setAvailableSeats(Math.max(0, existingEvent.getAvailableSeats() + diff));
						existingEvent.setMaxSeats(String.valueOf(newMax));
					}
				} catch (NumberFormatException e) {
				}
			}

			eveRepo.save(existingEvent);

			// Notify Attendees
			if (existingEvent.getBookings() != null && !existingEvent.getBookings().isEmpty()) {
				String newStatus = existingEvent.getStatus();
				boolean isCancelled = "Cancelled".equalsIgnoreCase(newStatus);
				boolean isDateChanged = oldDate != null && !oldDate.equals(existingEvent.getEventDate());
				boolean isLocationChanged = oldLocation != null && !oldLocation.equals(existingEvent.getLocation());

				java.util.Set<String> notifiedEmails = new java.util.HashSet<>();

				for (in.sfp.main.model.Bookings booking : existingEvent.getBookings()) {
					String email = booking.getCustomerEmail();
					if (email == null || notifiedEmails.contains(email)) {
						continue;
					}
					notifiedEmails.add(email);

					if (isCancelled) {
						String reason = existingEvent.getStatusReason() != null ? existingEvent.getStatusReason()
								: "Administrative reasons";
						emailService.sendBookingCancellationNotification(
								email,
								booking.getCustomerName(),
								existingEvent.getTitle(),
								reason,
								booking.getPaymentMode(),
								booking.getTotalAmount());

						whatsAppService.sendEventCancellationMessage(
								booking.getMobileNumber(),
								booking.getCustomerName(),
								existingEvent.getTitle(),
								reason);
					} else if (isDateChanged || isLocationChanged) {
						String changeType = isDateChanged && isLocationChanged ? "Date and Location Updated"
								: isDateChanged ? "Event Rescheduled" : "Venue Changed";

						// Send updated ticket with new event details and update text
						emailService.sendEventUpdateWithTicket(
								email,
								booking.getCustomerName(),
								existingEvent.getTitle(),
								existingEvent.getEventDate(),
								existingEvent.getLocation(),
								booking.getTotalAmount(),
								booking.getBookingId(),
								booking.getOrganization(),
								booking.getUserType(),
								booking.getMobileNumber(),
								existingEvent.getStartTime(),
								existingEvent.getEndTime(),
								changeType);
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean deleteEvents(int id) {
		Events event = eveRepo.findById(id).orElse(null);
		if (event != null) {
			event.setDeleted(true);
			eveRepo.save(event);

			// Notify attendees about deletion (cancellation)
			if (event.getBookings() != null && !event.getBookings().isEmpty()) {
				String reason = "This event has been removed from our listings.";
				for (in.sfp.main.model.Bookings booking : event.getBookings()) {
					emailService.sendBookingCancellationNotification(
							booking.getCustomerEmail(),
							booking.getCustomerName(),
							event.getTitle(),
							reason,
							booking.getPaymentMode(),
							booking.getTotalAmount());

					whatsAppService.sendEventCancellationMessage(
							booking.getMobileNumber(),
							booking.getCustomerName(),
							event.getTitle(),
							reason);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public List<Events> getEvents() {
		List<Events> events = eveRepo.findAll().stream()
				.filter(e -> !e.isDeleted() && (e.getCategory() == null || !e.getCategory().isDeleted()))
				.collect(Collectors.toList());
		events.forEach(this::healAvailableSeats);
		return events;
	}

	private void healAvailableSeats(Events event) {
		if (event.getAvailableSeats() == null) {
			try {
				int max = (event.getMaxSeats() != null) ? Integer.parseInt(event.getMaxSeats()) : 0;
				int bookedCount = (event.getBookings() != null)
						? event.getBookings().stream().mapToInt(b -> b.getQuantity() != null ? b.getQuantity() : 0)
								.sum()
						: 0;
				event.setAvailableSeats(Math.max(0, max - bookedCount));
				eveRepo.save(event);
			} catch (Exception e) {
				event.setAvailableSeats(0);
			}
		}
	}

	@Override
	public Optional<Events> getEventsById(int id) {
		Optional<Events> event = eveRepo.findById(id);
		event.ifPresent(this::healAvailableSeats);
		return event;
	}

	@Override
	public int getTotalEvents() {
		return (int) eveRepo.findAll().stream()
				.filter(e -> !e.isDeleted() && (e.getCategory() == null || !e.getCategory().isDeleted()))
				.count();
	}

	@Override
	public List<Events> getTopEvents() {
		List<Events> allEvents = eveRepo.findAll().stream()
				.filter(e -> !e.isDeleted() && (e.getCategory() == null || !e.getCategory().isDeleted()))
				.collect(Collectors.toList());
		allEvents.forEach(this::healAvailableSeats);

		return allEvents.stream()
				.sorted((e1, e2) -> {
					int size1 = (e1.getBookings() != null) ? e1.getBookings().size() : 0;
					int size2 = (e2.getBookings() != null) ? e2.getBookings().size() : 0;
					if (size1 != size2) {
						return Integer.compare(size2, size1);
					}
					return Integer.compare(e2.getEventId(), e1.getEventId());
				})
				.limit(10)
				.collect(Collectors.toList());
	}

	@Override
	public boolean updateEventStatus(int id, String status, String statusReason) {
		Events existingEvent = eveRepo.findById(id).orElse(null);
		if (existingEvent != null) {
			existingEvent.setStatus(status);
			existingEvent.setStatusReason(statusReason);
			eveRepo.save(existingEvent);

			// Notify Attendees if status changed to Cancelled
			if ("Cancelled".equalsIgnoreCase(status) && existingEvent.getBookings() != null
					&& !existingEvent.getBookings().isEmpty()) {
				String reason = statusReason != null ? statusReason : "Administrative reasons";
				java.util.Set<String> notifiedEmails = new java.util.HashSet<>();

				for (in.sfp.main.model.Bookings booking : existingEvent.getBookings()) {
					String email = booking.getCustomerEmail();
					if (email == null || notifiedEmails.contains(email)) {
						continue;
					}
					notifiedEmails.add(email);

					emailService.sendBookingCancellationNotification(
							email,
							booking.getCustomerName(),
							existingEvent.getTitle(),
							reason,
							booking.getPaymentMode(),
							booking.getTotalAmount());

					whatsAppService.sendEventCancellationMessage(
							booking.getMobileNumber(),
							booking.getCustomerName(),
							existingEvent.getTitle(),
							reason);
				}
			}
			return true;
		}
		return false;
	}
}
