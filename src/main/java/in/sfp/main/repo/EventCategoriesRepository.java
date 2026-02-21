package in.sfp.main.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.sfp.main.model.EventCategories;

@Repository
public interface EventCategoriesRepository extends JpaRepository<EventCategories, Integer> {

}
