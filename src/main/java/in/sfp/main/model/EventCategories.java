package in.sfp.main.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "EventCategories")
public class EventCategories {

    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int categId;

    @Column
    private String catName;

    @Column(columnDefinition = "LONGBLOB")
    private byte[] catIcon;

    @Column
    private String catColor;

    @Column
    private String description;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "category")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "category", "events" })
    private List<Events> events;

    @jakarta.persistence.Transient
    private String catIconBase64;

    public String getCatIconBase64() {
        if (catIcon != null && catIcon.length > 0) {
            return java.util.Base64.getEncoder().encodeToString(catIcon);
        }
        return null;
    }

    public void setCatIconBase64(String catIconBase64) {
        this.catIconBase64 = catIconBase64;
    }

    public EventCategories() {
    }

    public int getCategId() {
        return categId;
    }

    public void setCategId(int categId) {
        this.categId = categId;
    }

    public String getCatName() {
        return catName;
    }

    public void setCatName(String catName) {
        this.catName = catName;
    }

    public byte[] getCatIcon() {
        return catIcon;
    }

    public void setCatIcon(byte[] catIcon) {
        this.catIcon = catIcon;
    }

    public String getCatColor() {
        return catColor;
    }

    public void setCatColor(String catColor) {
        this.catColor = catColor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<Events> getEvents() {
        return events;
    }

    public void setEvents(List<Events> events) {
        this.events = events;
    }

    public Boolean isDeleted() {
        return isDeleted != null && isDeleted;
    }

    public void setDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
