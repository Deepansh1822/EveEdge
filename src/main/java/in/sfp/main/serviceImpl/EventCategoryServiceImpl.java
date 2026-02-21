package in.sfp.main.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import in.sfp.main.model.EventCategories;
import in.sfp.main.repo.EventCategoriesRepository;
import in.sfp.main.service.EventCategoriesService;

@Service
public class EventCategoryServiceImpl implements EventCategoriesService {

	@Autowired
	private EventCategoriesRepository eveCatRepo;

	@Override
	public EventCategories addCategories(EventCategories eveCat) {
		return eveCatRepo.save(eveCat);

	}

	@Override
	public boolean updateCategories(int id, EventCategories newEveCat) {

		EventCategories oldEveCat = eveCatRepo.findById(id).orElse(null);

		if (oldEveCat != null) {

			oldEveCat.setCatName(newEveCat.getCatName());

			// Only update icon if a new one is provided
			if (newEveCat.getCatIcon() != null && newEveCat.getCatIcon().length > 0) {
				oldEveCat.setCatIcon(newEveCat.getCatIcon());
			}

			oldEveCat.setCatColor(newEveCat.getCatColor());
			oldEveCat.setDescription(newEveCat.getDescription());
			oldEveCat.setUpdatedAt(LocalDateTime.now());

			eveCatRepo.save(oldEveCat);
			return true;
		}
		return false;
	}

	@Override
	public boolean deleteCategories(int id) {
		EventCategories cat = eveCatRepo.findById(id).orElse(null);
		if (cat != null) {
			cat.setDeleted(true);
			eveCatRepo.save(cat);
			return true;
		}
		return false;
	}

	@Override
	public List<EventCategories> getCategories() {
		List<EventCategories> cats = eveCatRepo.findAll().stream()
				.filter(c -> !c.isDeleted())
				.collect(java.util.stream.Collectors.toList());

		// Filter out deleted events from each category's list
		cats.forEach(c -> {
			if (c.getEvents() != null) {
				c.setEvents(c.getEvents().stream()
						.filter(e -> !e.isDeleted())
						.collect(java.util.stream.Collectors.toList()));
			}
		});
		return cats;
	}

	@Override
	public Optional<EventCategories> getCategoriesById(int id) {
		Optional<EventCategories> cat = eveCatRepo.findById(id);
		cat.ifPresent(c -> {
			if (c.getEvents() != null) {
				c.setEvents(c.getEvents().stream()
						.filter(e -> !e.isDeleted())
						.collect(java.util.stream.Collectors.toList()));
			}
		});
		return cat;
	}

	@Override
	public int getTotalCategories() {
		return (int) eveCatRepo.findAll().stream()
				.filter(c -> !c.isDeleted())
				.count();
	}

}
