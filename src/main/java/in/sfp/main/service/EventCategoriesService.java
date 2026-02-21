package in.sfp.main.service;

import java.util.List;
import java.util.Optional;

import in.sfp.main.model.EventCategories;



public interface EventCategoriesService {
	
	public EventCategories addCategories(EventCategories eveCat);
	
	public boolean updateCategories(int id, EventCategories newEveCat);
	
	public boolean deleteCategories(int id);
	
	public List<EventCategories> getCategories();
	
	public Optional<EventCategories> getCategoriesById(int id);
	
	public int getTotalCategories();

}
