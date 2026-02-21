package in.sfp.main.controllers;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import in.sfp.main.model.EventCategories;
import in.sfp.main.model.Events;
import in.sfp.main.service.EventCategoriesService;
import in.sfp.main.service.EventsService;

@Controller
public class OpenPageController {

    @Autowired
    private EventCategoriesService eveCatService;

    @Autowired
    private EventsService eventsService;

    @GetMapping("/routing-test")
    @org.springframework.web.bind.annotation.ResponseBody
    public String testRouting() {
        return "Routing is working!";
    }

    @GetMapping({ "/openEventPage", "/", "/home", "/homepage" })
    public String openHomePage(Model model) {
        List<EventCategories> categories = eveCatService.getCategories();
        model.addAttribute("categories", categories);
        return "index";
    }

    @GetMapping("/tickets")
    public String openTicketsPage() {
        return "booking";
    }

    @GetMapping("/cart")
    public String openCartPage() {
        return "cart";
    }

    @GetMapping("/event-details")
    public String openEventDetailsPage() {
        return "event-details";
    }

    @GetMapping("/create-event")
    public String openCreateEventPage(Model model) {
        List<EventCategories> categories = eveCatService.getCategories();
        model.addAttribute("categories", categories);
        return "create-event";
    }

    @GetMapping("/create-category")
    public String openCreateCategoryPage() {
        return "create-category";
    }

    @GetMapping("/manage-categories")
    public String openManageCategoriesPage(Model model) {
        model.addAttribute("categories", eveCatService.getCategories());
        return "manage-categories";
    }

    @GetMapping("/manage-events")
    public String openManageEventsPage(Model model) {
        model.addAttribute("events", eventsService.getEvents());
        return "manage-events";
    }

    @GetMapping("/events")
    public String openAllEventsPage(Model model) {
        model.addAttribute("events", eventsService.getEvents());
        model.addAttribute("categories", eveCatService.getCategories());
        return "all-events";
    }

    @GetMapping("/categories")
    public String openAllCategoriesPage(Model model) {
        model.addAttribute("categories", eveCatService.getCategories());
        return "all-categories";
    }

    @GetMapping("/update-category/{id}")
    public String openUpdateCategoryPage(@PathVariable int id, Model model) {
        Optional<EventCategories> catOpt = eveCatService.getCategoriesById(id);
        if (catOpt.isPresent()) {
            model.addAttribute("category", catOpt.get());
            return "update-category";
        }
        return "redirect:/manage-categories";
    }

    @GetMapping("/update-event/{id}")
    public String openUpdateEventPage(@PathVariable int id, Model model) {
        Optional<Events> eveOpt = eventsService.getEventsById(id);
        if (eveOpt.isPresent()) {
            model.addAttribute("event", eveOpt.get());
            model.addAttribute("categories", eveCatService.getCategories());
            return "update-event";
        }
        return "redirect:/manage-events";
    }

    @GetMapping("/ticket-template")
    public String openTicketTemplate() {
        return "ticket-template";
    }

    @GetMapping("/help")
    public String openHelpPage() {
        return "help";
    }

    @GetMapping("/contact")
    public String openContactPage() {
        return "contact";
    }

    @GetMapping("/documentation")
    public String openDocumentationPage() {
        return "documentation";
    }

    @GetMapping("/support")
    public String openSupportPage() {
        return "support";
    }
}