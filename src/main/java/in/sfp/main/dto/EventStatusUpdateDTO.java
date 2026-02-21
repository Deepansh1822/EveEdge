package in.sfp.main.dto;

public class EventStatusUpdateDTO {
    private String status;
    private String statusReason;

    public EventStatusUpdateDTO() {
    }

    public EventStatusUpdateDTO(String status, String statusReason) {
        this.status = status;
        this.statusReason = statusReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }
}
