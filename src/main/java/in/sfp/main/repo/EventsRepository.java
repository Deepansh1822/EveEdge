package in.sfp.main.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.sfp.main.model.Events;

@Repository
public interface EventsRepository extends JpaRepository<Events, Integer> {

}
