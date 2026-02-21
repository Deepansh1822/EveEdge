package in.sfp.main.service;

import java.util.List;
import java.util.Optional;

import in.sfp.main.model.Events;

public interface EventsService {

	public Events addEvents(Events eve);

	public boolean updateEvents(int id, Events eve);

	public boolean deleteEvents(int id);

	public List<Events> getEvents();

	public Optional<Events> getEventsById(int id);

	public int getTotalEvents();

	public List<Events> getTopEvents();

	public boolean updateEventStatus(int id, String status, String statusReason);
}
