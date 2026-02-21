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
import org.springframework.web.bind.annotation.RestController;

import in.sfp.main.model.Events;
import in.sfp.main.service.EventsService;

@RestController
@RequestMapping("/api/events")
public class EventsController {

    @Autowired
    private EventsService eventsService;

    @GetMapping("/test")
    public String test() {
        return "API is working";
    }

    @PostMapping("/add")
    public ResponseEntity<Events> addEvents(@RequestBody Events events) {
        Events savedEvent = eventsService.addEvents(events);
        return ResponseEntity.ok(savedEvent);
    }

    @GetMapping("/getall")
    public List<Events> getAllEvents() {
        return eventsService.getEvents();
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateEvents(@PathVariable int id, @RequestBody Events events) {
        boolean isUpdated = eventsService.updateEvents(id, events);
        if (isUpdated) {
            return ResponseEntity.ok("Event updated successfully!");
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteEvents(@PathVariable int id) {
        boolean isDeleted = eventsService.deleteEvents(id);
        if (isDeleted) {
            return ResponseEntity.ok("Event deleted successfully!");
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<Events> getCategoriesById(@PathVariable int id) {
        Events eve = eventsService.getEventsById(id).orElse(null);

        if (eve != null) {
            return ResponseEntity.ok().body(eve);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/get/count")
    public int getTotalEvents() {
        return eventsService.getTotalEvents();
    }

    @GetMapping("/top")
    public List<Events> getTopEvents() {
        return eventsService.getTopEvents();
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<String> updateEventStatus(@PathVariable int id,
            @RequestBody in.sfp.main.dto.EventStatusUpdateDTO statusUpdate) {
        boolean isUpdated = eventsService.updateEventStatus(id, statusUpdate.getStatus(),
                statusUpdate.getStatusReason());
        if (isUpdated) {
            return ResponseEntity.ok("Event status updated successfully!");
        }
        return ResponseEntity.notFound().build();
    }
}
