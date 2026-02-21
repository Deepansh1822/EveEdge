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

import in.sfp.main.model.EventCategories;
import in.sfp.main.service.EventCategoriesService;

@RestController
@RequestMapping("/api/categories") // ...add this servlet path as base url...
public class EventCategoriesController {

	@Autowired
	private EventCategoriesService eveCatService;

	@PostMapping("/add")
	public EventCategories addCategories(@RequestBody EventCategories evecat) {

		return eveCatService.addCategories(evecat);
	}

	@PutMapping("/update/{id}")
	public ResponseEntity<String> updateCategories(@PathVariable int id, @RequestBody EventCategories eveCat) {

		boolean updatedEveCat = eveCatService.updateCategories(id, eveCat);
		if (updatedEveCat == true) {
			return ResponseEntity.ok().body("Categories Updated Successfully...");
		}

		return ResponseEntity.notFound().build();
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<EventCategories> deleteCategories(@PathVariable int id) {

		boolean deletedEveCat = eveCatService.deleteCategories(id);

		if (deletedEveCat) {

			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}

	@GetMapping("/get")
	public List<EventCategories> getCategories() {

		return eveCatService.getCategories();
	}

	@GetMapping("/get/{id}")
	public ResponseEntity<EventCategories> getCategoriesById(@PathVariable int id) {
		EventCategories eveCat = eveCatService.getCategoriesById(id).orElse(null);

		if (eveCat != null) {
			return ResponseEntity.ok().body(eveCat);
		}

		return ResponseEntity.notFound().build();
	}

	// Controller for fetching total categories

	@GetMapping("/get/count")
	public int getTotalCategories() {

		int categCount = eveCatService.getTotalCategories();

		return categCount;
	}
}