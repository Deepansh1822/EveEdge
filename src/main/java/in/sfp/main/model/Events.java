package in.sfp.main.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "Events")
public class Events {

	@Column
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int eventId;

	@Column
	private String title;

	@ManyToOne
	@JoinColumn(name = "categoryId", referencedColumnName = "categId")
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "events", "category" })
	private EventCategories category;

	@Column
	private String description;

	@Column
	private String location;

	@Column(columnDefinition = "LONGBLOB")
	private byte[] banner_image;

	@Column
	private String maxSeats;

	@Column
	private Integer availableSeats;

	@Column
	private String price;

	@Column
	private String status = "Scheduled"; // Lifecycle: Scheduled, Cancelled, Rescheduled

	@Column
	private String createdBy;

	@Column
	private String statusReason;

	@Column
	private LocalDateTime createdAt;

	@Column
	private LocalDateTime updatedAt;

	@Column
	private String eventDate;

	@Column
	private Boolean isDeleted = false;

	@Column
	private Boolean paidEvent = true;

	@OneToMany(mappedBy = "event")
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "event", "bookings" })
	private List<Bookings> bookings;

	@jakarta.persistence.Transient
	private String bannerBase64;

	public String getBannerBase64() {
		if (banner_image != null && banner_image.length > 0) {
			return java.util.Base64.getEncoder().encodeToString(banner_image);
		}
		return null;
	}

	public void setBannerBase64(String bannerBase64) {
		this.bannerBase64 = bannerBase64;
	}

	@com.fasterxml.jackson.annotation.JsonProperty("displayStatus")
	public String getDisplayStatus() {
		String currentStatus = this.status != null ? this.status : "Scheduled";

		if ("Cancelled".equalsIgnoreCase(currentStatus)) {
			return "Cancelled";
		}

		if (this.eventDate == null || this.eventDate.isEmpty()) {
			return currentStatus;
		}

		try {
			LocalDate today = LocalDate.now();
			LocalDate eventDateObj = LocalDate.parse(this.eventDate);

			// If date is in past -> Passed
			if (eventDateObj.isBefore(today)) {
				return "Passed";
			}

			// If date is today, check time
			if (eventDateObj.isEqual(today)) {
				java.time.LocalTime now = java.time.LocalTime.now();

				// Case 1: Start and End time both exist
				if (this.startTime != null && !this.startTime.isEmpty() && this.endTime != null
						&& !this.endTime.isEmpty()) {
					try {
						java.time.LocalTime start = java.time.LocalTime.parse(this.startTime);
						java.time.LocalTime end = java.time.LocalTime.parse(this.endTime);

						if (now.isBefore(start)) {
							return "Upcoming";
						}
						if (now.isAfter(end)) {
							return "Passed";
						}
						return "Live";
					} catch (Exception e) {
					}
				}

				// Case 2: Only Start time exists (fallback to +2h logic)
				if (this.startTime != null && !this.startTime.isEmpty()) {
					try {
						java.time.LocalTime start = java.time.LocalTime.parse(this.startTime);

						if (now.isBefore(start)) {
							return "Upcoming";
						}
						// If start time + 2 hours is before now -> Passed
						if (start.plusHours(2).isBefore(now)) {
							return "Passed";
						}
						return "Live";
					} catch (Exception e) {
					}
				}
				return "Live"; // Default for today if no time set
			}

			return "Upcoming";
		} catch (Exception e) {
			return currentStatus;
		}
	}

	public Events() {
	}

	public int getEventId() {
		return eventId;
	}

	public void setEventId(int eventId) {
		this.eventId = eventId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public EventCategories getCategory() {
		return category;
	}

	public void setCategory(EventCategories category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public byte[] getBanner_image() {
		return banner_image;
	}

	public void setBanner_image(byte[] banner_image) {
		this.banner_image = banner_image;
	}

	public String getMaxSeats() {
		return maxSeats;
	}

	public void setMaxSeats(String maxSeats) {
		this.maxSeats = maxSeats;
	}

	public Integer getAvailableSeats() {
		return availableSeats;
	}

	public void setAvailableSeats(Integer availableSeats) {
		this.availableSeats = availableSeats;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getStatus() {
		if ("Upcoming".equalsIgnoreCase(status)) {
			return "Scheduled";
		}
		return status;
	}

	public void setStatus(String status) {
		if ("Upcoming".equalsIgnoreCase(status)) {
			this.status = "Scheduled";
		} else {
			this.status = status;
		}
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getStatusReason() {
		return statusReason;
	}

	public void setStatusReason(String statusReason) {
		this.statusReason = statusReason;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getEventDate() {
		return eventDate;
	}

	public void setEventDate(String eventDate) {
		this.eventDate = eventDate;
	}

	@Column
	private String startTime;

	@Column
	private String endTime;

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public List<Bookings> getBookings() {
		return bookings;
	}

	public void setBookings(List<Bookings> bookings) {
		this.bookings = bookings;
	}

	public Boolean isDeleted() {
		return isDeleted != null && isDeleted;
	}

	public void setDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public Boolean isPaidEvent() {
		return paidEvent != null && paidEvent;
	}

	public void setPaidEvent(Boolean paidEvent) {
		this.paidEvent = paidEvent;
	}

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		if (this.status == null) {
			this.status = "Scheduled";
		}
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
		if (!"Cancelled".equalsIgnoreCase(this.status)) {
			this.status = "Rescheduled";
		}
	}
}